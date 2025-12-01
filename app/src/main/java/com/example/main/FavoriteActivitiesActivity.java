package com.example.main;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class FavoriteActivitiesActivity extends AppCompatActivity {
    private String currentUserId;
    private DBManager dbManager;
    private List<ActivityBean> favoriteActivities;
    private FavoriteActivityAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_activities);

        // 新增：配置导航条（返回首页）
        Toolbar toolbar = findViewById(R.id.toolbar); // 布局中新增的导航条
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // 显示返回箭头
        getSupportActionBar().setTitle("已收藏活动"); // 导航条标题

        // 获取当前用户ID
        currentUserId = getIntent().getStringExtra("USER_ID");
        dbManager = new DBManager(this);

        // 初始化列表
        ListView lvFavoriteActivities = findViewById(R.id.lv_favorite_activities);
        adapter = new FavoriteActivityAdapter();
        lvFavoriteActivities.setAdapter(adapter);

        // 加载收藏数据
        loadFavoriteActivities();

        // 列表项点击事件（跳转到详情页）
        lvFavoriteActivities.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ActivityBean activity = favoriteActivities.get(position);
                Intent intent = new Intent(FavoriteActivitiesActivity.this, DetailActivity.class);
                intent.putExtra("ACTIVITY_ID", activity.getId());
                intent.putExtra("USER_ID", currentUserId);
                startActivity(intent);
            }
        });
    }

    // 加载收藏的活动
    private void loadFavoriteActivities() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                favoriteActivities = dbManager.getFavoriteActivities(currentUserId);

                // 更新UI
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();

                        if (favoriteActivities.isEmpty()) {
                            Toast.makeText(FavoriteActivitiesActivity.this, "暂无收藏的活动", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }

    // 自定义适配器
    private class FavoriteActivityAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return favoriteActivities == null ? 0 : favoriteActivities.size();
        }

        @Override
        public Object getItem(int position) {
            return favoriteActivities.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
                holder = new ViewHolder();
                holder.tvTitle = convertView.findViewById(android.R.id.text1);
                holder.tvInfo = convertView.findViewById(android.R.id.text2);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            // 设置数据
            ActivityBean activity = favoriteActivities.get(position);
            holder.tvTitle.setText(activity.getTitle());
            holder.tvInfo.setText(activity.getTime() + " | " + activity.getLocation());

            return convertView;
        }

        private class ViewHolder {
            TextView tvTitle;
            TextView tvInfo;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish(); // 关闭当前详情页，返回上一页（首页）
        return true;
    }
}