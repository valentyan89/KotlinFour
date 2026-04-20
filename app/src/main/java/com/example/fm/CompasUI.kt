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

    if (!viewModel.sensorAvailable) {
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
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "компас",
            color = Color(0xFFADE3FF),
            fontSize = 34.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(20.dp))

        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val compassSize = screenWidth * 0.76f

        Box(
            modifier = Modifier
                .size(compassSize)
                .clip(CircleShape)
                .background(Color(0xFF546179)),
            contentAlignment = Alignment.Center
        ) {
            CompassCanvas(animatedRotation)

            Text(
                text = "N",
                color = Color(0xFFE53935),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "азимут ${azimuth.toInt()}`",
            color = Color(0xFFADE3FF),
            fontSize = 32.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CompassCanvas(rotation: Float) {
    Canvas(modifier = Modifier.fillMaxSize(0.9f)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(size.width, size.height) / 2f

        rotate(degrees = rotation, pivot = Offset(cx, cy)) {
            drawLine(
                color = Color(0xFFE5357B),
                start = Offset(cx, cy),
                end = Offset(cx, cy - radius * 0.8f),
                strokeWidth = 28f
            )
            drawLine(
                color = Color(0xFF5A5A5A),
                start = Offset(cx, cy),
                end = Offset(cx, cy + radius * 0.8f),
                strokeWidth = 28f
            )
        }

        drawCircle(
            color = Color(0xFFD4D4D4),
            radius = 28f,
            center = Offset(cx, cy)
        )
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