package org.ar.screenshare

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * 屏幕媒体读取服务
 */
class MediaReaderService : Service(){

    companion object {
        private val TAG = MediaReaderService::class.java.simpleName
        const val START = 1
        const val STOP = 2
        private const val UNLOCK_NOTIFICATION_CHANNEL_ID = "unlock_notification"
    }

    private val NOTIFICATION_ID_ICON = 1

    private lateinit var videoCapturer: VideoCapturer

    override fun onCreate() {
        super.onCreate()
        initNotificationChannel()
    }

    private fun startServer() {
        buildNotification(R.mipmap.ic_launcher, getString(R.string.app_name), "正在运行")
         try {
            videoCapturer=VideoCapturer(App.app.mediaProjection,
                Config.width, Config.height,
                Config.bitRate, Config.frameRate
            )
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
            return
        }
    }

    private fun stopServer() {
        videoCapturer.exit()
        deleteNotification()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when(intent.getIntExtra("CMD",0)){
            START->{
                startServer()
            }
            STOP->{
                stopServer()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        throw Exception("unable to bind!")
    }



    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        super.onDestroy()
    }

    private fun initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //创建通知渠道
            val name: CharSequence = "运行通知"
            val description = "服务运行中"
            val channelId = UNLOCK_NOTIFICATION_CHANNEL_ID //渠道id
            val importance = NotificationManager.IMPORTANCE_DEFAULT //重要性级别
            val mChannel = NotificationChannel(channelId, name, importance)
            mChannel.description = description //渠道描述
            mChannel.enableLights(false) //是否显示通知指示灯
            mChannel.enableVibration(false) //是否振动
            val notificationManager = getSystemService(
                    Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel) //创建通知渠道
        }
    }

    private fun buildNotification(resId: Int, tiile: String, contenttext: String) {
        val builder = NotificationCompat.Builder(this, UNLOCK_NOTIFICATION_CHANNEL_ID)

        // 必需的通知内容
        builder.setContentTitle(tiile)
                .setContentText(contenttext)
                .setSmallIcon(resId)
        val notifyIntent = Intent(this, MainActivity::class.java)
        val notifyPendingIntent = PendingIntent.getService(this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(notifyPendingIntent)
        val notification = builder.build()
        //常驻状态栏的图标
        //notification.icon = resId;
        // 将此通知放到通知栏的"Ongoing"即"正在运行"组中
        notification.flags = notification.flags or Notification.FLAG_ONGOING_EVENT
        // 表明在点击了通知栏中的"清除通知"后，此通知不清除，经常与FLAG_ONGOING_EVENT一起使用
        notification.flags = notification.flags or Notification.FLAG_NO_CLEAR
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_ICON, notification)
        startForeground(NOTIFICATION_ID_ICON, notification)
    }

    private fun deleteNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID_ICON)
    }



}