package com.rc.common.car_app_service

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.rc.common.car_app_service.speed_control.SpeedControl

class SpeedHeraldAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return SpeedHeraldSession()
    }
}

class SpeedHeraldSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        SpeedControl.activateSpeedControl(carContext = carContext)
        return MainScreen(carContext = carContext)
    }
}