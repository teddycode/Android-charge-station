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
	
	TIM3_Init(9999,7199); // 定时器3初始化
	
	OLED_ShowStr(0,2,"Starting...",2);	
	DelayS(6);
			
	while(1)
	{
		SYN_Speaker(0,"请使用APP连接并开始充电");			// 1、等待充电
		DelayS(1);		
		while(m_charge_state==WAIT || m_charge_state == STOP)
		{
			//SYN_Speaker(0,"[v15][m1][t5]1");			// 1、等待充电	
			OLED_CLS();
			DelayMs(500);			
			OLED_ShowStr(0,2,"Wait for connect",2);
			DelayMs(500);
			//USART_SendStr(USART2,"hello",5);
		}		
		
		if(m_charge_state==START) // 2、开始充电
		{
			jx = Charge_Switch();				// 检测并修正电池接入极性			
			//RELAY1 = OFF; //Debug 使用zheng向
			//RELAY2 = OFF;			
			if(jx == 0){
				Charge_Stop();	
				SYN_Speaker(0,"充电失败");
			}else
			{
				Charge_Start();					// 开始充电
				SYN_Speaker(0,"开始充电");
				OLED_CLS();
				while(1)  							// 显示充电状态
				{		
					Voltage = Charge_Get_Voltage();
					Show_States(Voltage/4000.0,Current/10000.0,Energy2,Time,jx); // 显示充电状态
					Bt_Upload_States(m_charge_state,Energy2);	// 蓝牙上传充电状态
					IM_Read();
					DelayMs(500);															// 更新速率约为2hz
					IM_Analysis();
					if(Time>Time_Limit*3600) 
						m_charge_state=STOP;  // 充电超时
					if(Charge_Get_State()==1) 
						m_charge_state=FINISH;	  			// 充电完成
					if(m_charge_state==STOP || m_charge_state==FINISH) break;	// 状态退出
				}															
				//Bt_Upload_States(m_charge_state,Energy);	// 3、结束充电
				Charge_Stop();	
				SYN_Speaker(0,"结束充电");
				DelayS(2);
			}
		}
	}
}
