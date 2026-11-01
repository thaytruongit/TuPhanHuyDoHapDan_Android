package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver tự động khởi chạy lại dịch vụ chạy nền khi thiết bị khởi động lại
 * hoặc khi gói ứng dụng được cập nhật.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            ScreenDecayBackgroundEngine.ensureInitialized(context)
            if (ScreenDecayBackgroundEngine.sessionState.value.isBackgroundMonitorActive) {
                ScreenDecayForegroundService.startOrUpdateService(context)
            }
        }
    }
}
