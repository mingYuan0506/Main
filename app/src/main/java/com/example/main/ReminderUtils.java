package com.example.main;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ReminderUtils {
    // 活动时间格式（与数据库一致）
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    // 设置提醒（提前30分钟）
    public static void setReminder(Context context, String userId, ActivityBean activity) {
        try {
            // 解析活动开始时间
            Date activityDate = DATE_FORMAT.parse(activity.getTime());
            if (activityDate == null) return;

            // 计算提醒时间（提前30分钟）
            Calendar reminderTime = Calendar.getInstance();
            reminderTime.setTime(activityDate);
            reminderTime.add(Calendar.MINUTE, -30); // 提前30分钟提醒

            // 如果活动已过，不设置提醒
            if (reminderTime.before(Calendar.getInstance())) return;

            // 创建闹钟Intent
            Intent intent = new Intent(context, ActivityReminderReceiver.class);
            intent.putExtra("USER_ID", userId);
            intent.putExtra("ACTIVITY_ID", activity.getId());
            intent.putExtra("ACTIVITY_TITLE", activity.getTitle());
            intent.putExtra("ACTIVITY_TIME", activity.getTime());

            // 每个活动的PendingIntent用不同的requestCode（避免覆盖）
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    activity.getId(), // 用活动ID作为requestCode
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // 设置闹钟
            // 找到手机的闹钟管理器
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            AlarmManager.AlarmClockInfo clockInfo = new AlarmManager.AlarmClockInfo(
                    reminderTime.getTimeInMillis(),
                    PendingIntent.getActivity(context, 0, new Intent(), PendingIntent.FLAG_IMMUTABLE)
            );

            alarmManager.setAlarmClock(clockInfo, pendingIntent);

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    // 取消提醒
    public static void cancelReminder(Context context, int activityId) {
        Intent intent = new Intent(context, ActivityReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                activityId,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    // 批量恢复用户收藏活动的提醒（如开机后）
    public static void restoreReminders(Context context, String userId) {
        DBManager dbManager = new DBManager(context);
        for (ActivityBean activity : dbManager.getFavoriteActivities(userId)) {
            setReminder(context, userId, activity);
        }
    }
}
