package com.sibyl.httpfiledominator.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.sibyl.httpfiledominator.R
import com.sibyl.httpfiledominator.activities.TempActivity
import com.sibyl.httpfiledominator.mainactivity.view.MainActivity

/**
 * @author Sasuke on 2020/6/15.
 */
class NotiDominator(val context: Context) {
    var notification: Notification? = null

    fun showNotifi() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notification != null){
            notificationManager.notify(0, notification)
            return
        }
        // 获取系统 通知管理 服务
        // 构建 Notification
        // 构建 Notification
        val builder = Notification.Builder(context)
        builder.setContentTitle(context.getResources().getString(R.string.app_name))
                .setSmallIcon(R.drawable.icon_folder)
                .setContentText(context.getResources().getString(R.string.server_is_on))
                .setVibrate(longArrayOf(0))
                .setSound(null)

        // 兼容  API 26，Android 8.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 第三个参数表示通知的重要程度，默认则只在通知栏闪烁一下
            val channel = NotificationChannel("HttpFileDominator", "HTTP文件服务器", NotificationManager.IMPORTANCE_DEFAULT)
            channel.enableLights(false)
            channel.enableVibration(false)
            channel.vibrationPattern = longArrayOf(0)
            channel.setSound(null, null)
            // 注册通道，注册后除非卸载再安装否则不改变
            notificationManager.createNotificationChannel(channel)
            builder.setChannelId("HttpFileDominator")
        }

        //点击跳转
        val intent = Intent(context, TempActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(pendingIntent)

        //按钮
        val intent2 = Intent(context, MainActivity::class.java)
        intent2.putExtra("isStopServer", true)
        val pendingIntent2 = PendingIntent.getActivity(context, 1, intent2, PendingIntent.FLAG_UPDATE_CURRENT)
        val actionBuilder = Notification.Action.Builder(null, context.getResources().getString(R.string.stop), pendingIntent2)
        builder.addAction(actionBuilder.build())

        //设置不可清除
        notification = builder.build()
        notification?.flags = Notification.FLAG_NO_CLEAR //不可清除

        // 发出通知
        notificationManager.notify(0, notification)
    }

    fun dismissAll(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
    }
}