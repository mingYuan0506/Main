package com.example.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
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

    // 筛选相关
    private Spinner spinnerTags;
    private String selectedTag = null; // null 代表“全部”

    // 视图状态变量
    private boolean isAllActivitiesView = false; // false = 日历视图, true = 总览视图

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        initViews();

        currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
        currentUserId = getIntent().getStringExtra("USER_ID");
        dbManager = new DBManager(this);

        setupTagSpinner(); // 设置筛选器
        setupListeners();

        updateView(); // 加载初始视图
    }

    private void initViews() {
        listView = findViewById(R.id.listView);
        calendarView = findViewById(R.id.calendarView);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        btnToggleView = findViewById(R.id.btn_toggle_view);
        calendarContainer = findViewById(R.id.calendar_container);
        tvListTitle = findViewById(R.id.tv_list_title);
        spinnerTags = findViewById(R.id.spinner_tags);
    }

    /**
     * 初始化标签筛选的 Spinner
     */
    private void setupTagSpinner() {
        String[] tags = new String[]{"全部", "德育", "美育", "劳育", "安全", "其他"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tags);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTags.setAdapter(adapter);

        spinnerTags.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                if (selectedItem.equals("全部")) {
                    selectedTag = null;
                } else {
                    selectedTag = selectedItem;
                }
                // 每当用户做出新的筛选选择时，重新加载当前视图的数据
                updateView();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

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
            loadActivitiesByDate();
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (activityBeanList != null && !activityBeanList.isEmpty()) {
                ActivityBean bean = activityBeanList.get(position);
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                intent.putExtra("ACTIVITY_ID", bean.getId());
                intent.putExtra("USER_ID", currentUserId);
                startActivity(intent);
            }
        });

        btnToggleView.setOnClickListener(v -> {
            isAllActivitiesView = !isAllActivitiesView;
            updateView();
        });
    }

    private void updateView() {
        if (isAllActivitiesView) {
            calendarContainer.setVisibility(View.GONE);
            tvListTitle.setText("所有活动");
            btnToggleView.setImageResource(R.drawable.ic_calendar);
            loadAllActivities();
        } else {
            calendarContainer.setVisibility(View.VISIBLE);
            tvListTitle.setText("当日活动");
            btnToggleView.setImageResource(R.drawable.ic_view_list);
            loadActivitiesByDate();
        }
    }

    private void loadAllActivities() {
        new Thread(() -> {
            activityBeanList = dbManager.getAllActivitiesByTag(selectedTag);
            updateListView();
        }).start();
    }

    private void loadActivitiesByDate() {
        new Thread(() -> {
            activityBeanList = dbManager.getActivityByDateAndTag(currentDate, selectedTag);
            runOnUiThread(() -> tvCurrentDate.setText("当前日期：" + currentDate));
            updateListView();
        }).start();
    }

    private void updateListView() {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (activityBeanList == null || activityBeanList.isEmpty()) {
                String emptyTip = "暂无符合条件的活动";
                ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, new String[]{emptyTip});
                listView.setAdapter(adapter);
            } else {
                String[] activityDisplayItems = new String[activityBeanList.size()];
                for (int i = 0; i < activityBeanList.size(); i++) {
                    ActivityBean bean = activityBeanList.get(i);
                    String timeInfo = isAllActivitiesView ? bean.getTime() : bean.getTime().split(" ")[1];
                    activityDisplayItems[i] = bean.getTitle() + " | " + timeInfo;
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, activityDisplayItems);
                listView.setAdapter(adapter);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateView();
    }
}