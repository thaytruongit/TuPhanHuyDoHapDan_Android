package vn.khkt.dopaminedecay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = context.getSharedPreferences("dopamine_decay_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("service_running", true)) {
            val srvIntent = Intent(context, DopamineMonitorService::class.java).apply {
                action = DopamineMonitorService.ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(srvIntent)
                } else {
                    context.startService(srvIntent)
                }
            } catch (_: Exception) {}
        }
    }
}
