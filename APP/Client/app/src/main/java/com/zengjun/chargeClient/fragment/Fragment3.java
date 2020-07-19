package com.zengjun.chargeClient.fragment;

import android.annotation.SuppressLint;
import android.content.DialogInterface;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;

import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.kongzue.baseokhttp.HttpRequest;
import com.kongzue.baseokhttp.listener.ResponseListener;

import com.kongzue.dialog.v2.CustomDialog;
import com.kongzue.dialog.v2.DialogSettings;
import com.kongzue.dialog.v2.MessageDialog;
import com.kongzue.dialog.v2.SelectDialog;
import com.kongzue.dialog.v2.TipDialog;
import com.kongzue.dialog.v2.WaitDialog;
import com.zengjun.chargeClient.MainActivity;
import com.zengjun.chargeClient.R;
import com.zengjun.chargeClient.RecordListActivity;
import com.zengjun.chargeClient.adapter.MultipleItemQuickAdapter;
import com.zengjun.chargeClient.bean.MultipleItem;
import com.zengjun.chargeClient.widget.CircleImageView;
import com.yechaoa.yutils.YUtils;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Fragment3 extends Fragment {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;

    private MultipleItem multipleItem;

    private List<MultipleItem> itemDataList;

    private MultipleItemQuickAdapter multipleItemQuickAdapter;

    private MainActivity mActivity;

    private TextView myHeaderName;
    private TextView myHeaderMobile;
    private ImageView myHeaderSettings;

    //一级解析
    private static class _result {
        int    code;
        JsonObject data;
        String msg;
    }
    // 二级: 消费记录
    public static class _Cost {
          int id;
          int user_id;
          double amount;
          long created_on;
          long end_on;
          double electric;
        boolean isNew;
        public _Cost(){ this.isNew = true;}
    }
    // 二级： 充值记录
    public static class _Incomes{
         int id;
         int user_id;
         double amount;
         String method;
         long created_on;
        boolean isNew;
        public _Incomes() {this.isNew = true;}
    }

    private ArrayList<_Cost> mCostList = new ArrayList<>();
    private ArrayList<_Incomes> mIncomesList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_fragment3, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mActivity = (MainActivity) getActivity();


        mSwipeRefreshLayout = mActivity.findViewById(R.id.swipeRefreshLayout);
        mRecyclerView = mActivity.findViewById(R.id.recyclerView);

        InitSwipeRefreshLayout();

        InitItemData();

        InitRecyclerView();

        InitListener();

        QueryCosts();

        QueryIncomes();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    private void InitSwipeRefreshLayout() {
        mSwipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        multipleItemQuickAdapter.notifyDataSetChanged();
                        mSwipeRefreshLayout.setRefreshing(false);
                        //更新数据
                        mActivity.GetUserInfo();
                        QueryCosts();
                        QueryIncomes();
                        //更新视图
                        if(myHeaderMobile!=null && myHeaderMobile!=null){
                            if(mActivity.mUser.getRole() == mActivity.USER_ROLE_ADMIN){
                                myHeaderName.setText(mActivity.mUser.getUsername()+"(管理员)");
                            }else{
                                myHeaderName.setText(mActivity.mUser.getUsername());
                            }
                            myHeaderMobile.setText(mActivity.mUser.getPhone());
                        }
                        //余额更新
                        itemDataList.get(1).mString1= mActivity.mUser.getBalance() +"元";
                        //更新列表
                        FreshItemLists();
                        YUtils.showToast("刷新完成");
                    }
                }, 1000);
            }
        });
    }

    private void InitItemData() {
        itemDataList = new ArrayList<>();

        //修改、注销
        multipleItem = new MultipleItem(MultipleItem.TYPE_COUNT, 5);
        multipleItem.mString1 = "修改密码";
        multipleItem.mString2 = "注销登录";
        itemDataList.add(multipleItem);
        // 我的余额
        multipleItem = new MultipleItem(MultipleItem.TYPE_BALANCE, 5);
        multipleItem.mString1 = mActivity.mUser.getBalance() +"元";
        itemDataList.add(multipleItem);
        // 我的充值记录
        multipleItem = new MultipleItem(MultipleItem.TYPE_ORDER_HEADER, 5);
        multipleItem.mString2 = "type2";
        itemDataList.add(multipleItem);

        //充值记录
        for (int i = 0; i < 5; i++) {
            multipleItem = new MultipleItem(MultipleItem.TYPE_ORDER, 1);
            multipleItem.mString1 = "充值记录";
            multipleItem.isShow = false;
            multipleItem.count = 0;
            itemDataList.add(multipleItem);
        }

        // 我的消费记录
        multipleItem = new MultipleItem(MultipleItem.TYPE_TOOLS_HEADER, 5);
        multipleItem.mString1 = "type5";
        itemDataList.add(multipleItem);
        //消费记录
        for (int i = 0; i < 5; i++) {
            multipleItem = new MultipleItem(MultipleItem.TYPE_TOOLS, 1);
            multipleItem.mString1 = "消费记录";
            multipleItem.isShow = false;
            multipleItem.count = 0;
            itemDataList.add(multipleItem);
        }

    }


    private void InitRecyclerView() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(mActivity, 5);
        mRecyclerView.setLayoutManager(gridLayoutManager);

        multipleItemQuickAdapter = new MultipleItemQuickAdapter(itemDataList);

        View headerView = getHeaderView(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.my_header_image:
                        YUtils.showToast("你点击了头像");
                        ModifyUserInfo();
                        break;
                    case R.id.my_header_settings:
                        YUtils.showToast("你点击了设置");
                        break;
                }
            }
        });

        multipleItemQuickAdapter.addHeaderView(headerView);
        mRecyclerView.setAdapter(multipleItemQuickAdapter);
    }


    private View getHeaderView(View.OnClickListener listener) {
        View headerView = getLayoutInflater().inflate(R.layout.layout_my_header, (ViewGroup) mRecyclerView.getParent(), false);

        CircleImageView myHeaderImage = headerView.findViewById(R.id.my_header_image);
        myHeaderImage.setImageResource(R.drawable.header_image);
        myHeaderImage.setOnClickListener(listener);

        myHeaderName = headerView.findViewById(R.id.my_header_name);
        if(mActivity.mUser.getRole() == mActivity.USER_ROLE_ADMIN){
            myHeaderName.setText(mActivity.mUser.getUsername()+"(管理员)");
        }else{
            myHeaderName.setText(mActivity.mUser.getUsername());
        }

        myHeaderMobile = headerView.findViewById(R.id.my_header_mobile);
        myHeaderMobile.setText(mActivity.mUser.getPhone());

        myHeaderSettings = headerView.findViewById(R.id.my_header_settings);
        myHeaderSettings.setOnClickListener(listener);

        return headerView;
    }


    private void InitListener() {
        multipleItemQuickAdapter.setSpanSizeLookup(new BaseQuickAdapter.SpanSizeLookup() {
            @Override
            public int getSpanSize(GridLayoutManager gridLayoutManager, int position) {
                return itemDataList.get(position).getSpanSize();
            }
        });

        multipleItemQuickAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                YUtils.showToast("第  " + position);
                // 显示记录详情
                if(position > 2 && position <=13){
                    ShowInDetails(position-3);
                }
            }
        });

        multipleItemQuickAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                switch (view.getId()) {
                    case R.id.my_favorites:
                        YUtils.showToast("修改密码");
                        ModifyPassword();
                        break;
                    case R.id.my_bands:
                        YUtils.showToast("注销登录");
                        LogoutCurrentUser();
                        break;
                    case R.id.ll_my_order:
                        YUtils.showToast("全部充值");
                        ShowAllRecords("incomes");
                        break;
                     case R.id.ll_my_cost:
                        YUtils.showToast("全部消费");
                         ShowAllRecords("cost");
                        break;
                    case R.id.my_balance_btn:
                        YUtils.showToast("立即充值");
                        AddBalance();
                     break;
                    case R.id.my_code_add:
                        YUtils.showToast("添加充值码");

                        if(mActivity.mUser.getRole() == mActivity.USER_ROLE_ADMIN){
                            Log.d("B1","2、init button");
                            AddCode();
                        }else{
                            YUtils.showToast("您不是管理员，无法添加充值码！");
                        }
                        break;
                }
            }
        });
    }

    // 添加充值码
    private void AddCode(){
        final boolean[] is_ok = {false};
        DialogSettings.style = DialogSettings.STYLE_IOS;
        CustomDialog.show(mActivity, R.layout.dialog_input, new CustomDialog.BindView() {
            @Override
            public void onBind(final CustomDialog dialog, View rootView) {
                //绑定布局
                final EditText edCode= rootView.findViewById(R.id.txt_dialog_code);
                final EditText edAmoutn= rootView.findViewById(R.id.txt_dialog_password);
                Button btnOk = rootView.findViewById(R.id.btn_selectPositive);      //确定
                Button btnCancle = rootView.findViewById(R.id.btn_selectNegative);  //取消
                final TextView title =rootView.findViewById(R.id.txt_dialog_title);
                title.setText("添加充值码");
                edCode.setHint("输入充值码");
                edAmoutn.setHint("输入金额");
                edAmoutn.setInputType(InputType.TYPE_CLASS_PHONE);
                //绑定事件
                btnOk.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String code = edCode.getText().toString();
                        float amount = Float.parseFloat(edAmoutn.getText().toString());

                        WaitDialog.show(mActivity, "正在添加...");

                        String url = mActivity.BASE_SERVER_URL + "code";
                        String jsonPara = "{\"code\":\"" + code + "\",\"amount\":" + amount+ "}";
                        HttpRequest.JSONPOST(mActivity, url, jsonPara, new ResponseListener() {
                            @Override
                            public void onResponse(String response, Exception error) {
                                if (error == null) {
                                    Log.d("COST",response);
                                    try {
                                        Gson gson = new Gson();
                                        _result r = gson.fromJson(response, _result.class);
                                        if(r.code == 200) {
                                            is_ok[0] = true;
                                        }else if(r.code == 401){
                                            TipDialog.show(mActivity,"授权失败，请重新登录",TipDialog.SHOW_TIME_SHORT,TipDialog.TYPE_FINISH);
                                        }
                                    } catch (Exception e) {
                                        Log.d("COST",e.toString());
                                    }
                                } else {
                                    Log.d("COST",error.toString());
                                }
                                dialog.doDismiss();
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        WaitDialog.dismiss();
                                        if(is_ok[0] == true){
                                            TipDialog.show(mActivity, "添加成功", TipDialog.SHOW_TIME_SHORT, TipDialog.TYPE_FINISH);
                                            mActivity.GetUserInfo();
                                        }else{
                                            TipDialog.show(mActivity, "添加失败", TipDialog.SHOW_TIME_SHORT, TipDialog.TYPE_ERROR);
                                        }

                                    }
                                }, 2000);
                            }
                        });
                    }

                });
                btnCancle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        TipDialog.show(mActivity, "取消", TipDialog.SHOW_TIME_SHORT, TipDialog.TYPE_WARNING);
                        dialog.doDismiss();
                    }
                });
            }
        });

//
    }
    //充值
    private void AddBalance(){
        final boolean[] is_ok = {false};
        DialogSettings.style = DialogSettings.STYLE_IOS;
        CustomDialog.show(mActivity, R.layout.dialog_input, new CustomDialog.BindView() {
            @Override
            public void onBind(final CustomDialog dialog, View rootView) {
                //绑定布局
                final EditText edCode= rootView.findViewById(R.id.txt_dialog_code);
                final EditText edPasswd= rootView.findViewById(R.id.txt_dialog_password);
                Button btnOk = rootView.findViewById(R.id.btn_selectPositive);      //确定
                Button btnCancle = rootView.findViewById(R.id.btn_selectNegative);  //取消
                //绑定事件
                btnOk.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String code = edCode.getText().toString();
                        String password = edPasswd.getText().toString();

                        YUtils.showToast("您输入了："+code+","+password);
                        WaitDialog.show(mActivity, "正在充值...");

                        String url = mActivity.BASE_SERVER_URL + "balance";
                        String jsonPara = "{\"code\":\"" + code + "\",\"password\":\"" +password + "\"}";
                        HttpRequest.JSONPOST(mActivity, url, jsonPara, new ResponseListener() {
                            @Override
                            public void onResponse(String response, Exception error) {
                                if (error == null) {
                                    Log.d("COST",response);
                                    try {
                                        Gson gson = new Gson();
                                        _result r = gson.fromJson(response, _result.class);
                                        if(r.code == 200) {
                                            is_ok[0] = true;
                                        }else if(r.code == 401){
                                            TipDialog.show(mActivity,"授权失败，请重新登录",TipDialog.SHOW_TIME_SHORT,TipDialog.TYPE_FINISH);
                                            mActivity.ForceLogOut();
                                        }
                                    } catch (Exception e) {
                                        Log.d("COST",e.toString());
                                    }
                                } else {
                                    Log.d("COST",error.toString());
                                }
                                dialog.doDismiss();
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        WaitDialog.dismiss();
                                        if(is_ok[0] == true){
                                            TipDialog.show(mActivity, "充值成功", TipDialog.SHOW_TIME_SHORT, TipDialog.TYPE_FINISH);
                                            mActivity.GetUserInfo();
                                        }else{
                                            TipDialog.show(mActivity, "充值失败", TipDialog.SHOW_TIME_SHORT, TipDialog.TYPE_ERROR);
                                        }

                                    }
                                }, 2000);
                            }
                        });
                    }

                });
                btnCancle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        TipDialog.show(mActivity, "充值取消", TipDialog.SHOW_TIME_SHORT, TipDialog.TYPE_WARNING);
                        dialog.doDismiss();
                    }
                });
            }
        });

//
    }
    //修改个人信息
    private void  ModifyUserInfo(){
        final boolean[] is_ok = {false};
        DialogSettings.style = DialogSettings.STYLE_IOS;
        CustomDialog.show(mActivity, R.layout.dialog_input, new CustomDialog.BindView() {
            @Override
            public void onBind(final CustomDialog dialog, View rootView) {
                //绑定布局
                final TextView title =rootView.findViewById(R.id.txt_dialog_title);
                final EditText edName= rootView.findViewById(R.id.txt_dialog_code);
                final EditText edMobile= rootView.findViewById(R.id.txt_dialog_password);
                Button btnOk = rootView.findViewById(R.id.btn_selectPositive);      //确定
                Button btnCancle = rootView.findViewById(R.id.btn_selectNegative);  //取消

                title.setText("修改个人信息");
                edName.setHint("输入新用户名");
                edMobile.setHint("输入新手机号");
                edMobile.setInputType(InputType.TYPE_CLASS_PHONE);

                //绑定事件
                btnOk.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String name = edName.getText().toString();
                        String mobile = edMobile.getText().toString();
                        YUtils.showToast("您输入了："+name+","+mobile);

                        String url = mActivity.BASE_SERVER_URL + "modify";
                        String jsonPara = "{\"username\":\"" + name + "\",\"phone\":\"" + mobile + "\"}";
                        HttpRequest.JSONPOST(mActivity, url, jsonPara, new ResponseListener() {
                            @Override
                            public void onResponse(String response, Exception error) {
                                if (error == null) {
                                    Log.d("COST",response);
                                    try {
                                        Gson gson = new Gson();
                                        _result r = gson.fromJson(response, _result.class);
                                        if(r.code == 200) {
                                            is_ok[0] = true;
                                        }else if(r.code == 401){
                                            TipDialog.show(mActivity,"授权失败，请重新登录",TipDialog.SHOW_TIME_SHORT,TipDialog.TYPE_FINISH);
                                        }
                                    } catch (Exception e) {
                                        Log.d("COST",e.toString());
                                    }
                                } else {
                                    Log.d("COST",error.toString());
                                }
                                if(is_ok[0] == true){
                                    TipDialog.show(mActivity, "修改成功", TipDialog.SHOW_TIME_SHORT, TipDialog.TYPE_FINISH);
                                    mActivity.GetUserInfo();
                                }else{
                                    TipDialog.show(mActivity, "修改失败", TipDialog.SHOW_TIME_SHORT, TipDialog.TYPE_ERROR);
                                }
                                dialog.doDismiss();
                            }
                        });
                    }
                });
                btnCancle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        TipDialog.show(mActivity, "修改取消", TipDialog.SHOW_TIME_SHORT, TipDialog.TYPE_WARNING);
                        dialog.doDismiss();
                    }
                });
            }
        });
    }
    //修改密码
    private void  ModifyPassword(){
        DialogSettings.style = DialogSettings.STYLE_IOS;
        final boolean[] is_ok = {false};
        DialogSettings.style = DialogSettings.STYLE_IOS;
        CustomDialog.show(mActivity, R.layout.dialog_input, new CustomDialog.BindView() {
            @Override
            public void onBind(final CustomDialog dialog, View rootView) {
                //绑定布局
                final TextView title =rootView.findViewById(R.id.txt_dialog_title);
                final EditText edName= rootView.findViewById(R.id.txt_dialog_code);
                final EditText edMobile= rootView.findViewById(R.id.txt_dialog_password);
                Button btnOk = rootView.findViewById(R.id.btn_selectPositive);      //确定
                Button btnCancle = rootView.findViewById(R.id.btn_selectNegative);  //取消

                title.setText("修改密码");
                edName.setHint("输入旧密码");
                edMobile.setHint("输入新密码");

                //绑定事件
                btnOk.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String oldPassword = edName.getText().toString();
                        String newPassword = edMobile.getText().toString();
                        YUtils.showToast("您输入了："+oldPassword+","+newPassword);

                        String url = mActivity.BASE_SERVER_URL + "password";
                        String jsonPara = "{\"old_password\":\"" + oldPassword + "\",\"new_password\":\"" + newPassword + "\"}";
                        HttpRequest.JSONPOST(mActivity, url, jsonPara, new ResponseListener() {
                            @Override
                            public void onResponse(String response, Exception error) {
                                if (error == null) {
                                    Log.d("COST",response);
                                    try {
                                        Gson gson = new Gson();
                                        _result r = gson.fromJson(response, _result.class);
                                        if(r.code == 200) {
                                            is_ok[0] = true;
                                        }else if(r.code == 401){
                                            TipDialog.show(mActivity,"授权失败，请重新登录",TipDialog.SHOW_TIME_SHORT,TipDialog.TYPE_FINISH);
                                            mActivity.ForceLogOut();
                                        }
                                    } catch (Exception e) {
                                        Log.d("COST",e.toString());
                                    }
                                } else {
                                    Log.d("COST",error.toString());
                                }
                                if(is_ok[0] == true){
                                    TipDialog.show(mActivity, "修改成功", TipDialog.SHOW_TIME_SHORT, TipDialog.TYPE_FINISH);
                                }else{
                                    TipDialog.show(mActivity, "修改失败", TipDialog.SHOW_TIME_SHORT, TipDialog.TYPE_ERROR);
                                }
                                dialog.doDismiss();
                            }
                        });
                    }
                });
                btnCancle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        TipDialog.show(mActivity, "修改取消", TipDialog.SHOW_TIME_SHORT, TipDialog.TYPE_WARNING);
                        dialog.doDismiss();
                    }
                });
            }
        });

    }
    //查询消费记录
    private void  QueryCosts(){
        String url = mActivity.BASE_SERVER_URL+"costs";
        HttpRequest.GET(mActivity,url,null,new ResponseListener() {
            public void onResponse(String response, Exception error) {
                if (error == null) {
                    Log.d("LIST",response);
                    try {
                        Gson gson = new Gson();
                        _result r = gson.fromJson(response, _result.class);
                        if(r.code == 200) {
                            JsonObject jsonObject = r.data;
                            JsonArray jsonArray = jsonObject.getAsJsonArray("data");
                            mCostList.clear();
                            //循环遍历
                            for (JsonElement cost : jsonArray) {
                                //通过反射 得到_Cost.class
                                _Cost costBean = gson.fromJson(cost, new TypeToken<_Cost>() {}.getType());
                                mCostList.add(costBean);
                                Log.d("LIST","Cost : "+ costBean.toString());
                            }
                            FreshItemLists();
                        }else if(r.code == 401){
                            TipDialog.show(mActivity,"授权失败，请重新登录",TipDialog.SHOW_TIME_SHORT,TipDialog.TYPE_FINISH);
                        }else {
                            Toast.makeText(mActivity,"请求失败："+r.code+" msg:"+r.msg,Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.d("USERINFO",e.toString());
                    }
                } else {
                    Log.d("USERINFO",error.toString());
                }
            }
        });
    }
    //查询充值记录
    private void QueryIncomes(){
        String url = mActivity.BASE_SERVER_URL+"incomes";
        HttpRequest.GET(mActivity,url,null,new ResponseListener() {
            public void onResponse(String response, Exception error) {
                if (error == null) {
                    Log.d("LIST",response);
                    try {
                        Gson gson = new Gson();
                        _result r = gson.fromJson(response, _result.class);
                        if(r.code == 200) {
                            JsonObject jsonObject = r.data;
                            JsonArray jsonArray = jsonObject.getAsJsonArray("data");
                            mIncomesList.clear();
                            //循环遍历
                            for (JsonElement income : jsonArray) {
                                //通过反射 得到_Cost.class
                                _Incomes incomeBean = gson.fromJson(income, new TypeToken<_Incomes>() {}.getType());
                                mIncomesList.add(incomeBean);
                                Log.d("LIST","Cost : "+ incomeBean.toString());
                            }
                            FreshItemLists();
                        }else if(r.code == 401){
                            TipDialog.show(mActivity,"授权失败，请重新登录",TipDialog.SHOW_TIME_SHORT,TipDialog.TYPE_FINISH);
                        }else {
                            Toast.makeText(mActivity,"请求失败："+r.code+" msg:"+r.msg,Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.d("USERINFO",e.toString());
                    }
                } else {
                    Log.d("USERINFO",error.toString());
                }
            }
        });

    }
    //登出用户
    private  void LogoutCurrentUser(){
        final boolean[] is_ok = {false};
        DialogSettings.style = DialogSettings.STYLE_IOS;
        SelectDialog.show(mActivity, "注销提示", "确认是否注销当前登录账户", "确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                Toast.makeText(mActivity, "您点击了确定按钮", Toast.LENGTH_SHORT).show();

                String url = mActivity.BASE_SERVER_URL + "logout";
                HttpRequest.JSONPOST(mActivity, url, null, new ResponseListener() {
                    @Override
                    public void onResponse(String response, Exception error) {
                        if (error == null) {
                            Log.d("COST",response);
                            try {
                                Gson gson = new Gson();
                                _result r = gson.fromJson(response, _result.class);
                                if(r.code == 200 || r.code == 401) {
                                    is_ok[0] = true;
                                    mActivity.ForceLogOut();
                                }
                            } catch (Exception e) {
                                Log.d("COST",e.toString());
                            }
                        } else {
                            Log.d("COST",error.toString());
                        }
                        if(is_ok[0] == true){
                            TipDialog.show(mActivity,"注销完成，请重新登录",TipDialog.SHOW_TIME_SHORT,TipDialog.TYPE_FINISH);
                            mActivity.ForceLogOut();
                        }else{
                            TipDialog.show(mActivity, "注销失败", TipDialog.SHOW_TIME_SHORT, TipDialog.TYPE_ERROR);
                        }
                        dialog.dismiss();
                    }
                });
            }
        }, "取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(mActivity, "取消", Toast.LENGTH_SHORT).show();
            }
        });
    }
    //刷新记录
    private  void FreshItemLists(){
        for (int i = 0; i < 5; i++) {
            multipleItem = itemDataList.get(i+3);
            if( i < mIncomesList.size()){
                _Incomes income = mIncomesList.get(i);
                if(income.isNew == true){
                    income.isNew = false;
                    multipleItem.count=1;
                    multipleItem.isShow=true;
                    mIncomesList.set(i, income);
                }else{
                    multipleItem.count =0;
                    multipleItem.isShow =false;
                }
                multipleItem.mString1 = new SimpleDateFormat("MM-dd HH:mm").format(new Date(income.created_on*1000L));
            }else {
                multipleItem.mString1 = "--";
                multipleItem.isShow = false;
            }
            multipleItem = itemDataList.get(i+9);
            if(i<mCostList.size()){
                _Cost cost = mCostList.get(i);
                if  (cost.isNew == true)  {
                    cost.isNew = false;
                    multipleItem.count =1;
                    multipleItem.isShow =true;
                    mCostList.set(i,cost);
                }else{
                    multipleItem.count =0;
                    multipleItem.isShow =false;
                }
                multipleItem.mString1 = new SimpleDateFormat("MM-dd HH:mm").format(new Date(cost.created_on*1000L));
            }else{
                multipleItem.mString1 = "--";
                multipleItem.isShow = false;
            }
        }
    }
    // 显示单条记录
    private void ShowInDetails(int index){
        String msg = "未知";
        if(index >= 0 &&  index < 5)
        {
            if(index < mIncomesList.size()){
                _Incomes income = mIncomesList.get(index);
               // final Date date = new Date(income.created_on);
              //  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
               // final String strTime = sdf.format(date);
                msg =String.format("充值时间：%s\n充值金额：%.2f元\n充值方式：%s",
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(income.created_on*1000L)),
                        income.amount,income.method);
            }
        }else if(index>5 && index <=10){
            if(index-6< mCostList.size()){
                _Cost cost = mCostList.get(index-6);
                msg = String.format("消费时间：%s\n消费金额%.2f元\n耗电量：%.2f W",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(cost.created_on*1000L)),
                        cost.amount,cost.electric);
            }
        }
        MessageDialog.show(mActivity, "记录详情", msg, "知道了", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }
    //显示所有记录
    @SuppressLint("DefaultLocale")
    void ShowAllRecords(String title){
        String[] records = new String[100];
        Intent intent = new Intent(mActivity,RecordListActivity.class);
        if(title == "cost"){
            intent.putExtra("title", "全部消费记录");
            for(int i=0; i< mCostList.size();i++){
                _Cost cost = mCostList.get(i);
                records[i] = String.format("时间：%s\n金额%.2f元\t\t耗电量：%.2fW",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(cost.created_on*1000L)),
                        cost.amount,cost.electric);
            }
        }else {
            intent.putExtra("title", "全部充值记录");
            for(int i=0; i< mIncomesList.size();i++){
                _Incomes income = mIncomesList.get(i);
                records[i] =String.format("时间：%s\n金额：%.2f元\t\t充值方式：%s",
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(income.created_on*1000L)),
                        income.amount,income.method);
            }
        }

        Bundle b=new Bundle();
        b.putStringArray("records", records);
        intent.putExtras(b);

        mActivity.startActivity(intent);
    }
}
