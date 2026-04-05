package com.example.fm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow


class TimerService: Service(){
    private var sec: Int = 0
    private val ID = 52
    private var flag = false
    private val handler = Handler(Looper.getMainLooper())
    companion object {
        const val start = "startAction"
        const val end = "endAction"
    }

    private val started = object : Runnable{
        override fun run() {
            sec++
            TimerState.sec.value = sec
            val not = NotificationHelper.createMessage(this@TimerService,sec)
            getSystemService(NotificationManager::class.java).notify(ID,not)
            handler.postDelayed(this, 1000)
        }

    }

    override fun onBind(intent: Intent): IBinder {
        return Binder()
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            start -> startTimer()
            end  -> stopTimer()
        }
        return START_STICKY
    }

    fun startTimer(){
        if (flag) return
        flag = true
        sec = -0
        val notification = NotificationHelper.createMessage(this, sec)
        startForeground(ID, notification)
        handler.postDelayed(started, 1000L)
    }

    fun stopTimer(){
        flag = false
        handler.removeCallbacks(started)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        flag = false
        handler.removeCallbacks(started)
    }
}



object NotificationHelper{
    private const val CHANNEL_ID = "Test"
    private const val CHANNEL_NAME = "Name"

    fun createNotificationChannel(context: Context){
        val notificationManager: NotificationManager = context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(CHANNEL_ID,CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)
    }


    fun createMessage(context: Context, seconds: Int) =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.hllktt)
            .setContentTitle("Уведомление")
            .setContentText("Прошло $seconds секунд")
            .build()
}


object TimerState {
    val sec = MutableStateFlow(0)
}