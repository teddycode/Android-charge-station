#include "im1253b.h"

extern u8  USART3_State;
extern u8  USART3_Len;
extern u8  USART3_Tx_Buf[8];
extern u8  USART3_Rx_Buf[40];

u32 Voltage,Current,Power,Energy,Pf,CO2;
// 计算CRC
u16 calccrc(u8 crcbuf,u16 crc)
{
    u8 i;
    u8 chk;
    crc=crc^crcbuf;
    for(i=0;i<8;i++)
    {
        chk=(u8)(crc&1);
        crc=crc>>1;
        crc=crc&0x7fff;
        if(chk==1)
        crc=crc^0xa001;
        crc=crc&0xffff;
    }
    return crc;
}

// 校验CRC
u16 chkcrc(u8 *buf,u8 len)
{
    u8 hi,lo;
    u16 i;
    u16 crc;
    crc=0xFFFF;
    for(i=0;i<len;i++)
    {
        crc=calccrc(*buf,crc);
        buf++;
    }
    hi=(u8)(crc%256);
    lo=(u8)(crc/256);
    crc=(((u16)(hi))<<8)|lo;
    return crc;
}

// 读取数据
void IM_Read(void)
{
    union crcdata
    {
        u16 word16;
        u8 byte[2];
    }crcnow;
		
		USART3_Tx_Buf[0]=DEVICE_ID; //模块的 ID 号，默认 ID 为 0x01
		USART3_Tx_Buf[1]=0x03;
		USART3_Tx_Buf[2]=0x00;
		USART3_Tx_Buf[3]=0x48;
		USART3_Tx_Buf[4]=0x00;
		USART3_Tx_Buf[5]=0x06;
		crcnow.word16=chkcrc(USART3_Tx_Buf,6);
		USART3_Tx_Buf[6]=crcnow.byte[1]; //CRC 效验低字节在前
		USART3_Tx_Buf[7]=crcnow.byte[0];
		USART_SendStr(USART3,USART3_Tx_Buf,8); //发送 8 个数据
}

//解析数据
void IM_Analysis(void)
{
    union crcdata
    {
			u16 word16;
			u8 byte[2];
    }crcnow;
		
    if(USART3_State==1) //接收完成
    {
        USART3_State=0;
        if(USART3_Rx_Buf[0]==DEVICE_ID) //确认 ID 正确
        {
            crcnow.word16=chkcrc(USART3_Rx_Buf,USART3_Len-2); //reveive_numbe 是接收数据总长度
            if((crcnow.byte[0]==USART3_Rx_Buf[USART3_Len-1])&&(crcnow.byte[1]==USART3_Rx_Buf[USART3_Len-2]))//确认 CRC 校验正确
            {
                Voltage=(((u32)(USART3_Rx_Buf[3]))<<24)|(((u32)(USART3_Rx_Buf[4]))<<16)|(((u32)(USART3_Rx_Buf[5]))<<8)|USART3_Rx_Buf[6];
                Current=(((u32)(USART3_Rx_Buf[7]))<<24)|(((u32)(USART3_Rx_Buf[8]))<<16)|(((u32)(USART3_Rx_Buf[9]))<<8)|USART3_Rx_Buf[10];
                Power=(((u32)(USART3_Rx_Buf[11]))<<24)|(((u32)(USART3_Rx_Buf[12]))<<16)|(((u32)(USART3_Rx_Buf[13]))<<8)|USART3_Rx_Buf[14];
                Energy=(((u32)(USART3_Rx_Buf[15]))<<24)|(((u32)(USART3_Rx_Buf[16]))<<16)|(((u32)(USART3_Rx_Buf[17]))<<8)|USART3_Rx_Buf[18];
                Pf=(((u32)(USART3_Rx_Buf[19]))<<24)|(((u32)(USART3_Rx_Buf[20]))<<16)|(((u32)(USART3_Rx_Buf[21]))<<8)|USART3_Rx_Buf[22];
                CO2=(((u32)(USART3_Rx_Buf[23]))<<24)|(((u32)(USART3_Rx_Buf[24]))<<16)|(((u32)(USART3_Rx_Buf[25]))<<8)|USART3_Rx_Buf[26];
            }
        }
    }
}
