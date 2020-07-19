#ifndef __CHARGE_H
#define __CHARGE_H

#include "sys.h"

#define CHRG 			PBin(0)
#define STDY 			PBin(1)
#define CE			 	PAout(7)		//A.7
#define RELAY1 	  PCout(13)
#define RELAY2	  PCout(14)

#define ON  1 
#define OFF 0

#define WAIT   0
#define START  1
#define STOP   2
#define FINISH 3

#define VOLTAGE_GAP  500 // 电压差，判断是否接了电池

void Charge_Init(void);
u8 	 Charge_Get_State(void);
u8 	 Charge_Switch(void);
void Charge_Start(void);
void Charge_Stop(void);
u16 Charge_Get_Voltage();

#endif
