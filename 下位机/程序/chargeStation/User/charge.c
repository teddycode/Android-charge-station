#include "charge.h"
#include "delay.h"
#include "adc.h"
#include "usart.h"

extern u32 Time;
u8 m_charge_state;
extern float Energy2;

// 初始化充电器状态
void Charge_Init(void)
{
	GPIO_InitTypeDef  GPIO_InitStructure; 

	//初始化GPIO
	RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOA,ENABLE);//使能 GPIOA 时钟
	RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOB,ENABLE);//使能 GPIOB 时钟
	RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOC,ENABLE);//使能 GPIOC 时钟

	// 配置tp4056驱动引脚
	GPIO_InitStructure.GPIO_Pin = GPIO_Pin_7;				 		 //端口配置
	GPIO_InitStructure.GPIO_Mode = GPIO_Mode_Out_PP; 		 //驱动CE，使用推挽输出
	GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;		 //IO口速度为50MHz
	GPIO_Init(GPIOA, &GPIO_InitStructure);							 //根据设定参数初始化GPIOA.7
	// 配置tp4056 芯片检测引脚
	GPIO_InitStructure.GPIO_Pin = GPIO_Pin_0 | GPIO_Pin_1;	  //端口配置
	GPIO_InitStructure.GPIO_Mode = GPIO_Mode_IN_FLOATING ; 		//读取用浮空输入
	GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;		 			//IO口速度为50MHz
	GPIO_Init(GPIOB, &GPIO_InitStructure);										//根据设定参数初始化GPIOB.0.1
	
	//配置继电器控制引脚
	GPIO_InitStructure.GPIO_Pin = GPIO_Pin_13 | GPIO_Pin_14;	 //端口配置
	GPIO_InitStructure.GPIO_Mode = GPIO_Mode_Out_PP; 		 //驱动三极管，使用推挽输出
	GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;		 //IO口速度为50MHz
	GPIO_Init(GPIOC, &GPIO_InitStructure);							 //根据设定参数初始化GPIOA.7
	
	CE = DISABLE; // 默认关闭芯片
	
	RELAY1 = ON;  // 默认充电口无效
	RELAY2 = OFF;	
}


// 获取芯片的充电状态，耗时50ms
// 灭=1 ,亮=0；充满：chg=1;std=0; 返回0正在充电，返回1，充电结束
u8  Charge_Get_State(void)
{
	u8 stat_ch,stat_std;
	u8 i,count_ch[2]={0,0},count_std[2]={0,0};
	for(i=0;i<250;++i)
	{
		count_ch[CHRG]++;
		count_std[STDY]++;
		DelayUs(20);
	}
	
	//USART_Printf(USART1,"chrg0=%u chrg1=%u|stdy0=%u,stdy1=%u||",count_ch[0],count_ch[1],count_std[0],count_std[1]);
	
	if(count_ch[0] > count_ch[1])
		stat_ch = 0;
	else
		stat_ch =1;
	
	if(count_std[0] > count_std[1])
		stat_std = 0;
	else
		stat_std = 1;
		
	if(stat_ch==0 && stat_std == 1)
		return 0;
	else if(stat_ch == 1 && stat_std ==0)
		return 1;
	else
		return 2;
}

// 校正电池极性，返回状态值，耗时100ms,return 0:调节失败， 1：调节成功，2：正向，3：反向
u8 Charge_Switch(void)
{
	u16 i;
	u32 ch0=0, ch1=0;
	//读取两个adc的值
	for(i=0;i<200;i++)
	{
		ch0 += ADC_Read_Value(0);
		ch1 += ADC_Read_Value(1);
		DelayUs(20);
	}
	ch0 = ch0/200;
	ch1 = ch1/200;
	
	//USART_Printf(USART1,"adc_ch0=%ld|adc_ch1=%ld ||",ch0,ch1);
	
	if(ch1 > ch0)  //正向
	{
		if(ch1-ch0 < VOLTAGE_GAP )  // 电压差太低 
			return 0;
		
		RELAY1 = OFF;
		RELAY2 = OFF;
		return 2;
	}		
	else if(ch0 == ch1) // 反向
	{
		RELAY1 = ON;
		RELAY2 = ON;
		return 3;
	}else	if(ch0-ch1 < VOLTAGE_GAP )  // 电压差太低 
			return 0;
		
	return 1;
}

// 开始充电
void Charge_Start(void)
{
	CE=ENABLE;		// 打开芯片开关
	Time=0;  			// 开始计时
  Energy2 =0.0 ;
	m_charge_state = START;
}

// 停止充电
void Charge_Stop(void)
{
	CE=DISABLE; 	
	RELAY1 = ON;  // 充电口无效
	RELAY2 = OFF;	
	m_charge_state = WAIT;
}

u16 Charge_Get_Voltage()
{
	u8 i;
	u32 ch0=0, ch1=0;
	//读取两个adc的值
	for(i=0;i<200;i++)
	{
		ch0 += ADC_Read_Value(0);
		ch1 += ADC_Read_Value(1);
		DelayUs(20);
	}
	ch0 = ch0/200;
	ch1 = ch1/200;
	
	if(ch0 > ch1)
		return (ch0-ch1)*3.3*2;
	else
		return (ch1-ch0)*3.3*2;	
	
}

