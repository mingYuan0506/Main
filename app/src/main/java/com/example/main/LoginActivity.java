package com.example.main;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {
    private DBManager DBManager;
    private EditText etStudentId, etPassword;
    private TextView tvMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        DBManager = new DBManager(this);

        etStudentId = findViewById(R.id.et_student_id_login);
        etPassword = findViewById(R.id.et_password_login);
        tvMessage = findViewById(R.id.tv_login_message);

        // 登录按钮点击事件
        Button btnLogin = findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });

        // 跳转到注册界面
        TextView tvGoRegister = findViewById(R.id.tv_go_register);
        tvGoRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
    }

    private void login() {
        String studentId = etStudentId.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (studentId.isEmpty() || password.isEmpty()) {
            Toast.makeText(LoginActivity.this, "学号和密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isSuccess = DBManager.loginUser(studentId, password);
        if (isSuccess) {
            Toast.makeText(LoginActivity.this, "登录成功！", Toast.LENGTH_SHORT).show();

            // 登录成功！在这里加保存用户ID的代码
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            prefs.edit().putString("current_user_id", studentId).apply();

            // 传递用户ID到MainActivity
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("USER_ID", studentId); // 把登录的学号传过去
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(LoginActivity.this, "登录失败！", Toast.LENGTH_SHORT).show();
        }
    }
}