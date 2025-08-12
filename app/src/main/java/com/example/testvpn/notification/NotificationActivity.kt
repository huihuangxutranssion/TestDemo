package com.example.testvpn.notification

//noinspection SuspiciousImport
import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.core.app.NotificationManagerCompat

class NotificationActivity : ComponentActivity() {
    private val channelId = "test_channel"
    private val notificationId = 101
    private val replyKey = "key_text_reply"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()

        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = { sendBasicNotification() }) {
                    Text(text = "发送基本通知", fontSize = 18.sp)
                }
                Button(onClick = { sendNotificationWithAction() }) {
                    Text(text = "发送带操作按钮的通知", fontSize = 18.sp)
                }
                Button(onClick = { sendNotificationWithReply() }) {
                    Text(text = "发送带直接回复的通知", fontSize = 18.sp)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "测试通知渠道"
            val descriptionText = "这是一个用于测试的通知渠道"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendBasicNotification() {
        val intent = Intent(this, NotificationActivity::class.java).apply {
            this.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentTitle("基本通知")
            .setContentText("这是一个基本通知示例")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, builder.build())
        }
    }

    private fun sendNotificationWithAction() {
        val snoozeIntent = Intent(this, SnoozeReceiver::class.java).apply {
            this.setAction("ACTION_SNOOZE")
        }
        val snoozePendingIntent: PendingIntent =
            PendingIntent.getBroadcast(this, 0, snoozeIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentTitle("带操作按钮的通知")
            .setContentText("点击按钮执行操作")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(R.drawable.ic_menu_close_clear_cancel, "延迟", snoozePendingIntent)

        with(NotificationManagerCompat.from(this)) {
            notify(notificationId + 1, builder.build())
        }
    }

    private fun sendNotificationWithReply() {
        val replyLabel = "输入您的回复"
        val remoteInput = RemoteInput.Builder(replyKey).setLabel(replyLabel).build()

        val replyIntent = Intent(this, ReplyReceiver::class.java)
        val replyPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val action = NotificationCompat.Action.Builder(
            R.drawable.ic_menu_send,
            "回复",
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentTitle("带直接回复的通知")
            .setContentText("点击回复按钮直接输入")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(action)

        with(NotificationManagerCompat.from(this)) {
            notify(notificationId + 2, builder.build())
        }
    }
}

class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 在这里处理延迟操作
    }
}

class ReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val replyText = RemoteInput.getResultsFromIntent(intent)?.getCharSequence("key_text_reply")
        // 在这里处理用户的回复
    }
}
