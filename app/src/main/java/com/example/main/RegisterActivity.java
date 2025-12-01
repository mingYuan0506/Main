package com.example.main;


import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class RegisterActivity extends AppCompatActivity {
    private DBManager DBManager;
    private EditText etStudentId, etPassword, etConfirmPassword;
    private TextView tvMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        DBManager = new DBManager(this);

        etStudentId = findViewById(R.id.et_student_id_register);
        etPassword = findViewById(R.id.et_password_register);
        etConfirmPassword = findViewById(R.id.et_confirm_password);

        // 注册按钮点击事件
        Button btnRegister = findViewById(R.id.btn_register);
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });

        // 跳转到登录界面
        TextView tvGoLogin = findViewById(R.id.tv_go_login);
        tvGoLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private void register() {
        String studentId = etStudentId.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // 输入验证
        if (studentId.isEmpty()) {
            tvMessage.setText("学号不能为空");
            return;
        }

        if (password.isEmpty()) {
            tvMessage.setText("密码不能为空");
            return;
        }

        if (!password.equals(confirmPassword)) {
            tvMessage.setText("两次密码不一致");
            return;
        }

        boolean isSuccess = DBManager.registerUser(studentId, password);
        if (isSuccess) {
            // 用Toast提示注册成功
            Toast.makeText(RegisterActivity.this, "注册成功！", Toast.LENGTH_SHORT).show();

            // 打印当前数据库所有用户
            DBManager.printAllUsers();  // 新增这行

            // 清空输入框
            etStudentId.setText("");
            etPassword.setText("");
            etConfirmPassword.setText("");

            // 可以自动跳转到登录界面（可选）
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        } else {
            // 用Toast提示注册失败
            Toast.makeText(RegisterActivity.this, "学号已存在或注册失败", Toast.LENGTH_SHORT).show();
        }
    }
}