package com.zengjun.chargeClient;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.kongzue.baseokhttp.HttpRequest;
import com.kongzue.baseokhttp.listener.ResponseListener;
import com.kongzue.dialog.v2.DialogSettings;
import com.kongzue.dialog.v2.MessageDialog;

public class SignupActivity extends AppCompatActivity {
    private static final String TAG = "SignupActivity";

     private EditText _nameText;
     private EditText _emailText;
     private EditText _mobileText;
     private EditText _passwordText;
     private EditText _reEnterPasswordText;
     private Button _signupButton;
     private TextView _loginLink;


    private static class Data{
        String id;
    }
    private class Result {
        int    code;
        Data   data;
        String msg;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        _nameText = findViewById(R.id.input_name);

        _emailText  =findViewById(R.id.input_email);
        _mobileText =findViewById(R.id.input_mobile);
        _passwordText=findViewById(R.id.input_password);
        _reEnterPasswordText=findViewById(R.id.input_reEnterPassword);
        _signupButton=findViewById(R.id.btn_signup);
        _loginLink=findViewById(R.id.link_login);


        _signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signup();
            }
        });

        _loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish the registration screen and return to the Login activity
                Intent intent = new Intent(getApplicationContext(),LoginActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
            }
        });
    }

    // 返回键处理
    @Override
    public void onBackPressed() {
        // call the superclass method first
        Intent intent = new Intent(getApplicationContext(),LoginActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
    }
    public void signup() {
        Log.d(TAG, "Signup");

        if (!validate()) {
            onSignupFailed("输入有误");
            return;
        }

        _signupButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(SignupActivity.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("正在创建账户...");
        progressDialog.show();

        String name = _nameText.getText().toString();
        String email = _emailText.getText().toString();
        String phone = _mobileText.getText().toString();
        String password = _passwordText.getText().toString();
        String reEnterPassword = _reEnterPasswordText.getText().toString();


        String url = MainActivity.BASE_SERVER_URL+"reg";
        String jsonPara =  "{\"email\":\""+email+"\",\"password\":\""+password+"\",\"password_again\":\""+reEnterPassword+"\","+"\"phone\":\""+phone+"\",\"username\":\""+name+"\"}";
        HttpRequest.JSONPOST(this, url,jsonPara , new ResponseListener() {
            @Override
            public void onResponse(String response, Exception error) {
                if (error == null) {
                    try {
                        Gson gson = new Gson();
                        Result result = gson.fromJson(response, Result.class);
                        if(result.code == 200) {
                            onSignupSuccess();
                        }else{
                            Toast.makeText(SignupActivity.this, "注册失败："+result.msg, Toast.LENGTH_SHORT).show();
                            onSignupFailed(result.msg);
                        }
                    } catch (Exception e) {
                        onSignupFailed(e.toString());
                        Log.d(TAG,e.toString());
                        Toast.makeText(SignupActivity.this, "返回参数错误："+e.toString(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    onSignupFailed(error);
                    Log.d(TAG,error.toString());
                }
                progressDialog.dismiss();
             }
        });
    }


    public void onSignupSuccess() {
        final String email = _emailText.getText().toString();
        final String password = _passwordText.getText().toString();
        String msg = String.format("您的账号为：%s\n您的密码为：%s\n",email,password);
        DialogSettings.style = DialogSettings.STYLE_IOS;
        MessageDialog.show(this, "注册成功！", msg, "立即去登录", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
//                Intent intent = new Intent();
//                intent.putExtra("email", email);
//                intent.putExtra("password", password);
//                SignupActivity.this.setResult(RESULT_OK, intent);

//                Intent intent = new Intent(getApplicationContext(),LoginActivity.class);
////                intent.putExtra("email",email);
////                intent.putExtra("passwod",password);
//                startActivity(intent);
//                finish();
//                SignupActivity.this.finish();
                dialog.dismiss();
                Intent intent = new Intent(getApplicationContext(),LoginActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.push_left_out, R.anim.push_left_in);

            }
        });

        _signupButton.setEnabled(true);
    }

    public void onSignupFailed(Object e) {
        Toast.makeText(getBaseContext(), "注册失败:"+e.toString(), Toast.LENGTH_LONG).show();
        _signupButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String name = _nameText.getText().toString();
        String email = _emailText.getText().toString();
        String mobile = _mobileText.getText().toString();
        String password = _passwordText.getText().toString();
        String reEnterPassword = _reEnterPasswordText.getText().toString();

        if (name.isEmpty() || name.length() < 3) {
            _nameText.setError("至少三个字符");
            valid = false;
        } else {
            _nameText.setError(null);
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError("请输入有效的地址");
            valid = false;
        } else {
            _emailText.setError(null);
        }

        if (mobile.isEmpty() || mobile.length()!=11) {
            _mobileText.setError("请输入有效的手机号");
            valid = false;
        } else {
            _mobileText.setError(null);
        }

        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            _passwordText.setError("密码长度为4~9");
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        if (reEnterPassword.isEmpty() || reEnterPassword.length() < 4 || reEnterPassword.length() > 10 || !(reEnterPassword.equals(password))) {
            _reEnterPasswordText.setError("密码不匹配！");
            valid = false;
        } else {
            _reEnterPasswordText.setError(null);
        }
        return valid;
    }
}