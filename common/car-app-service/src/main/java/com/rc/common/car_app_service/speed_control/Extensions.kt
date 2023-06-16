package com.rc.common.car_app_service.speed_control

fun Float?.convertMsToKmh(): Int = this?.times(3.6)?.toInt() ?: 0