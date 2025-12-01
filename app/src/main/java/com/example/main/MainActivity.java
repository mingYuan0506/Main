package com.example.main;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private DBManager dbManager;
    // 几个关键的控件
    private CalendarView calendarView; // 日历
    private TextView tvCurrentDate; // 显示当前选中日期的文本控件
    private ListView listView; // 活动展示列表

    private String currentDate; // 记录当前选中的日期（格式：yyyy-MM-dd）
    private String currentUserId; // 新增：存储当前登录用户ID
    private List<ActivityBean> activityBeanList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 导航条 个人中心跳转
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // 隐藏默认标题

        ImageView ivUserCenter = findViewById(R.id.iv_user_center);
        ivUserCenter.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, UserCenterActivity.class)
                    .putExtra("USER_ID", currentUserId));
        });


        // 1. 初始化控件（新增显示当前日期的TextView）
        listView = findViewById(R.id.listView);
        calendarView = findViewById(R.id.calendarView);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);

        // 2. 初始化当前日期为今天（格式：yyyy-MM-dd）
        currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
        tvCurrentDate.setText("当前日期：" + currentDate); // 显示今天日期

        // 接收登录页传递的用户ID
        currentUserId = getIntent().getStringExtra("USER_ID");
//        if (currentUserId == null) {
//            currentUserId = "guest"; // 兜底：未登录时设为游客
//        }

        // 3. 初始化数据库，添加测试数据（首次打开才加）
        dbManager = new DBManager(this);
        new Thread(() -> {
            loadActivitiesByDate(currentDate); // 默认加载今天的活动
        }).start();  // 为啥这里要新开一个线程去加载活动啊

        // 4. 日历切换事件：更新当前日期并刷新列表
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // 月份是0-11，所以要+1；日不足2位补0（比如9→09）
            String monthStr = String.format(Locale.CHINA, "%02d", month + 1);
            String dayStr = String.format(Locale.CHINA, "%02d", dayOfMonth);
            currentDate = year + "-" + monthStr + "-" + dayStr;
            tvCurrentDate.setText("当前日期：" + currentDate); // 更新显示的日期
            loadActivitiesByDate(currentDate); // 加载选中日期的活动
        });


        // 列表点击事件：传递用户ID到详情页
        listView.setOnItemClickListener((parent, view, position, id) -> {
            ActivityBean bean = activityBeanList.get(position);
            Intent intent = new Intent(MainActivity.this, DetailActivity.class);
            intent.putExtra("ACTIVITY_ID", bean.getId());
            intent.putExtra("USER_ID", currentUserId); // 新增：传递用户ID
            startActivity(intent);
        });
    }

    // 核心修改：按日期加载活动（替代原来的loadActivities）
    private void loadActivitiesByDate(String date) {
        new Thread(() -> {
            // 按日期查询活动
            activityBeanList = dbManager.getActivityByDate(date);

            // 主线程更新列表
            new Handler(Looper.getMainLooper()).post(() -> {
                if (activityBeanList.isEmpty()) {
                    // 没有活动时显示提示
                    String[] emptyTip = new String[]{"当前日期暂无活动"};
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            MainActivity.this,
                            android.R.layout.simple_list_item_1,
                            emptyTip
                    );
                    listView.setAdapter(adapter);
                } else {
                    // 有活动时显示活动名称+时间
                    String[] activityNames = new String[activityBeanList.size()];
                    for (int i = 0; i < activityBeanList.size(); i++) {
                        ActivityBean bean = activityBeanList.get(i);
                        // 显示格式：活动名 | 具体时间（如：北航AI讲座 | 14:00）
                        activityNames[i] = bean.getTitle() + " | " + bean.getTime().split(" ")[1];
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            MainActivity.this,
                            android.R.layout.simple_list_item_1,
                            activityNames
                    );
                    listView.setAdapter(adapter);
                }
            });
        }).start();
    }

    // 返回页面时，刷新当前选中日期的活动列表
    @Override
    protected void onResume() {
        super.onResume();
        loadActivitiesByDate(currentDate);
    }
}