package com.rc.common.car_app_service.speed_control

import androidx.car.app.CarContext
import androidx.car.app.hardware.common.OnCarDataAvailableListener
import androidx.car.app.hardware.info.CarInfo
import androidx.car.app.hardware.info.Speed
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

// this class is responsible for
// converting callbacks into flow
// which we can collect off the main thread

class SpeedListener(
    private val carContext: CarContext,
    private val carInfo: CarInfo
) {
    fun generateSpeedDataFlow(): Flow<Int> = callbackFlow {
        val listener = OnCarDataAvailableListener<Speed> { data ->
            val currentSpeed = data.rawSpeedMetersPerSecond.value.convertMsToKmh()
            launch {
                send(currentSpeed)
            }
        }
        carInfo.addSpeedListener(carContext.mainExecutor, listener)
        awaitClose {
            carInfo.removeSpeedListener(listener)
        }
    }
}