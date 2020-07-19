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
//�������´���,֧��printf����,������Ҫѡ��use MicroLIB	  
#if 1
#pragma import(__use_no_semihosting)             
//��׼����Ҫ��֧�ֺ���                 
struct __FILE 
{ 
	int handle; 

}; 

FILE __stdout;       
//����_sys_exit()�Ա���ʹ�ð�����ģʽ    
_sys_exit(int x) 
{ 
	x = x; 
} 
//�ض���fputc���� 
int fputc(int ch, FILE *f)
{      
	while((USART1->SR&0X40)==0);//ѭ������,ֱ���������   
    USART1->DR = (u8) ch;      
	return ch;
}
#endif 

/*ʹ��microLib�ķ���*/
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
 

//����1�жϷ������
//ע��,��ȡUSARTx->SR�ܱ���Ī������Ĵ���   	
u8 USART_RX_BUF[USART_REC_LEN];     //���ջ���,���USART_REC_LEN���ֽ�.
u16 USART_RX_STA=0;       //����״̬���	  
  
void USART1_Init(u32 baudRate){
  //GPIO�˿�����
  GPIO_InitTypeDef GPIO_InitStructure;
	USART_InitTypeDef USART_InitStructure;
	NVIC_InitTypeDef NVIC_InitStructure;
		
	//ʹ��USART1��GPIOAʱ��	 
	RCC_APB2PeriphClockCmd(RCC_APB2Periph_USART1|RCC_APB2Periph_GPIOA, ENABLE);
  
	//USART1_TX   GPIOA.9
  GPIO_InitStructure.GPIO_Pin = GPIO_Pin_9; //PA.9
  GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;
  GPIO_InitStructure.GPIO_Mode = GPIO_Mode_AF_PP;	//�����������
  GPIO_Init(GPIOA, &GPIO_InitStructure);//��ʼ��GPIOA.9
   
  //USART1_RX	  GPIOA.10��ʼ��
  GPIO_InitStructure.GPIO_Pin = GPIO_Pin_10;//PA10
  GPIO_InitStructure.GPIO_Mode = GPIO_Mode_IN_FLOATING;//��������
  GPIO_Init(GPIOA, &GPIO_InitStructure);//��ʼ��GPIOA.10  

  //Usart1 NVIC ����
  NVIC_InitStructure.NVIC_IRQChannel = USART1_IRQn;
	NVIC_InitStructure.NVIC_IRQChannelPreemptionPriority=3 ;//��ռ���ȼ�3
	NVIC_InitStructure.NVIC_IRQChannelSubPriority = 3;		//�����ȼ�3
	NVIC_InitStructure.NVIC_IRQChannelCmd = ENABLE;			//IRQͨ��ʹ��
	NVIC_Init(&NVIC_InitStructure);	//����ָ���Ĳ�����ʼ��VIC�Ĵ���
  
   //USART ��ʼ������

	USART_InitStructure.USART_BaudRate = baudRate;//���ڲ�����
	USART_InitStructure.USART_WordLength = USART_WordLength_8b;//�ֳ�Ϊ8λ���ݸ�ʽ
	USART_InitStructure.USART_StopBits = USART_StopBits_1;//һ��ֹͣλ
	USART_InitStructure.USART_Parity = USART_Parity_No;//����żУ��λ
	USART_InitStructure.USART_HardwareFlowControl = USART_HardwareFlowControl_None;//��Ӳ������������
	USART_InitStructure.USART_Mode = USART_Mode_Rx | USART_Mode_Tx;	//�շ�ģʽ

  USART_Init(USART1, &USART_InitStructure); //��ʼ������1
  USART_ITConfig(USART1, USART_IT_RXNE, ENABLE);//�������ڽ����ж�
  USART_Cmd(USART1, ENABLE);                    //ʹ�ܴ���1 

}
 
void USART2_Init(u32 baudRate)
{
	GPIO_InitTypeDef GPIO_InitStrue;
	USART_InitTypeDef USART_InitStrue;
	NVIC_InitTypeDef NVIC_InitStrue;
	
	// ����ʹ��ʱ��
	RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOA,ENABLE);
	RCC_APB1PeriphClockCmd(RCC_APB1Periph_USART2,ENABLE);
	USART_DeInit(USART2);  //��λ����2 -> ����û��
	
	// ��ʼ�� ���ڶ�ӦIO��  TX-PA2  RX-PA3
	GPIO_InitStrue.GPIO_Mode=GPIO_Mode_AF_PP;
	GPIO_InitStrue.GPIO_Pin=GPIO_Pin_2;
	GPIO_InitStrue.GPIO_Speed=GPIO_Speed_50MHz;
	GPIO_Init(GPIOA,&GPIO_InitStrue);
	
	GPIO_InitStrue.GPIO_Mode=GPIO_Mode_IN_FLOATING;
	GPIO_InitStrue.GPIO_Pin=GPIO_Pin_3;
  GPIO_Init(GPIOA,&GPIO_InitStrue);
	
	// ��ʼ�� ����ģʽ״̬
	USART_InitStrue.USART_BaudRate=baudRate; // ������
	USART_InitStrue.USART_HardwareFlowControl=USART_HardwareFlowControl_None; // Ӳ��������
	USART_InitStrue.USART_Mode=USART_Mode_Tx|USART_Mode_Rx; // ���� ���� ģʽ��ʹ��
	USART_InitStrue.USART_Parity=USART_Parity_No; // û����żУ��
	USART_InitStrue.USART_StopBits=USART_StopBits_1; // һλֹͣλ
	USART_InitStrue.USART_WordLength=USART_WordLength_8b; // ÿ�η������ݿ��Ϊ8λ
	USART_Init(USART2,&USART_InitStrue);
	
	USART_Cmd(USART2,ENABLE);//ʹ�ܴ���
	USART_ITConfig(USART2,USART_IT_RXNE,ENABLE);//���������ж�
	
	// ��ʼ�� �ж����ȼ�
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
	gpioInitStruct.GPIO_Mode = GPIO_Mode_AF_PP;			//�����������
	GPIO_Init(GPIOB, &gpioInitStruct);					//��ʼ��GPIOB.10
   
	//USART1_RX	  GPIOB.11��ʼ��
	gpioInitStruct.GPIO_Pin = GPIO_Pin_11;				//PB.11
	gpioInitStruct.GPIO_Mode = GPIO_Mode_IN_FLOATING;		//��������
	GPIO_Init(GPIOB, &gpioInitStruct);					//��ʼ��GPIOB.11 
	
	usartInitStruct.USART_BaudRate = baudRate;
	usartInitStruct.USART_HardwareFlowControl = USART_HardwareFlowControl_None;		//��Ӳ������
	usartInitStruct.USART_Mode = USART_Mode_Rx | USART_Mode_Tx;				//���պͷ���
	usartInitStruct.USART_Parity = USART_Parity_No;						//��У��
	usartInitStruct.USART_StopBits = USART_StopBits_1;					//1λֹͣλ
	usartInitStruct.USART_WordLength = USART_WordLength_8b;					//8λ����λ
	USART_Init(USART3, &usartInitStruct);
	
	USART_Cmd(USART3, ENABLE);														//ʹ�ܴ���
	
	USART_ITConfig(USART3, USART_IT_RXNE, ENABLE);						//ʹ�ܽ����ж�
	
	nvicInitStruct.NVIC_IRQChannel = USART3_IRQn;
	nvicInitStruct.NVIC_IRQChannelCmd = ENABLE;
	nvicInitStruct.NVIC_IRQChannelPreemptionPriority = 0;
	nvicInitStruct.NVIC_IRQChannelSubPriority = 0;
	NVIC_Init(&nvicInitStruct);
 
}

/*
************************************************************
*	�������ƣ�	UsartPrintf
*
*	�������ܣ�	��ʽ����ӡ
*
*	��ڲ�����	USARTx��������
*				fmt����������
*
*	���ز�����	��
*
*	˵����		
************************************************************
*/
void USART_Printf(USART_TypeDef *USARTx, char *fmt,...)
{ 
	unsigned char UsartPrintfBuf[296];
	va_list ap;
	unsigned char *pStr = UsartPrintfBuf;
	
	va_start(ap, fmt);
	vsprintf((char *)UsartPrintfBuf, fmt, ap);				//��ʽ��
	va_end(ap);
	
	while(*pStr != 0)
	{
		USART_SendData(USARTx, *pStr++);
		while(USART_GetFlagStatus(USARTx, USART_FLAG_TC) == RESET);
	}
}


/*
************************************************************
*	�������ƣ�	Usart_SendString
*
*	�������ܣ�	�������ݷ���
*
*	��ڲ�����	USARTx��������
*				str��Ҫ���͵�����
*				len�����ݳ���
*
*	���ز�����	��
*
*	˵����		
************************************************************
*/
 
void USART_SendStr(USART_TypeDef *USARTx, char *str, u8 len)
{
 
	unsigned short count = 0;
	
	for(; count < len; count++)
	{
		USART_SendData(USARTx, *str++);						//��������
		while(USART_GetFlagStatus(USARTx, USART_FLAG_TC) == RESET);		//�ȴ��������
	}
}

void USART1_IRQHandler(void)                	//����1�жϷ������
{
	u8 Res;

	if(USART_GetITStatus(USART1, USART_IT_RXNE) != RESET)  //�����ж�
		{
			USART_ClearITPendingBit(USART1,USART_IT_RXNE);
		  Res=USART_ReceiveData(USART1);	//��ȡ���յ�������
			USART_SendData(USART3,Res);//�ش����յ���������USART2	
			
	/*		if(Res=='1')//���յ�1������LED
			{
				LED0=1;
			}
			else//�������Ϩ��LED
			{
				LED0=0;
			}		
			*/
			USART_RX_STA=0;		
     }
} 
 
// �������ڴ�����
// ֡ͷ|����|���� *118#,1����״̬��1��,8ʱ������
u8 Time_Limit;
char buf2[16];
void USART2_IRQHandler(void) // ����2�жϷ�����
{
	char data;
	static u8 i=0,start=0,fn=0,arg=0;
	if(USART_GetITStatus(USART2,USART_IT_RXNE) != RESET) // �жϱ�־
	{	
	  USART_ClearFlag(USART2, USART_FLAG_RXNE);
		data = USART_ReceiveData(USART2);  					// ����2 ����
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


void USART3_IRQHandler(void)  //����3�жϺ���
{
	static u8 i=0,first=0,second=0,start=0;;
	if(USART_GetITStatus(USART3, USART_IT_RXNE) != RESET)	//�����ж�
	{
		first = second;
		second = USART_ReceiveData(USART3);  		// ����3 ����
		USART_SendData(USART2,second);
		if(start==0 && first==0x01 && second==0x03)		 // ֡ͷ�ж�
		{	
			USART3_Rx_Buf[0]=first;
			USART3_Rx_Buf[1]=second;
			start=1;
			i=2;
//			LED0=0;
		}else if(start==1) // ��ʼȡ����
		{
			// ��ȡ����
			if(i==2)
			{
				USART3_Len = second+5;  //��������+��β5�ֽ�
				if(USART3_Len > 40)  		//�쳣����ֵ�����¿�ʼ
				{
					start=0;
				}
			}
			if(i<USART3_Len){
				USART3_Rx_Buf[i]=second;
			//	USART_SendData(USART1,USART3_Rx_Buf[i]);
			}else{ //֡����
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

// �����ϴ���APP
void Bt_Upload_States(u8 state,float energy)
{
	USART_Printf(USART2," S:%d, E:%.4f\n",state,energy);
}



