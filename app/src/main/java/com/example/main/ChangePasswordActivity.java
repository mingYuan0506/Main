package com.example.main;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ChangePasswordActivity extends AppCompatActivity {
    private String currentUserId;
    private DBManager dbManager;
    private EditText etOldPassword, etNewPassword, etConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // 新增：配置导航条（返回首页）
        Toolbar toolbar = findViewById(R.id.toolbar); // 布局中新增的导航条
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // 显示返回箭头
        getSupportActionBar().setTitle("修改密码"); // 导航条标题

        // 获取当前用户ID
        currentUserId = getIntent().getStringExtra("USER_ID");
        dbManager = new DBManager(this);

        // 初始化控件
        etOldPassword = findViewById(R.id.et_old_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        Button btnSubmit = findViewById(R.id.btn_submit);

        // 提交按钮点击事件
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePassword();
            }
        });
    }

    private void changePassword() {
        String oldPassword = etOldPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // 输入验证
        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "请填写所有字段", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "两次输入的新密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(this, "新密码长度不能少于6位", Toast.LENGTH_SHORT).show();
            return;
        }

        // 验证原密码
        if (!dbManager.loginUser(currentUserId, oldPassword)) {
            Toast.makeText(this, "原密码错误", Toast.LENGTH_SHORT).show();
            return;
        }

        // 更新密码
        boolean success = dbManager.updatePassword(currentUserId, newPassword);
        if (success) {
            Toast.makeText(this, "密码修改成功", Toast.LENGTH_SHORT).show();
            finish(); // 返回上一级页面
        } else {
            Toast.makeText(this, "密码修改失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish(); // 关闭当前详情页，返回上一页（首页）
        return true;
    }
}