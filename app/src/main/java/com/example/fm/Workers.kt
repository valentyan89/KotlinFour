package com.example.fm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ArrayCreatingInputMerger
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.fm.NotificationHelper.createNotificationChannel
import kotlinx.coroutines.delay
import kotlin.random.Random
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.work.WorkInfo
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy

class WeatherWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val city = inputData.getString(KEY_CITY) ?: return Result.failure()

        setForegroundAsync(
            createProgressNotification(applicationContext, "Загружаем погоду: $city...")
        )
        Log.d("www", "Start: $city")

        delay(2000)

        val temp = Random.nextInt(-20, 25)
        val weatherConditions = listOf("ясно", "облачно", "дождливо", "снежно").random()

        return Result.success(
            workDataOf(
                KEY_CITY to city,
                KEY_TEMP to temp,
                KEY_CONDITION to weatherConditions
            )
        )
    }

    companion object {
        const val KEY_CITY = "city"
        const val KEY_TEMP = "temperature"
        const val KEY_CONDITION = "weather conditions"
    }
}

data class CityWeather(
    val city: String,
    val temp: Int?,
    val cond: String?
)

class ComninationWeatherWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        setForegroundAsync(
            createProgressNotification(applicationContext, "Формируем итоговый отчёт...")
        )

        delay(1500)

        val cities = inputData.getStringArray(WeatherWorker.KEY_CITY) ?: emptyArray()
        val temperatures = inputData.getIntArray(WeatherWorker.KEY_TEMP) ?: IntArray(0)
        val weatherConditions = inputData.getStringArray(WeatherWorker.KEY_CONDITION) ?: emptyArray()

        Log.d("RRR", "cities size = ${cities.size}")

        if (cities.isEmpty()) return Result.failure()

        val reportString = "OK"
        return Result.success(workDataOf("reportCompose" to reportString))
    }
}

fun createNotificationChannel1(context: Context, channelId: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "прогресс погоды",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }
}

fun createProgressNotification(
    context: Context,
    message: String
) : ForegroundInfo {
    val channelId = "progress_bar"
    createNotificationChannel1(context, channelId)

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_menu_myplaces)
        .setContentTitle("Сбор прогноза погоды")
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .setProgress(0, 0, true)
        .build()

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ForegroundInfo(3435, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    } else {
        ForegroundInfo(3435, notification)
    }
}


@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
fun showFinalNotification(context: Context, report: String) {
    val channelId = "weather_progress"
    createNotificationChannel1(context, channelId)


    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra("weather_report", report)
    }
    val pendingIntent = PendingIntent.getActivity(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_menu_myplaces)
        .setContentTitle("прогноз реди")
        .setContentText("тапни")
        .setStyle(NotificationCompat.BigTextStyle().bigText(report))
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(context).notify(1002, notification)
}

fun startChain(context: Context){
    val workManager = WorkManager.getInstance(context)
    val towns = listOf("Moscow", "London", "Chernogorie", "Netherland")
    val requests = towns.map { it ->
        OneTimeWorkRequestBuilder<WeatherWorker>()
            .setInputData(workDataOf(WeatherWorker.KEY_CITY to it))
            .addTag("weather")
            .addTag("city:$it")
            .build()
    }

    val combineRequest = OneTimeWorkRequestBuilder<ComninationWeatherWorker>()
        .setInputMerger(ArrayCreatingInputMerger::class.java)
        .addTag("combine")
        .build()

    workManager.beginUniqueWork(
        "weather_chain",
        ExistingWorkPolicy.REPLACE,
        requests
    )
        .then(combineRequest)
        .enqueue()
}

@Composable
fun WeatherScreen() {
    val context = LocalContext.current
    val workManager = remember { WorkManager.getInstance(context) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                Log.d("p", "Notifications Granted")
            }
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isPermissionGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!isPermissionGranted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    var isWorking by remember { mutableStateOf(false) }
    var overallStatus by remember { mutableStateOf("ready to start") }
    var finalReport by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val towns = remember { listOf("Moscow", "London", "Chernogorie", "Netherland") }
    val cityStates = remember { mutableStateMapOf<String, CityStatus>() }

    LaunchedEffect(Unit) {
        towns.forEach {
            cityStates[it] = CityStatus("waiting", null, null)
        }
    }

    val workInfos by workManager.getWorkInfosByTagLiveData("weather")
        .observeAsState(initial = emptyList())


    LaunchedEffect(workInfos) {
        if (workInfos.isEmpty()) {
            isWorking = false
            return@LaunchedEffect
        }

        val allSucceeded = workInfos.all { it.state == WorkInfo.State.SUCCEEDED }
        val anyFailed = workInfos.any { it.state == WorkInfo.State.FAILED }
        val anyCancelled = workInfos.any { it.state == WorkInfo.State.CANCELLED }

        val runningCount = workInfos.count { it.state == WorkInfo.State.RUNNING }

        workInfos.forEach { info ->
            val city = info.tags.find { it.startsWith("city:") }?.removePrefix("city:") ?: return@forEach

            when (info.state) {
                WorkInfo.State.RUNNING -> {
                    cityStates[city] = CityStatus("loading", null, null)
                }
                WorkInfo.State.SUCCEEDED -> {
                    val temp = info.outputData.getInt(WeatherWorker.KEY_TEMP, 0)
                    val cond = info.outputData.getString(WeatherWorker.KEY_CONDITION) ?: "clear"
                    cityStates[city] = CityStatus("ready", temp, cond)
                }
                WorkInfo.State.FAILED -> {
                    cityStates[city] = CityStatus("err", null, null)
                }
                WorkInfo.State.CANCELLED -> {
                    cityStates[city] = CityStatus("err", null, null)
                }
                else -> {}
            }
        }

        overallStatus = when {
            anyCancelled -> "some canceled by user"
            anyFailed -> "err with loading"
            allSucceeded -> "all good"
            runningCount > 0 -> "loading.. ($runningCount processed)"
            else -> "waiting.."
        }

        isWorking = !allSucceeded && !anyFailed && !anyCancelled

        if (allSucceeded) {
            val temps = cityStates.values.mapNotNull { it.temp }.ifEmpty { listOf(0) }
            val avg = temps.average().toInt()

            finalReport = buildString {
                append("ended pred\n")
                cityStates.forEach { (city, status) ->
                    if (status.temp != null) {
                        append("$city: ${status.temp}°C, ${status.cond}\n")
                    }
                }
                append("\naverage temp: $avg°C")
            }

            showFinalNotification(context, finalReport!!)
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Weather prediction",
            style = MaterialTheme.typography.headlineMedium,
        )

        Text(
            text = overallStatus,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = when {
                isWorking -> MaterialTheme.colorScheme.primary
                errorMessage != null -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
        Spacer(modifier = Modifier.height(24.dp))


        towns.forEach {
            CityWeatherCard(
                city = it,
                state = cityStates[it] ?: CityStatus("—", null, null)
            )
        }


        Spacer(modifier = Modifier.height(32.dp))

        finalReport?.let { report ->
            FinalReportCard(report = report)
        }

        Spacer(modifier = Modifier.height(48.dp))

        WeatherActionButtons(
            isWorking = isWorking,
            onStartClick = {
                workManager.pruneWork()
                startChain(context)
                isWorking = true
                overallStatus = "started"
                finalReport = null
                errorMessage = null
                towns.forEach {
                    cityStates[it] = CityStatus("waiting", null, null)
                }
            },
            onCancelClick = {
                workManager.cancelAllWorkByTag("weather")
            }
        )
    }
}

data class CityStatus(
    val status: String,
    val temp: Int?,
    val cond: String?
)

@Composable
fun CityWeatherCard(
    city: String,
    state: CityStatus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = city,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = state.status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (state.status) {
                        "ready" -> MaterialTheme.colorScheme.primary
                        "err" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            when {
                state.status == "loading" -> {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
                state.status == "ready" && state.temp != null -> {
                    Text(
                        text = "${state.temp}°C",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                state.status == "err" -> {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "err",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun FinalReportCard(
    report: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = report,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}


@Composable
fun WeatherActionButtons(
    isWorking: Boolean,
    onStartClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onStartClick,
            enabled = !isWorking,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(if (isWorking) "in process" else "get pred")
        }

        if (isWorking) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("cancel")
            }
        }
    }
}