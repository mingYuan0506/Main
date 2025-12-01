package com.example.main;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class DetailActivity extends AppCompatActivity {
    private DBManager dbManager;
    private int activityId;
    private ActivityBean activityBean;
    // 当前登录用户（这里简化为固定值，实际应从登录页传递）
    private String currentUserId; // 接收传递的用户ID
    // 控件声明
    private TextView tvTitle, tvType, tvTime, tvLocation, tvOrganizer, tvApplyCount, tvDescription;
    private Button btnCollect, btnApply;

    private Button btnDebugPrint; // 调试按钮

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // 新增：配置导航条（返回首页）
        Toolbar toolbar = findViewById(R.id.toolbar); // 布局中新增的导航条
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // 显示返回箭头
        getSupportActionBar().setTitle("活动详情"); // 导航条标题


        // 获取传递的活动ID
        activityId = getIntent().getIntExtra("ACTIVITY_ID", -1);
        currentUserId = getIntent().getStringExtra("USER_ID");
        if (activityId == -1) {
            Toast.makeText(this, "参数错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (currentUserId == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化控件
        initViews();

        // 初始化数据库
        dbManager = new DBManager(this);

        // 加载活动详情
        loadActivityDetail();

        // 设置按钮点击事件
        setButtonListeners();
    }

    // 新增：导航条返回箭头点击事件（返回首页）
    @Override
    public boolean onSupportNavigateUp() {
        finish(); // 关闭当前详情页，返回上一页（首页）
        return true;
    }

    // 初始化控件
    private void initViews() {
        tvTitle = findViewById(R.id.tv_title);
        tvType = findViewById(R.id.tv_type);
        tvTime = findViewById(R.id.tv_time);
        tvLocation = findViewById(R.id.tv_location);
        tvOrganizer = findViewById(R.id.tv_organizer);
        tvApplyCount = findViewById(R.id.tv_apply_count);
        tvDescription = findViewById(R.id.tv_description);

        btnCollect = findViewById(R.id.btn_collect);
        btnApply = findViewById(R.id.btn_apply);

        btnDebugPrint = findViewById(R.id.btn_debug_print); // 绑定调试按钮
    }

    // 加载活动详情
    private void loadActivityDetail() {
        new Thread(() -> {
            // 获取活动基本信息
            activityBean = dbManager.getActivityById(activityId);
            if (activityBean == null) {
                new Handler(Looper.getMainLooper()).post(this::finish);
                return;
            }

            // 获取报名相关信息
            int currentPeople = dbManager.getCurrentPeopleByActivityId(activityId);
            boolean isApplied = dbManager.isUserApplied(currentUserId, activityId);
            boolean isCollected = dbManager.isUserCollected(currentUserId, activityId);
            boolean isFull = currentPeople >= activityBean.getMaxPeople();

            // 主线程更新UI
            new Handler(Looper.getMainLooper()).post(() -> {
                // 设置基本信息
                tvTitle.setText(activityBean.getTitle());
                tvType.setText("活动类型：" + activityBean.getType());
                tvTime.setText("活动时间：" + activityBean.getTime());
                tvLocation.setText("活动地点：" + activityBean.getLocation());
                tvOrganizer.setText("主办方：" + activityBean.getOrganizer());
                tvApplyCount.setText("报名情况：" + currentPeople + "/" + activityBean.getMaxPeople());
                tvDescription.setText("活动详情：\n" + activityBean.getDescription());

                // 更新收藏按钮状态
                btnCollect.setText(isCollected ? "已收藏" : "收藏");
                btnCollect.setBackgroundTintList(getResources().getColorStateList(
                        isCollected ? android.R.color.darker_gray : R.color.orange));

                // 更新报名按钮状态
                updateApplyButtonStatus(isFull, isApplied);
            });
        }).start();
    }

    // 更新报名按钮状态
    private void updateApplyButtonStatus(boolean isFull, boolean isApplied) {
        if (isApplied) {
            // 已报名
            btnApply.setText("取消报名");
            btnApply.setBackgroundTintList(getResources().getColorStateList(R.color.red));
            btnApply.setEnabled(true);
        }
        else if (isFull) {
            // 人数已满
            btnApply.setText("人数已满");
            btnApply.setBackgroundTintList(getResources().getColorStateList(android.R.color.darker_gray));
            btnApply.setEnabled(false);
        } else {
            // 可报名
            btnApply.setText("报名参加");
            btnApply.setBackgroundTintList(getResources().getColorStateList(R.color.blue));
            btnApply.setEnabled(true);
        }
    }

    // 设置按钮点击事件
    private void setButtonListeners() {
        // 收藏按钮
        btnCollect.setOnClickListener(v -> new Thread(() -> {
            boolean isCollected = dbManager.isUserCollected(currentUserId, activityId);
            boolean success;

            if (isCollected) {
                success = dbManager.uncollectActivity(currentUserId, activityId);
                if (success) {
                    // 取消收藏时，取消提醒
//                    ReminderUtils.cancelReminder(DetailActivity.this, activityId);
                }
            } else {
                success = dbManager.collectActivity(currentUserId, activityId);
                if (success) {
                    // 收藏时，设置提醒
//                    ReminderUtils.setReminder(DetailActivity.this, currentUserId, activityBean);
                }
            }

            if (success) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    btnCollect.setText(isCollected ? "收藏" : "已收藏");
                    btnCollect.setBackgroundTintList(getResources().getColorStateList(
                            isCollected ? R.color.orange : android.R.color.darker_gray));
                    Toast.makeText(DetailActivity.this,
                            isCollected ? "取消收藏成功" : "收藏成功",
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start());

        // 报名按钮
        btnApply.setOnClickListener(v -> new Thread(() -> {
            boolean isApplied = dbManager.isUserApplied(currentUserId, activityId);
            int currentPeople = dbManager.getCurrentPeopleByActivityId(activityId);
            boolean isFull = currentPeople >= activityBean.getMaxPeople();

            if (isFull) return; // 人数已满直接返回

            boolean success;
            if (isApplied) {
                // 取消报名
                success = dbManager.cancelApplyActivity(currentUserId, activityId);
                currentPeople--;
            } else {
                // 报名参加
                success = dbManager.applyActivity(currentUserId, activityId);
                currentPeople++;
            }

            if (success) {
                final int c = currentPeople;
                new Handler(Looper.getMainLooper()).post(() -> {
                    // 更新报名人数显示
                    tvApplyCount.setText("报名情况：" + c + "/" + activityBean.getMaxPeople());
                    // 更新按钮状态
                    updateApplyButtonStatus(c >= activityBean.getMaxPeople(), !isApplied);
                    // 提示信息
                    Toast.makeText(DetailActivity.this,
                            isApplied ? "取消报名成功" : "报名成功",
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start());

        // 调试按钮
        btnDebugPrint.setOnClickListener(v -> printAllTableData());
    }

    // 根据ID获取活动（需要在DBManager中实现）
    private ActivityBean getActivityById(int id) {
        // 这里调用DBManager的方法获取活动
        // 先获取所有活动再筛选（后续可优化为直接查询）
        for (ActivityBean bean : dbManager.getAllActivityList()) {
            if (bean.getId() == id) {
                return bean;
            }
        }
        return null;
    }

    // 打印所有表的数据
    private void printAllTableData() {
        new Thread(() -> {
            // 打印用户表
            dbManager.printUsersTable();
            // 打印活动表
            dbManager.printActivitiesTable();
            // 打印报名表
            dbManager.printRegistrationsTable();
            // 打印收藏表
            dbManager.printFavoritesTable();
            // 打印提醒表
            dbManager.printRemindersTable();

            // 主线程提示
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(this, "已打印所有表数据到Logcat", Toast.LENGTH_SHORT).show()
            );
        }).start();
    }
}