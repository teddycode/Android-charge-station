package com.zengjun.chargeClient.fragment;

import android.content.DialogInterface;
import android.os.Bundle;

import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kongzue.baseokhttp.HttpRequest;
import com.kongzue.baseokhttp.listener.ResponseListener;
import com.kongzue.dialog.v2.SelectDialog;
import com.zengjun.chargeClient.MainActivity;
import com.zengjun.chargeClient.R;

import java.io.IOException;
import java.io.OutputStream;

public class Fragment1 extends Fragment {

    private static String _chargeStatus;

    private ImageView ivCharge;
    private Button btnChangePreTime;
    private TextView tvCurYuan;
    private Chronometer chSpendTime;
    private TextInputEditText edPreChargeTime;
    private MainActivity mActivity;

    private int preChargeTime=0;
    private float electric = (float) 1.0;

    //一级解析
    private static class _result {
        int    code;
        JsonObject data;
        String msg;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_fragment1, container, false);
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

            mActivity= (MainActivity) getActivity();
            ivCharge = mActivity.findViewById(R.id.img_charge);
            btnChangePreTime=mActivity.findViewById(R.id.btnChagePreTime);
            tvCurYuan = mActivity.findViewById(R.id.tvCurYuan);
            chSpendTime = mActivity.findViewById(R.id.chSpendTime);
            edPreChargeTime = mActivity.findViewById(R.id.edChargePreTime);
            _chargeStatus = "ready_start";
            chSpendTime.setFormat("%s");
            int _hour = (int) ((SystemClock.elapsedRealtime() - chSpendTime.getBase()) / 1000 / 60);
            chSpendTime.setFormat("0"+String.valueOf(_hour)+":%s");
            InitLisetners();
    }

    // 重绘界面
    @Override
    public void onResume() {
        super.onResume();
        tvCurYuan.setText(String.format("%.2f元",mActivity.mUser.getBalance()));
    }

    //初始化监听器
    public void InitLisetners(){
        final float[] total = new float[1];
        // 修改充电时长监听器
        btnChangePreTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        //充电按键监听器
        ivCharge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mActivity.mUserToken == "") {
                    Toast.makeText(mActivity,"请先登录！",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(_chargeStatus == "ready_start"){           // 处于准备充电状态
                    if(mActivity.mIsBtConnected == true) {    //蓝牙是否连接
                        if(edPreChargeTime.getText().toString().length()==0){
                            Toast.makeText(mActivity,"请输入预充电时长",Toast.LENGTH_SHORT).show();
                            return;
                        }else{
                            preChargeTime = Integer.parseInt(edPreChargeTime.getText().toString());
                            total[0] = (float) (preChargeTime /3.0);
                            if(mActivity.mUser.getBalance() > total[0]){        //检查余额是否足够
                                String url = mActivity.BASE_SERVER_URL+"cost";
                                String jsonPara =  "{\"amount\":"+total[0]+",\"charge_time\":"+preChargeTime+"}";
                                HttpRequest.JSONPOST(mActivity,url,jsonPara,new ResponseListener() {
                                        @Override
                                        public void onResponse(String response, Exception error) {
                                        if (error == null) {
                                            Log.d("COST",response);
                                            try {
                                                Gson gson = new Gson();
                                                _result r = gson.fromJson(response, _result.class);
                                                if(r.code == 200) {
                                                    JsonObject jsonObject = r.data;
                                                    mActivity.mCurrentCostID = jsonObject.get("data").getAsInt();
                                                    mActivity.mUser.reduceBalance(total[0]);
                                                    StartCharge(preChargeTime);
                                                }else{
                                                    Toast.makeText(mActivity,"请求失败："+r.code+" msg:"+r.msg,Toast.LENGTH_LONG).show();
                                                }
                                            } catch (Exception e) {
                                                Log.d("COST",e.toString());
                                            }
                                        } else {
                                            Log.d("COST",error.toString());
                                        }
                                    }
                                });
                            }else{
                                Toast.makeText(mActivity,"余额不足！",Toast.LENGTH_SHORT).show();
                            }
                        }
                    }else{
                        Toast.makeText(mActivity,"请先连接蓝牙",Toast.LENGTH_LONG).show();
                    }
                }else{
                    SelectDialog.show(mActivity, "结束提醒", "确认是否结束当前充电？", "确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    StopCharge();
                                    dialog.dismiss();
                                }
                            }, "取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(mActivity, "取消", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    public void  StartCharge(int time){
        edPreChargeTime.setFocusable(false);
        _chargeStatus = "ready_stop";
        ivCharge.setImageResource(R.drawable.charge_stop);
        tvCurYuan.setText(String.format("%.2f元",mActivity.mUser.getBalance()));
        chSpendTime.start();
        // 蓝牙下发指令
        try {
            int i,n=0;
            if(time>9) time =9;
            else if(time <0) time=0;
            OutputStream os = mActivity.mBtSocket.getOutputStream();   //蓝牙连接输出流
            byte[] bos = new String("*11"+time +"#").getBytes();
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
    public void StopCharge() {
        String url = mActivity.BASE_SERVER_URL + "updatecost";
        String jsonPara = "{\"electric\":" + electric + ",\"id\":" + mActivity.mCurrentCostID + "}";
        HttpRequest.JSONPOST(mActivity, url, jsonPara, new ResponseListener() {
            @Override
            public void onResponse(String response, Exception error) {
                if (error == null) {
                    Log.d("COST",response);
                    try {
                        Gson gson = new Gson();
                        _result r = gson.fromJson(response, _result.class);
                        if(r.code == 200) {
                            Toast.makeText(mActivity,"记录上传成功！",Toast.LENGTH_LONG).show();
                        }else{
                            Toast.makeText(mActivity,"记录上传失败："+r.code+" msg:"+r.msg,Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.d("COST",e.toString());
                    }
                } else {
                    Log.d("COST",error.toString());
                }
            }
        });

        _chargeStatus = "ready_start";
        ivCharge.setImageResource(R.drawable.charge_start);
        edPreChargeTime.setFocusable(true);
        chSpendTime.stop();
        chSpendTime.setBase(SystemClock.elapsedRealtime());
        //下发指令

        chSpendTime.start();
        // 蓝牙下发指令
        try {
            int i,n=0;
            OutputStream os = mActivity.mBtSocket.getOutputStream();   //蓝牙连接输出流
            byte[] bos = new String("*120#").getBytes();
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
}
