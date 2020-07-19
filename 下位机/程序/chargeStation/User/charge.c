#include "charge.h"
#include "delay.h"
#include "adc.h"
#include "usart.h"

extern u32 Time;
u8 m_charge_state;
extern float Energy2;

// ��ʼ�������״̬
void Charge_Init(void)
{
	GPIO_InitTypeDef  GPIO_InitStructure; 

	//��ʼ��GPIO
	RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOA,ENABLE);//ʹ�� GPIOA ʱ��
	RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOB,ENABLE);//ʹ�� GPIOB ʱ��
	RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOC,ENABLE);//ʹ�� GPIOC ʱ��

	// ����tp4056��������
	GPIO_InitStructure.GPIO_Pin = GPIO_Pin_7;				 		 //�˿�����
	GPIO_InitStructure.GPIO_Mode = GPIO_Mode_Out_PP; 		 //����CE��ʹ���������
	GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;		 //IO���ٶ�Ϊ50MHz
	GPIO_Init(GPIOA, &GPIO_InitStructure);							 //�����趨������ʼ��GPIOA.7
	// ����tp4056 оƬ�������
	GPIO_InitStructure.GPIO_Pin = GPIO_Pin_0 | GPIO_Pin_1;	  //�˿�����
	GPIO_InitStructure.GPIO_Mode = GPIO_Mode_IN_FLOATING ; 		//��ȡ�ø�������
	GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;		 			//IO���ٶ�Ϊ50MHz
	GPIO_Init(GPIOB, &GPIO_InitStructure);										//�����趨������ʼ��GPIOB.0.1
	
	//���ü̵�����������
	GPIO_InitStructure.GPIO_Pin = GPIO_Pin_13 | GPIO_Pin_14;	 //�˿�����
	GPIO_InitStructure.GPIO_Mode = GPIO_Mode_Out_PP; 		 //���������ܣ�ʹ���������
	GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;		 //IO���ٶ�Ϊ50MHz
	GPIO_Init(GPIOC, &GPIO_InitStructure);							 //�����趨������ʼ��GPIOA.7
	
	CE = DISABLE; // Ĭ�Ϲر�оƬ
	
	RELAY1 = ON;  // Ĭ�ϳ�����Ч
	RELAY2 = OFF;	
}


// ��ȡоƬ�ĳ��״̬����ʱ50ms
// ��=1 ,��=0��������chg=1;std=0; ����0���ڳ�磬����1��������
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

// У����ؼ��ԣ�����״ֵ̬����ʱ100ms,return 0:����ʧ�ܣ� 1�����ڳɹ���2������3������
u8 Charge_Switch(void)
{
	u16 i;
	u32 ch0=0, ch1=0;
	//��ȡ����adc��ֵ
	for(i=0;i<200;i++)
	{
		ch0 += ADC_Read_Value(0);
		ch1 += ADC_Read_Value(1);
		DelayUs(20);
	}
	ch0 = ch0/200;
	ch1 = ch1/200;
	
	//USART_Printf(USART1,"adc_ch0=%ld|adc_ch1=%ld ||",ch0,ch1);
	
	if(ch1 > ch0)  //����
	{
		if(ch1-ch0 < VOLTAGE_GAP )  // ��ѹ��̫�� 
			return 0;
		
		RELAY1 = OFF;
		RELAY2 = OFF;
		return 2;
	}		
	else if(ch0 == ch1) // ����
	{
		RELAY1 = ON;
		RELAY2 = ON;
		return 3;
	}else	if(ch0-ch1 < VOLTAGE_GAP )  // ��ѹ��̫�� 
			return 0;
		
	return 1;
}

// ��ʼ���
void Charge_Start(void)
{
	CE=ENABLE;		// ��оƬ����
	Time=0;  			// ��ʼ��ʱ
  Energy2 =0.0 ;
	m_charge_state = START;
}

// ֹͣ���
void Charge_Stop(void)
{
	CE=DISABLE; 	
	RELAY1 = ON;  // ������Ч
	RELAY2 = OFF;	
	m_charge_state = WAIT;
}

u16 Charge_Get_Voltage()
{
	u8 i;
	u32 ch0=0, ch1=0;
	//��ȡ����adc��ֵ
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

