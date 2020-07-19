#ifndef __ADC_H
#define __ADC_H

#include "sys.h"

void ADC_GPIO_Init(void);
void ADC_Conf_Init(void);
u16 ADC_Read_Value(u8 ch);

#endif
