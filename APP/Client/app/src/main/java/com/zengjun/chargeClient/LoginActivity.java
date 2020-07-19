package com.zengjun.chargeClient;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.kongzue.baseokhttp.HttpRequest;
import com.kongzue.baseokhttp.listener.ResponseListener;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_SIGNUP = 0;

    private static class Data{
        String token;
    }
    private class Result {
        int    code;
        Data   data;
        String msg;
    }

    private EditText _emailText;
    private EditText _passwordText;
    private Button _loginButton;
    private TextView _signupLink;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        _loginButton = findViewById(R.id.btn_login);
        _emailText = findViewById(R.id.input_email);
        _passwordText = findViewById(R.id.input_password);
        _signupLink = findViewById(R.id.link_signup);

        // for debug
        _emailText.setText("105533@qq.com");
        _passwordText.setText("123456");

        _loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                login();
            }
        });

        _signupLink.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Start the Signup activity
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivityForResult(intent, REQUEST_SIGNUP);
                finish();
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out); // activity 切换效果
            }
        });
    }

    // 处理 登录逻辑
    public void login() {
        Log.d(TAG, "Login");

        if (!validate()) {
            onLoginFailed("输入有误！");
            return;
        }

        _loginButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("正在登录...");
        progressDialog.show();

        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        String url = MainActivity.BASE_SERVER_URL+"auth";
        String jsonPara =  "{\"email\":\""+email+"\",\"password\":\""+password+"\"}";
        HttpRequest.JSONPOST(this, url,jsonPara , new ResponseListener() {
            @Override
            public void onResponse(String response, Exception error) {
                if (error == null) {
                    Log.d(TAG,response);
                    try {
                        Gson gson = new Gson();
                        Result result = gson.fromJson(response,Result.class);
                        if(result.code == 200) {
                            onLoginSuccess(result.data.token);
                        }else{
                            onLoginFailed(result.msg);
                        }
                    } catch (Exception e) {
                        onLoginFailed(e.toString());
                        Log.d(TAG,e.toString());
                    }
                } else {
                    onLoginFailed(error.toString());
                    Log.d(TAG,error.toString());
                }
                progressDialog.dismiss();
            }
        });
    }

//    注册成功后直接登录
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        Log.d("CALLBACK","LoginActivity has been called! result code: "+resultCode);
//        if (requestCode == REQUEST_SIGNUP) {
//            if (resultCode == RESULT_OK) {
//                _emailText.setText(data.getStringExtra("email"));
//                _passwordText.setText(data.getStringExtra("password"));
//            }
//        }
//    }

    //处理返回键
    @Override
    public void onBackPressed() {
        // Disable going back to the MainActivity
        moveTaskToBack(true);
    }

    public void onLoginSuccess(String token) {
        Toast.makeText(this,"登录成功！",Toast.LENGTH_SHORT);
        Intent data = new Intent();
        data.putExtra("msg_loginToken",token);
        setResult(RESULT_OK, data);
        _loginButton.setEnabled(true);
        finish();
    }

    public void onLoginFailed(String msg) {
        Toast.makeText(getBaseContext(), "登录失败: "+msg, Toast.LENGTH_LONG).show();
        _loginButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError("无效的邮箱地址");
            valid = false;
        } else {
            _emailText.setError(null);
        }

        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            _passwordText.setError("密码长度在4~9之间");
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        return valid;
    }
}
