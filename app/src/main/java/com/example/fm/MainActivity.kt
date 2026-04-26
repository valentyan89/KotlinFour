package com.example.fm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.compass.CompasLifecycle
import com.example.compass.CompasScreen
import com.example.fm.ui.theme.FMTheme

class MainActivity : ComponentActivity() {

    private val vm: CompasViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FMTheme {
                FiveTask()
//                WeatherScreen()
//                LocationAddressScreen()

//                CompasLifecycle(vm)
//                CompasScreen(vm)
            }
        }
    }
}




@Composable
fun CourseScreen(viewModel: CourceViewModel = viewModel()){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFDAEDEF)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val course by viewModel.rate.collectAsState()
        val trend by viewModel.trend.collectAsState()
        val _color = when (trend) {
            1 -> Color(0xF08DFFAE)
            -1 -> Color(0xF0FF8D9A)
            else -> Color(0xF0797979)
        }

        val _char = when(trend){
            1 -> '+'
            -1 -> '-'
            else -> '='
        }

        Text(
            text = "Course USDT to RUB",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.padding(top = 16.dp))


        Text(
            text = "${"%.2f".format(course)} RUB",
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(
                    color = Color(0x85B9C2C4),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(18.dp)
        )


        Spacer(Modifier.padding(top = 16.dp))
        Text(
            text = "$_char",
            fontSize = 30.sp,
            color = _color
        )

        Spacer(Modifier.padding(top = 16.dp))

        Button(
            onClick = {viewModel.buttonRefresher()},
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(Color(0xFF74D5FC))
        ) {
            Text(
                text = "Refresh now",
                fontSize = 24.sp,
                color = Color(0xFFFFFFFF)
            )
        }
    }
}