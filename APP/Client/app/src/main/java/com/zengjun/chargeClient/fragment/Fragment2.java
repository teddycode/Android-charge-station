package com.zengjun.chargeClient.fragment;

import android.Manifest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.zengjun.chargeClient.DeviceListActivity;
import com.zengjun.chargeClient.MainActivity;
import com.zengjun.chargeClient.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class Fragment2 extends Fragment {
    private String TAG = "F2";

    private boolean _isBtOpened;
    private static boolean _isStartGetBtAddress = false;

    private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";   //SPP服务UUID号

    private InputStream is;         //输入流，用来接收蓝牙数据
    private EditText edit0;         //发送数据输入句柄
    private TextView tv_in;          //接收数据显示句柄
    private ScrollView sv;           //翻页句柄
    private String smsg = "";        //显示用数据缓存
    private String fmsg = "";        //保存用数据缓存
    private int msgSize = 0;        // 数据量

    BluetoothDevice _device = null;      //蓝牙设备
    BluetoothSocket _socket = null;      //蓝牙通信socket
    boolean _discoveryFinished = false;
    boolean bRun = true;
    boolean bThread = false;

    private Button btn_send;
    private Button btn_clear;
    private Button btn_connect;
    private TextView tv_connectingDev;

    private BluetoothAdapter _bluetooth = BluetoothAdapter.getDefaultAdapter();    //获取本地蓝牙适配器，即蓝牙设备

    private  MainActivity mActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG,"OnCreateView...");
        return inflater.inflate(R.layout.fragment_fragment2, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG,"onActivityCreated...");

        // 获取所属activity对象
        mActivity= (MainActivity) getActivity();
        /* 解决兼容性问题，6.0以上使用新的API*/
        final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
        final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(mActivity.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},MY_PERMISSION_ACCESS_COARSE_LOCATION);
                Log.e("11111","ACCESS_COARSE_LOCATION");
            }
            if(mActivity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},MY_PERMISSION_ACCESS_FINE_LOCATION);
                Log.e("11111","ACCESS_FINE_LOCATION");
            }
        }

        edit0 = mActivity.findViewById(R.id.Edit0);        //得到输入框句柄
        sv = mActivity.findViewById(R.id.ScrollView01);    //得到翻页句柄
        tv_in =  mActivity.findViewById(R.id.in);          //得到数据显示句柄

        btn_send = mActivity.findViewById(R.id.BtnSend);
        btn_connect = mActivity.findViewById(R.id.BtnConnect);
        btn_clear = mActivity.findViewById(R.id.BtnClear);
        tv_connectingDev = mActivity.findViewById(R.id.tvBtHeader);


        //蓝牙打开失败，提示信息
        if (_bluetooth == null) {
            _isBtOpened = false;
            Toast.makeText(mActivity, "无法打开手机蓝牙，请确认手机是否有蓝牙功能！", Toast.LENGTH_LONG).show();
        } else {
            _isBtOpened = true;
        }

        // 设置设备可以被搜索
        new Thread() {
            public void run() {
                if (_bluetooth.isEnabled() == false) {
                    _bluetooth.enable();
                }
            }
        }.start();

        InitListeners();
        Log.d("CALLBACK","F cal M: "+mActivity.mBtAddress);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG,"onStart...");
    }

    public void  InitListeners(){
        //发送按键响应
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int i = 0;
                int n = 0;
                if (_socket == null) {
                    Toast.makeText(mActivity, "请先连接蓝牙模块", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (edit0.getText().length() == 0) {
                    Toast.makeText(mActivity, "请先输入数据", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    OutputStream os = _socket.getOutputStream();   //蓝牙连接输出流
                    byte[] bos = edit0.getText().toString().getBytes();
                    for (i = 0; i < bos.length; i++) {
                        if (bos[i] == 0x0a) n++;
                    }
                    byte[] bos_new = new byte[bos.length + n];
                    n = 0;
                    for (i = 0; i < bos.length; i++) { //手机中换行为0a,将其改为0d 0a后再发送
                        if (bos[i] == 0x0a) {
                            bos_new[n] = 0x0d;
                            n++;
                            bos_new[n] = 0x0a;
                        } else {
                            bos_new[n] = bos[i];
                        }
                        n++;
                    }

                    os.write(bos_new);
                } catch (IOException e) {
                }
            }
        });

        // 响应连接按键
        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(_bluetooth.isEnabled()==false){  //如果蓝牙服务不可用则提示
                    Toast.makeText(mActivity, " 打开蓝牙中...", Toast.LENGTH_LONG).show();
                    return;
                }
                //如未连接设备则打开DeviceListActivity进行设备搜索
                Button btn =  mActivity.findViewById(R.id.BtnConnect);
                if(_socket==null){
                    Log.d("CALLBACK",mActivity.toString());
                    Intent serverIntent = new Intent(mActivity, DeviceListActivity.class); //跳转程序设置
                    mActivity.startActivityForResult(serverIntent, mActivity.REQUEST_CONNECT_DEVICE);  //设置返回宏定义
                    _isStartGetBtAddress = true;
                }
                else{
                    //关闭连接socket
                    try{
                        bRun = false;
                        Thread.sleep(2000);
                        is.close();
                        _socket.close();
                        _socket = null;

                        btn.setText("连接");
                        tv_connectingDev.setText("当前连接设备：未连接");

                    }catch(IOException e){}
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        //清除按键响应函数
       btn_clear.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               smsg="";
               fmsg="";
               tv_in.setText(smsg);
               return;
           }
       });
    }

    // 通过重载onResume方法主动获取MainActivity参数
    @Override
    public void onResume() {
        super.onResume();
        Log.d("CALLBACK","Recovered!");
        if(_isStartGetBtAddress == true){
            _isStartGetBtAddress = false;
            String address = mActivity.mBtAddress;
            if (!address.isEmpty()){
                // 得到蓝牙设备句柄
                _device = _bluetooth.getRemoteDevice(address);
                // 用服务号得到socket
                try{
                    _socket = _device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
                }catch(IOException e){
                    Toast.makeText(mActivity, "连接失败！", Toast.LENGTH_SHORT).show();
                }
                //连接socket
                try{
                    _socket.connect();
                    Toast.makeText(mActivity, "连接"+_device.getName()+"成功！", Toast.LENGTH_SHORT).show();
                    tv_connectingDev.setText("当前连接设备："+_device.getName());
                    btn_connect.setText("断开");
                    mActivity.mBtSocket = _socket;
                    mActivity.mBtDevice = _device;
                }catch(IOException e){
                    try{
                        Toast.makeText(mActivity, "连接失败！", Toast.LENGTH_SHORT).show();
                        _socket.close();
                        _socket = null;
                    }catch(IOException ee){
                        Toast.makeText(mActivity, "连接失败！", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }

                //打开接收线程
                try{
                    is = _socket.getInputStream();   //得到蓝牙数据输入流
                }catch(IOException e){
                    Toast.makeText(mActivity, "接收数据失败！", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(bThread==false){
                    readThread.start();
                    bThread=true;
                }else{
                    bRun = true;
                }
            }
        }
    }

    //响应startActivityForResult()
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        Log.d("CALLBACK","Fragment2 has been called! result code: "+resultCode);
//        Toast.makeText(mActivity,"Get result form intent, code is: "+resultCode,Toast.LENGTH_LONG);
//        switch(requestCode){
//            case REQUEST_CONNECT_DEVICE:     //连接结果，由DeviceListActivity设置返回
//                // 响应返回结果
//                if (resultCode == Activity.RESULT_OK) {   //连接成功，由DeviceListActivity设置返回
//                    // MAC地址，由DeviceListActivity设置返回
//                    String address = data.getExtras()
//                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
//
//                }
//                break;
//            default:break;
//        }
//    }

    //接收数据线程
    Thread readThread=new Thread(){
        public void run(){
            int num = 0;
            byte[] buffer = new byte[1024];
            byte[] buffer_new = new byte[1024];
            int i = 0;
            int n = 0;
            bRun = true;
            //接收线程
            while(true){
                try{
                    while(is.available()==0){
                        while(bRun == false){}
                    }
                    while(true){
                        if(!bThread)//跳出循环
                            return;

                        num = is.read(buffer);         //读入数据
                        n=0;

                        String s0 = new String(buffer,0,num);
                        fmsg+=s0;    //保存收到数据
                        for(i=0;i<num;i++){
                            if((buffer[i] == 0x0d)&&(buffer[i+1]==0x0a)){
                                buffer_new[n] = 0x0a;
                                i++;
                            }else{
                                buffer_new[n] = buffer[i];
                            }
                            n++;
                        }
                        String s = new String(buffer_new,0,n);
                        smsg+=s;   //写入接收缓存
                        if(is.available()==0)break;  //短时间没有数据才跳出进行显示
                    }
                    //发送显示消息，进行显示刷新
                    handler.sendMessage(handler.obtainMessage());
                }catch(IOException e){
                }
            }
        }
    };

    //消息处理队列
    Handler handler= new Handler(){
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            tv_in.setText(smsg);   //显示数据
            sv.scrollTo(0,tv_in.getMeasuredHeight()); //跳至数据最后一页
            msgSize++;
            if(msgSize > 500) {
                msgSize=0;
                smsg="";
                fmsg="";
                tv_in.setText(smsg);
            }
        }
    };

    //关闭程序掉用处理部分
    public void onDestroy(){
        super.onDestroy();
        if(_socket!=null)  //关闭连接socket
            try{
                _socket.close();
            }catch(IOException e){}
        //	_bluetooth.disable();  //关闭蓝牙服务
    }

}
