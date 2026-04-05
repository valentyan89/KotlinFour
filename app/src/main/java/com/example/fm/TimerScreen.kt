package com.example.fm

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FiveTask(){
    val context = LocalContext.current
    val thisSec by TimerState.sec.collectAsState()
    TimerScreen(
        thisSec,
        onStartClick = {
            val intent = Intent(context, TimerService::class.java).apply {
                action = TimerService.start
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        },
        onStopClick = {
            val intent = Intent(context, TimerService::class.java).apply {
                action = TimerService.end
            }
            context.startService(intent)
            TimerState.sec.value = 0
        }
    )
}


@Composable
fun TimerScreen(
    secs: Int,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
){
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ){
        Text(
            text = secs.toString(),
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onStartClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDCAB80))
        ) {
            Text(
                text = "start",
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onStopClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDCAB80))
        ) {
            Text(
                text ="stop",
                fontSize = 18.sp
            )
        }
    }
}