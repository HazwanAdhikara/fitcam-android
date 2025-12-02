package com.example.fitcam.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SensorSystem(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // This function returns a "stream" of data (Flow) that updates whenever the sensor moves
    fun getAccelerometerData(): Flow<Triple<Float, Float, Float>> {
        return callbackFlow {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    event?.let {
                        // Send X, Y, Z values to the stream
                        trySend(Triple(it.values[0], it.values[1], it.values[2]))
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    // Not needed for now
                }
            }

            // Register the listener
            accelerometer?.let {
                sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
            }

            // Unregister listener when the flow is closed (saving battery)
            awaitClose {
                sensorManager.unregisterListener(listener)
            }
        }
    }
}