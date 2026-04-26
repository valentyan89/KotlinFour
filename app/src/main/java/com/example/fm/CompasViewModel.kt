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

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    var azimuth by mutableFloatStateOf(0f)
        private set
    var sensorAvailable by mutableStateOf(accelerometerSensor == null || magneticSensor == null)
        private set

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)

    private var hasGravity = false
    private var hasGeomagnetic = false

    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    fun start() {
        if (accelerometerSensor == null || magneticSensor == null) return

        accelerometerSensor.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        magneticSensor.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        hasGravity=false
        hasGeomagnetic=false
    }


    override fun onAccuracyChanged(p0: Sensor?, p1: Int) = Unit

    private fun smoothAngle(current: Float, target: Float, factor: Float): Float {
        var delta = target - current
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        val result = current + factor * delta
        return when {
            result < 0f -> result + 360f
            result >= 360f -> result - 360f
            else -> result
        }
    }


    private fun lowPass(newValue: Float, oldValue: Float, alpha: Float = 0.12f): Float {
        return oldValue + alpha * (newValue - oldValue)
    }


    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                gravity[0] = lowPass(event.values[0], gravity[0])
                gravity[1] = lowPass(event.values[1], gravity[1])
                gravity[2] = lowPass(event.values[2], gravity[2])
                hasGravity = true
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                geomagnetic[0] = lowPass(event.values[0], geomagnetic[0])
                geomagnetic[1] = lowPass(event.values[1], geomagnetic[1])
                geomagnetic[2] = lowPass(event.values[2], geomagnetic[2])
                hasGeomagnetic = true
            }
        }

        if (hasGravity && hasGeomagnetic) {
            val isGood = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
            if (isGood) {
                SensorManager.getOrientation(rotationMatrix, orientation)

                var deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                if (deg < 0) deg += 360f

                azimuth = smoothAngle(azimuth, deg, factor = 0.18f)
            }
        }
    }
}


