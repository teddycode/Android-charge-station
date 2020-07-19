package com.zengjun.chargeClient;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;

import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kongzue.baseokhttp.HttpRequest;
import com.kongzue.baseokhttp.listener.ResponseListener;
import com.kongzue.baseokhttp.util.BaseOkHttp;
import com.kongzue.baseokhttp.util.Parameter;
import com.kongzue.dialog.v2.TipDialog;
import com.zengjun.chargeClient.fragment.Fragment1;
import com.zengjun.chargeClient.fragment.Fragment2;
import com.zengjun.chargeClient.fragment.Fragment3;


public class MainActivity extends AppCompatActivity {

    // 全局变量
    public final static int USER_ROLE_ADMIN = 1;
    public final static int USER_ROLE_COMMEN = 0;

    public final static int REQUEST_LOGIN_TOKEN    = 1;    //宏定义登录令牌请求
    public final static int REQUEST_CONNECT_DEVICE = 2;    //宏定义查询设备句柄
    public final static String BASE_SERVER_URL="http://129.211.127.83:8001/api/v1/";
    //public final static String BASE_SERVER_URL="http://192.168.43.243:8000/api/v1/";

    public static boolean mIsUserReady = false;
    public static boolean mIsBtConnected=false;

    public static String mUserToken = "";
    public static String mBtAddress= "";
    public static BluetoothSocket  mBtSocket;
    public static BluetoothDevice  mBtDevice;

    public static  int mCurrentCostID=-1;

    public static User mUser = new User();

    //一级解析
    private static class _result {
        int    code;
        JsonObject data;
        String msg;
    }

    private ViewPager mViewPager;
    private BottomNavigationView mNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigation = findViewById(R.id.navigation);
        mViewPager = findViewById(R.id.viewPager);
        mViewPager.setAdapter(new SimpleFragmentPagerAdapter(getSupportFragmentManager()));
        mViewPager.addOnPageChangeListener(mOnPageChangeListener);
        mNavigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivityForResult(intent, REQUEST_LOGIN_TOKEN);
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        if(mUserToken == "" && isTopActivity("LoginActivity")==false) {  //重新登录
//            Log.d("USERINFO","Relogin");
//            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
//            startActivityForResult(intent, REQUEST_LOGIN_TOKEN);
//        }else{  // 设置全局header请求头
//            BaseOkHttp.overallHeader = new Parameter().add("token", mUserToken);
//            GetUserInfo(); //获取用户信息
//        }
//    }
//
//    private boolean isTopActivity(String activityName){
//        ActivityManager manager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
//        List<ActivityManager.RunningTaskInfo> runningTaskInfos = manager.getRunningTasks(1);
//        String cmpNameTemp = null;
//        if(runningTaskInfos != null){
//            cmpNameTemp = runningTaskInfos.get(0).topActivity.toString();
//        }
//        if(cmpNameTemp == null){
//            return false;
//        }
//        return cmpNameTemp.equals(activityName);
//    }
    //     设置 viewpage 切换监听器
    private ViewPager.OnPageChangeListener mOnPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//            Toast.makeText(MainActivity.this, String.format("page scrolled! position %d, offset %f",position,positionOffset), Toast.LENGTH_SHORT).show();
        }

        @Override
//        page 被选中
        public void onPageSelected(int position) {
            mNavigation.getMenu().getItem(position).setChecked(true);
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    // 设置底部导航栏监听
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mViewPager.setCurrentItem(0);
                    return true;
                case R.id.navigation_category:
                    mViewPager.setCurrentItem(1);
                    return true;
                case R.id.navigation_my:
                    mViewPager.setCurrentItem(2);
                    return true;
            }
            return false;
        }
    };


    private class SimpleFragmentPagerAdapter extends FragmentPagerAdapter {

        private Fragment[] mFragment = new Fragment[]{new Fragment1(), new Fragment2(), new Fragment3()};
        private SimpleFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
        }
        @Override
        public Fragment getItem(int position) {
            return mFragment[position];
        }
        @Override
        public int getCount() {
            return mFragment.length;
        }

    }


    //获取登录返回token
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("CALLBACK","MainActivity has been called! request code:"+requestCode+"| result code: "+resultCode+" | data:"+data);
//        Toast.makeText(this,"Get result form intent, code is: "+resultCode,Toast.LENGTH_LONG);
        switch(requestCode){
            case REQUEST_CONNECT_DEVICE:     //蓝牙连接地址，由DeviceListActivity设置返回
                if(resultCode == RESULT_OK){
                    mBtAddress=data.getStringExtra("msg_BtAddress");
                    mIsBtConnected  = true;
                }else{
                    mBtAddress="";
                }
                break;
            case REQUEST_LOGIN_TOKEN:     //登录
                if (resultCode == RESULT_OK) {
                    mUserToken=data.getStringExtra("msg_loginToken");
                    if(mUserToken!=null){
                        mIsUserReady = true;
                        BaseOkHttp.overallHeader = new Parameter().add("Authorization", mUserToken); // 设置全局header请求头
                        GetUserInfo();
                    }
                }else{
                    mUserToken="";
                }
                break;
        }
    }

    public  void GetUserInfo(){
        String url = BASE_SERVER_URL+"currentuser";
        HttpRequest.GET(this,url,null,new ResponseListener() {
            public void onResponse(String response, Exception error) {
                if (error == null) {
                    Log.d("USERINFO",response);
                    try {
                        Gson gson = new Gson();
                        _result r = gson.fromJson(response, _result.class);
                        if(r.code == 200) {
                            JsonObject jsonObject = r.data;
                            mUser = gson.fromJson(jsonObject.get("data"),User.class);
                            mIsUserReady = true;
                            Log.d("USERINFO","User Info: "+mUser.toString()+" token"+mUserToken);
                        }else if(r.code == 401){
                            TipDialog.show(MainActivity.this,"授权失败，请重新登录",TipDialog.SHOW_TIME_SHORT,TipDialog.TYPE_FINISH);
                            if(mIsUserReady == false){
                                ForceLogOut();
                            }
                        }else {
                            Toast.makeText(MainActivity.this,"请求失败："+r.code+" msg:"+r.msg,Toast.LENGTH_LONG).show();
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

    public void ForceLogOut(){
        BaseOkHttp.overallHeader = new Parameter().add("Authorization", ""); // 设置全局header请求头
        mIsUserReady=false;
        mUser=new User();
        mUserToken = "";
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, REQUEST_LOGIN_TOKEN);
    }
}
