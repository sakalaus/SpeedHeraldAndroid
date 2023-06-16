package com.rc.common.car_app_service.speed_control

sealed interface SpeedControlState {
    object ACTIVATING : SpeedControlState
    object DEACTIVATED : SpeedControlState
    class ACTIVATED(val speedLimit: Int) : SpeedControlState
}