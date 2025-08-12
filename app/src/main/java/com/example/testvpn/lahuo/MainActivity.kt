package com.example.testvpn.lahuo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DailyLiveNotificationScreen()
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyLiveNotificationScreen() {
    val context = LocalContext.current.applicationContext // 确保在 @Composable 中调用
    var firstNotificationTime by remember { mutableStateOf("") }
    var firstNotificationDelay by remember { mutableStateOf("") }
    var secondNotificationTime by remember { mutableStateOf("") }
    var secondNotificationDelay by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val currentTime = Calendar.getInstance()

        // 第一个通知时间
        val hourOfDay = 14 // 示例：每日 9 点
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, 43)
            set(Calendar.SECOND, 0)
            if (before(currentTime)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val initialDelay = targetTime.timeInMillis - currentTime.timeInMillis
        firstNotificationTime = "$hourOfDay:00"
        firstNotificationDelay = "${initialDelay / 1000 / 60} 分钟"

        // 第二个通知时间
        val targetTime2 = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 43)
            set(Calendar.SECOND, 30)
            if (before(currentTime)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val initialDelay2 = targetTime2.timeInMillis - currentTime.timeInMillis
        secondNotificationTime = "22:00"
        secondNotificationDelay = "${initialDelay2 / 1000 / 60} 分钟"

        // 调度任务
        val workManager = WorkManager.getInstance(context)
        val firstWorkRequest = PeriodicWorkRequest.Builder(
            DailyLiveNotificationWorker::class.java,
            24, TimeUnit.HOURS
        ).setInitialDelay(initialDelay, TimeUnit.MILLISECONDS).build()

        val secondWorkRequest = PeriodicWorkRequest.Builder(
            DailyLiveSecondNotificationWorker::class.java,
            24, TimeUnit.HOURS
        ).setInitialDelay(initialDelay2, TimeUnit.MILLISECONDS).build()

        workManager.enqueueUniquePeriodicWork(
            "DailyLiveNotification",
            ExistingPeriodicWorkPolicy.REPLACE,
            firstWorkRequest
        )
        workManager.enqueueUniquePeriodicWork(
            "DailyLiveSecondNotification",
            ExistingPeriodicWorkPolicy.REPLACE,
            secondWorkRequest
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("每日拉活通知") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "第一个通知时间：$firstNotificationTime")
            Text(text = "距离通知还有：$firstNotificationDelay")
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "第二个通知时间：$secondNotificationTime")
            Text(text = "距离通知还有：$secondNotificationDelay")
        }
    }
}
