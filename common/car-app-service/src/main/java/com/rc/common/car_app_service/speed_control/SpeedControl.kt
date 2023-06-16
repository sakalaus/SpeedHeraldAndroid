package com.rc.common.car_app_service.speed_control

import android.app.Notification
import android.app.NotificationManager
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.notification.CarAppExtender
import androidx.car.app.notification.CarNotificationManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import com.rc.common.car_app_service.R
import com.rc.common.car_app_service.speed_control.SpeedControlState.ACTIVATED
import com.rc.common.car_app_service.speed_control.SpeedControlState.ACTIVATING
import com.rc.common.car_app_service.speed_control.SpeedControlState.DEACTIVATED
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.Calendar

// This Singleton is responsible for requesting permissions,
// collecting values from the flow provided by the SpeedControl,
// and building message channel for speed notifications

object SpeedControl {

    private const val SPEED_LIMIT = 140
    private const val SPEED_PERMISSION = "android.car.permission.CAR_SPEED"

    private const val NOTIFICATION_INTERVAL_MS = 5000L
    private const val NOTIFICATION_CHANNEL_NAME = "SPEED_NOTIFICATION_CHANNEL_NAME"
    private const val NOTIFICATION_CHANNEL_ID = "SPEED_NOTIFICATION_CHANNEL_ID"
    private var lastNotificationTimeStamp = 0L

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _speedControlFlow: MutableStateFlow<SpeedControlState> = MutableStateFlow(ACTIVATING)
    val speedControlFlow = _speedControlFlow.asStateFlow()

    // Permission request is asynchronous
    // Once the value is returned, it's sent into the flow
    // If it is ACTIVATED then the notification channel will start functioning
    // This flow also collected in the Ui to reflect the current state

    fun activateSpeedControl(carContext: CarContext) {
        with (carContext) {
            requestSpeedPermissions(this)
            speedControlFlow.onEach { speedControlState ->
                if (speedControlState is ACTIVATED) {
                     activateNotifications(this)
                }
            }.launchIn(
                scope
            )
        }
    }

    private fun requestSpeedPermissions(carContext: CarContext) {
        carContext.requestPermissions(listOf(SPEED_PERMISSION)) { grantedPermissions, _ ->
            if (grantedPermissions.contains(SPEED_PERMISSION)) {
                _speedControlFlow.value = ACTIVATED(speedLimit = SPEED_LIMIT)
            } else {
                _speedControlFlow.value = DEACTIVATED
            }
        }
    }

    private fun activateNotifications(carContext: CarContext) {

        val notificationBuilder = NotificationCompat.Builder(carContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.speed)
            .setContentTitle(
                carContext.getString(
                    R.string.speed_limit_of_km_h_exceeded,
                    SPEED_LIMIT.toString()
                )
            )
            .setOngoing(true)
            .setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .extend(
                CarAppExtender.Builder()
                    .setImportance(NotificationManager.IMPORTANCE_MAX)
                    .setChannelId(NOTIFICATION_CHANNEL_ID)
                    .build()
            )

        val notificationChannel = NotificationChannelCompat.Builder(
            NOTIFICATION_CHANNEL_ID,
            NotificationManager.IMPORTANCE_HIGH
        ).setName(NOTIFICATION_CHANNEL_NAME)
            .build()

        val notificationManager = CarNotificationManager.from(carContext).also { it ->
            it.createNotificationChannel(notificationChannel)
        }

        // Collecting speed data mainsafe, on the IO thread pool
        // notification interval is defined by NOTIFICATION_INTERVAL_MS

        // Documentation states that CATEGORY_MESSAGE should both trigger Hands on notification and
        // post the notification into the Notification centre
        // Somehow it is only shown in the Notification centre
        // That's why a duplicate notification is sent with CATEGORY_CALL to make it appear as HUN too

        SpeedListener(
                carContext = carContext,
                carInfo = carContext.getCarService(CarHardwareManager::class.java).carInfo
            ).generateSpeedDataFlow()
                .onEach { currentSpeed ->
                    if (currentSpeed > SPEED_LIMIT) {
                        Calendar.getInstance().timeInMillis.also {
                            if (it > lastNotificationTimeStamp + NOTIFICATION_INTERVAL_MS) {
                                notificationManager.notify(
                                    1,
                                    notificationBuilder.setContentTitle(
                                        carContext.getString(
                                            R.string.speed_limit_of_km_h_exceeded_current_speed_km_h,
                                            SPEED_LIMIT.toString(),
                                            currentSpeed.toString()
                                        )
                                    )
                                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                                )
                                lastNotificationTimeStamp = it
                                notificationManager.notify(
                                    2,
                                    notificationBuilder.setContentTitle(
                                        carContext.getString(
                                            R.string.speed_limit_of_km_h_exceeded_current_speed_km_h,
                                            SPEED_LIMIT.toString(),
                                            currentSpeed.toString()
                                        )
                                    )
                                        .setCategory(NotificationCompat.CATEGORY_CALL)
                                )
                            }
                        }
                    }
                }.catch { e ->
                    e.localizedMessage?.let { Log.e("ERROR", it) } ?: run {Log.e("ERROR", "Unknown error")}
                }
                .launchIn(scope)
        }
}