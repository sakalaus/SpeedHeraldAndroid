package com.rc.common.car_app_service

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.rc.common.car_app_service.speed_control.SpeedControlState
import com.rc.common.car_app_service.speed_control.SpeedControlState.ACTIVATED
import com.rc.common.car_app_service.speed_control.SpeedControlState.ACTIVATING
import com.rc.common.car_app_service.speed_control.SpeedControlState.DEACTIVATED
import com.rc.common.car_app_service.speed_control.SpeedControl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class MainScreen(carContext: CarContext) : Screen(carContext) {

    private var speedControlState: SpeedControlState = ACTIVATING
    private val screenScope = CoroutineScope(Dispatchers.Default)

    override fun onGetTemplate(): Template {

        val rowTitle: Row = Row.Builder()
            .setTitle(carContext.getString(R.string.speed_herald)).build()

        // This row is updated automatically
        // when a new value is collected from the flow.
        // Those values are emitted when a permission request is answered by a user.

        val rowStatus: Row = Row.Builder()
            .setTitle(
                when (speedControlState) {
                    ACTIVATING -> carContext.getString(R.string.speed_control_activating)
                    DEACTIVATED -> carContext.getString(R.string.speed_control_deactivated)
                    is ACTIVATED -> carContext.getString(
                        R.string.speed_control_activated,
                        (speedControlState as ACTIVATED).speedLimit.toString()
                    )
                },
            ).build()

        SpeedControl.speedControlFlow
            .onEach {
                speedControlState = it
                invalidate()
            }.launchIn(screenScope)

        return PaneTemplate.Builder(
            Pane.Builder()
                .addRow(rowTitle)
                .addRow(rowStatus)
                .build()
        ).build()

    }
}