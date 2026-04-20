package com.example.fm

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

class CompasViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val sensorManager =
        application.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationVectorSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    var azimuth by mutableFloatStateOf(0f)
        private set

    var sensorAvailable by mutableStateOf(rotationVectorSensor != null)
        private set

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    fun start() {
        if (!sensorAvailable) return
        rotationVectorSensor?.let { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }


    override fun onAccuracyChanged(p0: Sensor?, p1: Int) = Unit

    override fun onSensorChanged(p0: SensorEvent?) {
        p0 ?: return
        if (p0.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, p0.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            var degrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            if (degrees < 0) degrees += 360f

            azimuth = degrees
        }
    }
}


