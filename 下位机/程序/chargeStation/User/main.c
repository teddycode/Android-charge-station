#include "oled.h"
#include "delay.h"
#include "led.h"
#include "usart.h"
#include "adc.h"
#include "charge.h"
#include "syn6288.h"
#include "timer.h"
#include "im1253b.h"

char buf[16];

int main(void)
{
	u8 i,jx;
	u16 adc_value;	
	
	extern u32 Time;
	extern float Energy2;
	extern u8 m_charge_state;
	extern u32 Voltage,Current,Power,Energy,Pf,CO2;
		
	I2C_Configuration();
	
	DelayInit();
	Charge_Init();	
	OLED_Init();
	//LED_Init();
	USART1_Init(9600);
	USART2_Init(9600);
	USART3_Init(9600);
	
	ADC_Conf_Init();
	
	TIM3_Init(9999,7199); // ��ʱ��3��ʼ��
	
	OLED_ShowStr(0,2,"Starting...",2);	
	DelayS(6);
			
	while(1)
	{
		SYN_Speaker(0,"��ʹ��APP���Ӳ���ʼ���");			// 1���ȴ����
		DelayS(1);		
		while(m_charge_state==WAIT || m_charge_state == STOP)
		{
			//SYN_Speaker(0,"[v15][m1][t5]1");			// 1���ȴ����	
			OLED_CLS();
			DelayMs(500);			
			OLED_ShowStr(0,2,"Wait for connect",2);
			DelayMs(500);
			//USART_SendStr(USART2,"hello",5);
		}		
		
		if(m_charge_state==START) // 2����ʼ���
		{
			jx = Charge_Switch();				// ��Ⲣ������ؽ��뼫��			
			//RELAY1 = OFF; //Debug ʹ��zheng��
			//RELAY2 = OFF;			
			if(jx == 0){
				Charge_Stop();	
				SYN_Speaker(0,"���ʧ��");
			}else
			{
				Charge_Start();					// ��ʼ���
				SYN_Speaker(0,"��ʼ���");
				OLED_CLS();
				while(1)  							// ��ʾ���״̬
				{		
					Voltage = Charge_Get_Voltage();
					Show_States(Voltage/4000.0,Current/10000.0,Energy2,Time,jx); // ��ʾ���״̬
					Bt_Upload_States(m_charge_state,Energy2);	// �����ϴ����״̬
					IM_Read();
					DelayMs(500);															// ��������ԼΪ2hz
					IM_Analysis();
					if(Time>Time_Limit*3600) 
						m_charge_state=STOP;  // ��糬ʱ
					if(Charge_Get_State()==1) 
						m_charge_state=FINISH;	  			// ������
					if(m_charge_state==STOP || m_charge_state==FINISH) break;	// ״̬�˳�
				}															
				//Bt_Upload_States(m_charge_state,Energy);	// 3���������
				Charge_Stop();	
				SYN_Speaker(0,"�������");
				DelayS(2);
			}
		}
	}
}
