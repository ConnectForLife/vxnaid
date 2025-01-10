package com.idi.vaccinetracker.sync.domain.usecases.error

import android.os.Build
import com.idi.vaccinetracker.sync.data.models.DeviceHardware
import javax.inject.Inject

class BuildDeviceHardwareUseCase @Inject constructor() {

    fun build(): DeviceHardware {
        return DeviceHardware(
            androidVersion = Build.VERSION.RELEASE,
            model = Build.MODEL,
            device = Build.DEVICE,
            product = Build.PRODUCT,
        )
    }
}