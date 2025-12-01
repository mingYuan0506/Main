package com.example.main;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class ActivityReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "activity_reminder_channel";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        // 获取活动信息
        String activityTitle = intent.getStringExtra("ACTIVITY_TITLE");
        String activityTime = intent.getStringExtra("ACTIVITY_TIME");

        // 创建通知渠道（Android O及以上）
        createNotificationChannel(context);

        // 构建通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // 需添加通知图标
                .setContentTitle("活动提醒")
                .setContentText(activityTitle + "即将开始！\n时间：" + activityTime)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent( // 点击通知跳转到活动详情页
                        PendingIntent.getActivity(
                                context,
                                0,
                                new Intent(context, DetailActivity.class)
                                        .putExtra("ACTIVITY_ID", intent.getIntExtra("ACTIVITY_ID", -1))
                                        .putExtra("USER_ID", intent.getStringExtra("USER_ID")),
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        )
                );

        // 显示通知
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID + intent.getIntExtra("ACTIVITY_ID", 0), builder.build());
    }

    // 创建通知渠道
    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "活动提醒";
            String description = "收藏活动的时间提醒";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
