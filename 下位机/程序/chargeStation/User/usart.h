#ifndef __USART_H
#define __USART_H

#include "stdio.h"	
#include "sys.h" 
#include "stdarg.h"
#include "stdlib.h"

#define USART_REC_LEN  			200  	//定义最大接收字节数 200
#define EN_USART1_RX 			  1		//使能（1）/禁止（0）串口1接收
	  	
extern u8  USART_RX_BUF[USART_REC_LEN]; //接收缓冲,最大USART_REC_LEN个字节.末字节为换行符 
extern u16 USART_RX_STA;         		//接收状态标记	
//USART3 缓冲区
extern u8  USART3_State;
extern u8  USART3_Len;
extern u8  USART3_Tx_Buf[8];
extern u8  USART3_Rx_Buf[40];

//限时充电
extern u8 Time_Limit;

void USART1_Init(u32 baudRate);
void USART2_Init(u32 baudRate);
void USART3_Init(u32 baudRate);
void USART_SendStr(USART_TypeDef *USARTx, char *str, u8 len);
void USART_Printf(USART_TypeDef *USARTx, char *fmt,...);
void Bt_Upload_States(u8 state,float energy);

#endif


