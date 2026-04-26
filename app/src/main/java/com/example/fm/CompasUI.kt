package com.example.compass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.fm.CompasViewModel
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlin.math.min

@Composable
fun CompasScreen(viewModel: CompasViewModel) {
    val azimuth = viewModel.azimuth
    val animatedRotation by animateFloatAsState(
        targetValue = -azimuth,
        animationSpec = tween(durationMillis = 220),
        label = "compass_rotation"
    )

    if (viewModel.sensorAvailable) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1115))
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "нет датчика",
                color = Color.Red,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1115))
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("компас", color = Color(0xFFADE3FF), fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(CircleShape)
                .background(Color(0xFF546179)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize(0.9f)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val radius = min(size.width, size.height) / 2f

                rotate(animatedRotation, Offset(cx, cy)) {
                    drawLine(
                        color = Color(0xFFE5359F),
                        start = Offset(cx, cy),
                        end = Offset(cx, cy - radius * 0.8f),
                        strokeWidth = 16f
                    )
                    drawLine(
                        color = Color.DarkGray,
                        start = Offset(cx, cy),
                        end = Offset(cx, cy + radius * 0.8f),
                        strokeWidth = 16f
                    )
                }

                drawCircle(Color.LightGray, 12f, Offset(cx, cy))
            }

            Text(
                text = "N",
                color = Color(0xFFE5359F),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
            )
        }

        Spacer(Modifier.height(20.dp))
        Text("азимут ${azimuth.toInt()}`", color = Color(0xFFADE3FF), fontSize = 32.sp)
    }
}

@Composable
fun CompasLifecycle(vm: CompasViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(Unit) {
        val job = lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                vm.start()
                try {
                    awaitCancellation()
                }
                finally {
                    vm.stop()
                }
            }
        }

        onDispose { job.cancel() }
    }
}