package com.example.testvpn.old

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import com.example.testvpn.R

class MainActivity : ComponentActivity() {
    private var isVpnRunning by mutableStateOf(false)
    private var localIp by mutableStateOf("获取中...")
    private var publicIp by mutableStateOf("获取中...")
    private var location by mutableStateOf("获取中...")
    private lateinit var mManager: NotificationManager
    private val mHighChannelId = "high_channel_id"
    private val mHighChannelName = "重要通知"
    private val mNormalChannelId = "normal_channel_id"
    private val mNormalChannelName = "普通通知"
    private val mHighNotificationId = 1
    private val mNormalNotificationId = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // IP信息展示区域
                IpInfoDisplay(
                    localIp = localIp,
                    publicIp = publicIp,
                    location = location
                )

                // 间隔
                Text(modifier = Modifier.padding(20.dp), text = "")

                // VPN控制按钮
                Button(
                    onClick = { toggleVpn() },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (isVpnRunning) "停止VPN" else "启动VPN",
                        fontSize = 18.sp
                    )
                }

                // 通知控制按钮
                Button(
                    onClick = { sendMessage() },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "发送通知",
                        fontSize = 18.sp
                    )
                }
            }
        }

        // 初始加载IP信息
        loadIpInfo()
    }

    // 加载IP信息
    private fun loadIpInfo() {
        // 获取本地IP
        localIp = IpLocationUtils.getLocalIpAddress() ?: "无法获取本地IP"

        // 检查网络并获取公网IP及属地
        if (IpLocationUtils.isNetworkAvailable(this)) {
            IpLocationUtils.getPublicIpLocation(object : IpLocationUtils.OnIpLocationListener {
                override fun onSuccess(ipLocation: IpLocation) {
                    runOnUiThread {
                        publicIp = ipLocation.ip
                        location = "${ipLocation.country_name} ${ipLocation.region} ${ipLocation.city}"
                    }
                }

                override fun onFailure(error: String) {
                    runOnUiThread {
                        publicIp = "获取失败"
                        location = error
                        Log.e("IP获取", error)
                    }
                }
            })
        } else {
            publicIp = "无网络连接"
            location = "请检查网络"
        }
    }

    private fun sendMessage() {
        // 初始化 NotificationManager
        mManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // 发起通知
        createNotificationForHigh()
        createNotificationForNormal()
    }

    private fun createNotificationForHigh() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // 创建通知渠道（适配 Android 8.0 及以上）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                mHighChannelId,
                mHighChannelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.setShowBadge(true)
            mManager.createNotificationChannel(channel)
        }

        // 构建通知
        val mBuilder = NotificationCompat.Builder(this, mHighChannelId)
            .setContentTitle("重要通知")
            .setContentText("这是一个重要通知")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setAutoCancel(true)
            .setNumber(999)
            .addAction(R.mipmap.ic_launcher, "去看看", pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)

        // 发起通知
        mManager.notify(mHighNotificationId, mBuilder.build())
    }

    private fun createNotificationForNormal() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // 创建通知渠道（适配 Android 8.0 及以上）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                mNormalChannelId,
                mNormalChannelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "普通通知描述"
                setShowBadge(false)
            }
            mManager.createNotificationChannel(channel)
        }

        // 构建通知
        val mBuilder = NotificationCompat.Builder(this, mNormalChannelId)
            .setContentTitle("普通通知")
            .setContentText("这是一个普通通知")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // 发起通知
        mManager.notify(mNormalNotificationId, mBuilder.build())
    }


    private fun toggleVpn() {
        if (isVpnRunning) {
            stopService(Intent(this, MyVpn::class.java))
            isVpnRunning = false
        } else {
            // 请求VPN权限
            val intent = VpnService.prepare(this)
            if (intent != null) {
                startActivityForResult(intent, 100)
            } else {
                startVpnService()
            }
        }
        // VPN状态变化后重新获取IP信息
        loadIpInfo()
    }

    private fun startVpnService() {
        startService(Intent(this, MyVpn::class.java))
        isVpnRunning = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            startVpnService()
            // 授权后重新获取IP信息
            loadIpInfo()
        }
    }
}

// IP信息展示组件
@Composable
fun IpInfoDisplay(
    localIp: String,
    publicIp: String,
    location: String
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.padding(16.dp)
    ) {
        Text(text = "本地IP: $localIp", fontSize = 16.sp)
        Text(text = "公网IP: $publicIp", fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
        Text(text = "IP属地: $location", fontSize = 16.sp)
    }
}
