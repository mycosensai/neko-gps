package com.nekogps.app.features.speedcamera

/**
 * Sample speed camera seed data used on first launch.
 * In production, this would load from a database or API.
 */
object DefaultSpeedCameras {

    fun cameras(): List<SpeedCamera> {
        return uptownCameras() + downtownCameras()
    }

    private fun uptownCameras(): List<SpeedCamera> {
        return listOf(
            SpeedCamera(
                id = "cam_001",
                latitude = 40.7580,
                longitude = -73.9855,
                type = CameraType.FIXED,
                speedLimitKmh = 50,
                description = "Times Square - Fixed Camera"
            ),
            SpeedCamera(
                id = "cam_002",
                latitude = 40.7484,
                longitude = -73.9857,
                type = CameraType.RED_LIGHT,
                speedLimitKmh = 50,
                description = "Empire State - Red Light"
            ),
            SpeedCamera(
                id = "cam_003",
                latitude = 40.7614,
                longitude = -73.9776,
                type = CameraType.MOBILE,
                speedLimitKmh = 40,
                description = "5th Ave - Mobile Zone"
            ),
            SpeedCamera(
                id = "cam_004",
                latitude = 40.7527,
                longitude = -73.9772,
                type = CameraType.AVERAGE_SPEED,
                speedLimitKmh = 40,
                description = "Park Ave - Average Speed"
            )
        )
    }

    private fun downtownCameras(): List<SpeedCamera> {
        return listOf(
            SpeedCamera(
                id = "cam_005",
                latitude = 40.7061,
                longitude = -74.0087,
                type = CameraType.FIXED,
                speedLimitKmh = 30,
                description = "Financial District - Fixed"
            ),
            SpeedCamera(
                id = "cam_006",
                latitude = 40.7282,
                longitude = -73.9942,
                type = CameraType.TRAFFIC_LIGHT,
                speedLimitKmh = 40,
                description = "Union Square - Traffic Light"
            ),
            SpeedCamera(
                id = "cam_007",
                latitude = 40.7411,
                longitude = -73.9897,
                type = CameraType.FIXED,
                speedLimitKmh = 35,
                description = "Flatiron - Fixed Camera"
            ),
            SpeedCamera(
                id = "cam_008",
                latitude = 40.7589,
                longitude = -73.9851,
                type = CameraType.MOBILE,
                speedLimitKmh = 50,
                description = "Broadway - Mobile Zone"
            )
        )
    }
}
