package com.example.main;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    // 数据库和数据模型
    private DBManager dbManager;
    private List<ActivityBean> activityBeanList;
    private String currentUserId;
    private String currentDate;

    // UI 控件
    private ListView listView;
    private CalendarView calendarView;
    private TextView tvCurrentDate;
    private ImageButton btnToggleView;
    private LinearLayout calendarContainer;
    private TextView tvListTitle;

    // 标签筛选相关
    private String selectedTag = null;
    private List<Button> tagButtons = new ArrayList<>();

    // 视图状态变量
    private boolean isAllActivitiesView = false; // false = 日历视图, true = 总览视图

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 动态申请通知权限 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // 初始化所有视图控件
        initViews();

        // 获取Intent数据并初始化管理器
        currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
        currentUserId = getIntent().getStringExtra("USER_ID");
        dbManager = new DBManager(this);

        // 为所有可交互控件设置监听器
        setupListeners();

        // 初始化标签按钮和加载初始数据
        setupTagButtons();
        updateView(); // 使用新方法来加载初始视图
    }

    // 集中初始化所有视图控件
    private void initViews() {
        listView = findViewById(R.id.listView);
        calendarView = findViewById(R.id.calendarView);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        btnToggleView = findViewById(R.id.btn_toggle_view);
        calendarContainer = findViewById(R.id.calendar_container);
        tvListTitle = findViewById(R.id.tv_list_title);
    }

    // 集中设置所有控件的监听器
    private void setupListeners() {
        ImageView ivUserCenter = findViewById(R.id.iv_user_center);
        ivUserCenter.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, UserCenterActivity.class);
            intent.putExtra("USER_ID", currentUserId);
            startActivity(intent);
        });

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String monthStr = String.format(Locale.CHINA, "%02d", month + 1);
            String dayStr = String.format(Locale.CHINA, "%02d", dayOfMonth);
            currentDate = year + "-" + monthStr + "-" + dayStr;
            // 日期变化只在日历视图下加载数据
            loadActivitiesByDate(currentDate);
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            // 添加保护，防止在列表为空时点击导致崩溃
            if (activityBeanList != null && !activityBeanList.isEmpty()) {
                ActivityBean bean = activityBeanList.get(position);
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                intent.putExtra("ACTIVITY_ID", bean.getId());
                intent.putExtra("USER_ID", currentUserId);
                startActivity(intent);
            }
        });

        // 切换视图按钮的逻辑
        btnToggleView.setOnClickListener(v -> {
            isAllActivitiesView = !isAllActivitiesView; // 切换视图状态
            updateView(); // 根据新状态更新整个界面
        });
    }

    /**
     * 核心方法：根据 isAllActivitiesView 的状态来更新整个UI
     */
    private void updateView() {
        if (isAllActivitiesView) {
            // --- 切换到总览视图 ---
            calendarContainer.setVisibility(View.GONE); // 隐藏日历和相关控件
            tvListTitle.setText("所有活动");
            // 切换图标为“日历”，提示用户可以切回去 (假设您有 ic_calendar 图标)
            btnToggleView.setImageResource(R.drawable.ic_calendar);
            loadAllActivities(); // 加载所有活动数据
        } else {
            // --- 切换到日历视图 ---
            calendarContainer.setVisibility(View.VISIBLE); // 显示日历和相关控件
            tvListTitle.setText("当日活动");
            // 切换图标为“列表”，提示用户可以切换到总览 (假设您有 ic_view_list 图标)
            btnToggleView.setImageResource(R.drawable.ic_view_list);
            loadActivitiesByDate(currentDate); // 加载当天活动数据
        }
    }

    /**
     * 从数据库加载所有活动
     */
    private void loadAllActivities() {
        new Thread(() -> {
            activityBeanList = dbManager.getAllActivityList();
            updateListView(); // 使用统一的UI更新方法
        }).start();
    }

    /**
     * 从数据库加载指定日期的活动
     * @param date 日期字符串 "yyyy-MM-dd"
     */
    private void loadActivitiesByDate(String date) {
        new Thread(() -> {
            activityBeanList = dbManager.getActivityByDateAndTag(date, selectedTag);
            // 在UI线程更新日期显示
            runOnUiThread(() -> tvCurrentDate.setText("当前日期：" + date));
            updateListView(); // 使用统一的UI更新方法
        }).start();
    }

    /**
     * 统一更新 ListView 的UI，避免代码重复
     */
    private void updateListView() {
        // 确保UI更新在主线程执行
        new Handler(Looper.getMainLooper()).post(() -> {
            if (activityBeanList == null || activityBeanList.isEmpty()) {
                String emptyTip = isAllActivitiesView ? "暂无任何活动" : "当前日期暂无活动";
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        MainActivity.this, android.R.layout.simple_list_item_1, new String[]{emptyTip});
                listView.setAdapter(adapter);
            } else {
                String[] activityDisplayItems = new String[activityBeanList.size()];
                for (int i = 0; i < activityBeanList.size(); i++) {
                    ActivityBean bean = activityBeanList.get(i);
                    // 在总览视图下，显示完整日期；日历视图下，只显示时间
                    String timeInfo = isAllActivitiesView ? bean.getTime() : bean.getTime().split(" ")[1];
                    activityDisplayItems[i] = bean.getTitle() + " | " + timeInfo;
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        MainActivity.this, android.R.layout.simple_list_item_1, activityDisplayItems);
                listView.setAdapter(adapter);
            }
        });
    }

    // 初始化标签按钮及其点击事件
    private void setupTagButtons() {
        Button btnDeyu = findViewById(R.id.btn_tag_deyu);
        Button btnMeiyu = findViewById(R.id.btn_tag_meiyu);
        Button btnLaoyu = findViewById(R.id.btn_tag_laoyu);
        Button btnSafety = findViewById(R.id.btn_tag_safety);
        Button btnOther = findViewById(R.id.btn_tag_other);
        Button btnAll = findViewById(R.id.btn_tag_all);

        tagButtons.add(btnDeyu);
        tagButtons.add(btnMeiyu);
        tagButtons.add(btnLaoyu);
        tagButtons.add(btnSafety);
        tagButtons.add(btnOther);
        tagButtons.add(btnAll);

        btnDeyu.setOnClickListener(v -> handleTagSelection("德育", (Button) v));
        btnMeiyu.setOnClickListener(v -> handleTagSelection("美育", (Button) v));
        btnLaoyu.setOnClickListener(v -> handleTagSelection("劳育", (Button) v));
        btnSafety.setOnClickListener(v -> handleTagSelection("安全", (Button) v));
        btnOther.setOnClickListener(v -> handleTagSelection("其他", (Button) v));
        btnAll.setOnClickListener(v -> handleTagSelection(null, (Button) v));

        // 默认选中“全选”
        btnAll.setSelected(true);
    }

    // 处理标签选择逻辑
    private void handleTagSelection(String tag, Button selectedButton) {
        for (Button button : tagButtons) {
            button.setSelected(false);
        }
        selectedButton.setSelected(true);
        selectedTag = tag;
        // 标签筛选只在日历视图下生效
        if (!isAllActivitiesView) {
            loadActivitiesByDate(currentDate);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次返回主页时，根据当前视图状态刷新数据，确保数据是最新（例如从详情页返回）
        updateView();
    }
}