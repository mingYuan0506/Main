package com.example.main;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// 需要在Manifest中注册，并添加RECEIVE_BOOT_COMPLETED权限
public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // 这里需要获取当前登录用户ID（需持久化存储，如SharedPreferences）
            String currentUserId = getCurrentUserId(context); // 需自己实现用户ID持久化
            if (currentUserId != null) {
                ReminderUtils.restoreReminders(context, currentUserId);
            }
        }
    }

    // 从SharedPreferences获取当前登录用户ID（示例）
    private String getCurrentUserId(Context context) {
        return context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getString("current_user_id", null);
    }
}
