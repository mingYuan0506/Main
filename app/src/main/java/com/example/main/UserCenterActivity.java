package com.example.main;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class UserCenterActivity extends AppCompatActivity {
    private String currentUserId;
    private DBManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_center);

        // 新增：配置导航条（返回首页）
        Toolbar toolbar = findViewById(R.id.toolbar); // 布局中新增的导航条
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // 显示返回箭头
            getSupportActionBar().setTitle("用户中心"); // 导航条标题
        }


        // 获取当前用户ID
        currentUserId = getIntent().getStringExtra("USER_ID");
        dbManager = new DBManager(this);

        // 显示用户名
        TextView tvUsername = findViewById(R.id.tv_username);
        tvUsername.setText(currentUserId);

        // 【修改点 1】功能列表新增“活动总结助手”
        String[] functions = {"修改密码", "已收藏活动", "已报名活动", "个性化推荐", "活动总结助手"};
        ListView lvFunctions = findViewById(R.id.lv_functions);
        lvFunctions.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, functions));

        // 【修改点 2】功能列表点击事件新增 case
        lvFunctions.setOnItemClickListener((parent, view, position, id) -> {
            switch (position) {
                case 0:
                    // 修改密码
                    startActivity(new Intent(this, ChangePasswordActivity.class)
                            .putExtra("USER_ID", currentUserId));
                    break;
                case 1:
                    // 已收藏活动
                    startActivity(new Intent(this, FavoriteActivitiesActivity.class)
                            .putExtra("USER_ID", currentUserId));
                    break;
                case 2:
                    // 已报名活动
                    startActivity(new Intent(this, RegisteredActivitiesActivity.class)
                            .putExtra("USER_ID", currentUserId));
                    break;
                case 3:
                    // 个性化推荐
                    startActivity(new Intent(this, RecommendActivity.class)
                            .putExtra("USER_ID", currentUserId));
                    break;
                case 4:
                    // 【新增】活动总结助手
                    startActivity(new Intent(this, SummaryGeneratorActivity.class));
                    break;
            }
        });

        // 退出登录
        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish(); // 关闭当前详情页，返回上一页（首页）
        return true;
    }
}