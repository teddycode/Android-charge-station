#include "sys.h"
#include "usart.h"	
#include "led.h"
#include "delay.h"
#include "charge.h"

extern u8 m_charge_state;
// usart3
u8  USART3_State;
u8  USART3_Len;
u8  USART3_Tx_Buf[8];
u8  USART3_Rx_Buf[40];

//////////////////////////////////////////////////////////////////
//加入以下代码,支持printf函数,而不需要选择use MicroLIB	  
#if 1
#pragma import(__use_no_semihosting)             
//标准库需要的支持函数                 
struct __FILE 
{ 
	int handle; 

}; 

FILE __stdout;       
//定义_sys_exit()以避免使用半主机模式    
_sys_exit(int x) 
{ 
	x = x; 
} 
//重定义fputc函数 
int fputc(int ch, FILE *f)
{      
	while((USART1->SR&0X40)==0);//循环发送,直到发送完毕   
    USART1->DR = (u8) ch;      
	return ch;
}
#endif 

/*使用microLib的方法*/
 /* 
int fputc(int ch, FILE *f)
{
	USART_SendData(USART1, (uint8_t) ch);

	while (USART_GetFlagStatus(USART1, USART_FLAG_TC) == RESET) {}	
   
    return ch;
}
int GetKey (void)  { 

    while (!(USART1->SR & USART_FLAG_RXNE));

    return ((int)(USART1->DR & 0x1FF));
}
*/
 

//串口1中断服务程序
//注意,读取USARTx->SR能避免莫名其妙的错误   	
u8 USART_RX_BUF[USART_REC_LEN];     //接收缓冲,最大USART_REC_LEN个字节.
u16 USART_RX_STA=0;       //接收状态标记	  
  
void USART1_Init(u32 baudRate){
  //GPIO端口设置
  GPIO_InitTypeDef GPIO_InitStructure;
	USART_InitTypeDef USART_InitStructure;
	NVIC_InitTypeDef NVIC_InitStructure;
		
	//使能USART1，GPIOA时钟	 
	RCC_APB2PeriphClockCmd(RCC_APB2Periph_USART1|RCC_APB2Periph_GPIOA, ENABLE);
  
	//USART1_TX   GPIOA.9
  GPIO_InitStructure.GPIO_Pin = GPIO_Pin_9; //PA.9
  GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;
  GPIO_InitStructure.GPIO_Mode = GPIO_Mode_AF_PP;	//复用推挽输出
  GPIO_Init(GPIOA, &GPIO_InitStructure);//初始化GPIOA.9
   
  //USART1_RX	  GPIOA.10初始化
  GPIO_InitStructure.GPIO_Pin = GPIO_Pin_10;//PA10
  GPIO_InitStructure.GPIO_Mode = GPIO_Mode_IN_FLOATING;//浮空输入
  GPIO_Init(GPIOA, &GPIO_InitStructure);//初始化GPIOA.10  

  //Usart1 NVIC 配置
  NVIC_InitStructure.NVIC_IRQChannel = USART1_IRQn;
	NVIC_InitStructure.NVIC_IRQChannelPreemptionPriority=3 ;//抢占优先级3
	NVIC_InitStructure.NVIC_IRQChannelSubPriority = 3;		//子优先级3
	NVIC_InitStructure.NVIC_IRQChannelCmd = ENABLE;			//IRQ通道使能
	NVIC_Init(&NVIC_InitStructure);	//根据指定的参数初始化VIC寄存器
  
   //USART 初始化设置

	USART_InitStructure.USART_BaudRate = baudRate;//串口波特率
	USART_InitStructure.USART_WordLength = USART_WordLength_8b;//字长为8位数据格式
	USART_InitStructure.USART_StopBits = USART_StopBits_1;//一个停止位
	USART_InitStructure.USART_Parity = USART_Parity_No;//无奇偶校验位
	USART_InitStructure.USART_HardwareFlowControl = USART_HardwareFlowControl_None;//无硬件数据流控制
	USART_InitStructure.USART_Mode = USART_Mode_Rx | USART_Mode_Tx;	//收发模式

  USART_Init(USART1, &USART_InitStructure); //初始化串口1
  USART_ITConfig(USART1, USART_IT_RXNE, ENABLE);//开启串口接受中断
  USART_Cmd(USART1, ENABLE);                    //使能串口1 

}
 
void USART2_Init(u32 baudRate)
{
	GPIO_InitTypeDef GPIO_InitStrue;
	USART_InitTypeDef USART_InitStrue;
	NVIC_InitTypeDef NVIC_InitStrue;
	
	// 外设使能时钟
	RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOA,ENABLE);
	RCC_APB1PeriphClockCmd(RCC_APB1Periph_USART2,ENABLE);
	USART_DeInit(USART2);  //复位串口2 -> 可以没有
	
	// 初始化 串口对应IO口  TX-PA2  RX-PA3
	GPIO_InitStrue.GPIO_Mode=GPIO_Mode_AF_PP;
	GPIO_InitStrue.GPIO_Pin=GPIO_Pin_2;
	GPIO_InitStrue.GPIO_Speed=GPIO_Speed_50MHz;
	GPIO_Init(GPIOA,&GPIO_InitStrue);
	
	GPIO_InitStrue.GPIO_Mode=GPIO_Mode_IN_FLOATING;
	GPIO_InitStrue.GPIO_Pin=GPIO_Pin_3;
  GPIO_Init(GPIOA,&GPIO_InitStrue);
	
	// 初始化 串口模式状态
	USART_InitStrue.USART_BaudRate=baudRate; // 波特率
	USART_InitStrue.USART_HardwareFlowControl=USART_HardwareFlowControl_None; // 硬件流控制
	USART_InitStrue.USART_Mode=USART_Mode_Tx|USART_Mode_Rx; // 发送 接收 模式都使用
	USART_InitStrue.USART_Parity=USART_Parity_No; // 没有奇偶校验
	USART_InitStrue.USART_StopBits=USART_StopBits_1; // 一位停止位
	USART_InitStrue.USART_WordLength=USART_WordLength_8b; // 每次发送数据宽度为8位
	USART_Init(USART2,&USART_InitStrue);
	
	USART_Cmd(USART2,ENABLE);//使能串口
	USART_ITConfig(USART2,USART_IT_RXNE,ENABLE);//开启接收中断
	
	// 初始化 中断优先级
	NVIC_InitStrue.NVIC_IRQChannel=USART2_IRQn;
	NVIC_InitStrue.NVIC_IRQChannelCmd=ENABLE;
	NVIC_InitStrue.NVIC_IRQChannelPreemptionPriority=1;
	NVIC_InitStrue.NVIC_IRQChannelSubPriority=1;
	NVIC_Init(&NVIC_InitStrue);
}


void USART3_Init(u32 baudRate)
{
 
	GPIO_InitTypeDef gpioInitStruct;
	USART_InitTypeDef usartInitStruct;
	NVIC_InitTypeDef nvicInitStruct;
	
	RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOB, ENABLE);
	RCC_APB1PeriphClockCmd(RCC_APB1Periph_USART3, ENABLE);
	
	//USART1_TX   GPIOB.10
	gpioInitStruct.GPIO_Pin = GPIO_Pin_10; //PB.10
	gpioInitStruct.GPIO_Speed = GPIO_Speed_50MHz;
	gpioInitStruct.GPIO_Mode = GPIO_Mode_AF_PP;			//复用推挽输出
	GPIO_Init(GPIOB, &gpioInitStruct);					//初始化GPIOB.10
   
	//USART1_RX	  GPIOB.11初始化
	gpioInitStruct.GPIO_Pin = GPIO_Pin_11;				//PB.11
	gpioInitStruct.GPIO_Mode = GPIO_Mode_IN_FLOATING;		//浮空输入
	GPIO_Init(GPIOB, &gpioInitStruct);					//初始化GPIOB.11 
	
	usartInitStruct.USART_BaudRate = baudRate;
	usartInitStruct.USART_HardwareFlowControl = USART_HardwareFlowControl_None;		//无硬件流控
	usartInitStruct.USART_Mode = USART_Mode_Rx | USART_Mode_Tx;				//接收和发送
	usartInitStruct.USART_Parity = USART_Parity_No;						//无校验
	usartInitStruct.USART_StopBits = USART_StopBits_1;					//1位停止位
	usartInitStruct.USART_WordLength = USART_WordLength_8b;					//8位数据位
	USART_Init(USART3, &usartInitStruct);
	
	USART_Cmd(USART3, ENABLE);														//使能串口
	
	USART_ITConfig(USART3, USART_IT_RXNE, ENABLE);						//使能接收中断
	
	nvicInitStruct.NVIC_IRQChannel = USART3_IRQn;
	nvicInitStruct.NVIC_IRQChannelCmd = ENABLE;
	nvicInitStruct.NVIC_IRQChannelPreemptionPriority = 0;
	nvicInitStruct.NVIC_IRQChannelSubPriority = 0;
	NVIC_Init(&nvicInitStruct);
 
}

/*
************************************************************
*	函数名称：	UsartPrintf
*
*	函数功能：	格式化打印
*
*	入口参数：	USARTx：串口组
*				fmt：不定长参
*
*	返回参数：	无
*
*	说明：		
************************************************************
*/
void USART_Printf(USART_TypeDef *USARTx, char *fmt,...)
{ 
	unsigned char UsartPrintfBuf[296];
	va_list ap;
	unsigned char *pStr = UsartPrintfBuf;
	
	va_start(ap, fmt);
	vsprintf((char *)UsartPrintfBuf, fmt, ap);				//格式化
	va_end(ap);
	
	while(*pStr != 0)
	{
		USART_SendData(USARTx, *pStr++);
		while(USART_GetFlagStatus(USARTx, USART_FLAG_TC) == RESET);
	}
}


/*
************************************************************
*	函数名称：	Usart_SendString
*
*	函数功能：	串口数据发送
*
*	入口参数：	USARTx：串口组
*				str：要发送的数据
*				len：数据长度
*
*	返回参数：	无
*
*	说明：		
************************************************************
*/
 
void USART_SendStr(USART_TypeDef *USARTx, char *str, u8 len)
{
 
	unsigned short count = 0;
	
	for(; count < len; count++)
	{
		USART_SendData(USARTx, *str++);						//发送数据
		while(USART_GetFlagStatus(USARTx, USART_FLAG_TC) == RESET);		//等待发送完成
	}
}

void USART1_IRQHandler(void)                	//串口1中断服务程序
{
	u8 Res;

	if(USART_GetITStatus(USART1, USART_IT_RXNE) != RESET)  //接收中断
		{
			USART_ClearITPendingBit(USART1,USART_IT_RXNE);
		  Res=USART_ReceiveData(USART1);	//读取接收到的数据
			USART_SendData(USART3,Res);//回传接收到的数据至USART2	
			
	/*		if(Res=='1')//接收到1，点亮LED
			{
				LED0=1;
			}
			else//其他情况熄灭LED
			{
				LED0=0;
			}		
			*/
			USART_RX_STA=0;		
     }
} 
 
// 蓝牙串口处理函数
// 帧头|功能|参数 *118#,1开关状态，1开,8时间限制
u8 Time_Limit;
char buf2[16];
void USART2_IRQHandler(void) // 串口2中断服务函数
{
	char data;
	static u8 i=0,start=0,fn=0,arg=0;
	if(USART_GetITStatus(USART2,USART_IT_RXNE) != RESET) // 中断标志
	{	
	  USART_ClearFlag(USART2, USART_FLAG_RXNE);
		data = USART_ReceiveData(USART2);  					// 串口2 接收
		USART_SendData(USART2,data);
		
		if(start == 0){
			if(data == '*') start = 1;			
			i = 0;
		}else {
			if(i<15) buf2[i] = data;
			i++;
			if(data == '#'){
				switch(buf2[0]){
					case '1' : 
						if(buf2[1]=='1') { m_charge_state=START; Time_Limit=buf2[2]-'0';}
						else if(buf2[1] == '2') m_charge_state = STOP;
						else m_charge_state=WAIT; break;
					case '2' : 					
							
					break;
				}
				start =0;
				i=0;
			}			
		}
	}
}


void USART3_IRQHandler(void)  //串口3中断函数
{
	static u8 i=0,first=0,second=0,start=0;;
	if(USART_GetITStatus(USART3, USART_IT_RXNE) != RESET)	//接收中断
	{
		first = second;
		second = USART_ReceiveData(USART3);  		// 串口3 接收
		USART_SendData(USART2,second);
		if(start==0 && first==0x01 && second==0x03)		 // 帧头判断
		{	
			USART3_Rx_Buf[0]=first;
			USART3_Rx_Buf[1]=second;
			start=1;
			i=2;
//			LED0=0;
		}else if(start==1) // 开始取数据
		{
			// 获取长度
			if(i==2)
			{
				USART3_Len = second+5;  //数据内容+首尾5字节
				if(USART3_Len > 40)  		//异常长度值，重新开始
				{
					start=0;
				}
			}
			if(i<USART3_Len){
				USART3_Rx_Buf[i]=second;
			//	USART_SendData(USART1,USART3_Rx_Buf[i]);
			}else{ //帧结束
				USART3_State=1;
				start=0;
//				LED0=1;
			}
			i++;
		}
   //USART_SendData(USART1,second);
		USART_ClearFlag(USART3, USART_FLAG_RXNE);
	}
}

// 蓝牙上传至APP
void Bt_Upload_States(u8 state,float energy)
{
	USART_Printf(USART2," S:%d, E:%.4f\n",state,energy);
}



