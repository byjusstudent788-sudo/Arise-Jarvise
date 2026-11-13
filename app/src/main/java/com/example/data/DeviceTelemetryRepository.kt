package com.example.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.util.Log
import java.util.Locale

private const val TAG = "DeviceTelemetryRepo"

data class SystemTelemetry(
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val chargingSource: String = "AC",
    val batteryTempCelsius: Float = 28.0f,
    val batteryHealthLabel: String = "OPTIMAL",
    val totalRamGb: Float = 0f,
    val availableRamGb: Float = 0f,
    val ramUsagePercent: Int = 0,
    val totalStorageGb: Float = 0f,
    val availableStorageGb: Float = 0f,
    val storageUsagePercent: Int = 0,
    val networkStatus: String = "UNKNOWN",
    val isNetworkConnected: Boolean = false,
    val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    val androidVersion: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    val uptimeFormatted: String = "0m",
    val hasFlashlight: Boolean = false,
    val isFlashlightOn: Boolean = false
) {
    fun toPromptContext(): String {
        return """
            |LIVE DEVICE TELEMETRY:
            |- Device: $deviceModel ($androidVersion)
            |- Battery: $batteryPercent% (${if (isCharging) "Charging via $chargingSource" else "Discharging"}), Temp: ${String.format(Locale.US, "%.1f", batteryTempCelsius)}°C, Health: $batteryHealthLabel
            |- Memory (RAM): ${String.format(Locale.US, "%.2f", availableRamGb)} GB free / ${String.format(Locale.US, "%.2f", totalRamGb)} GB total ($ramUsagePercent% used)
            |- Internal Storage: ${String.format(Locale.US, "%.1f", availableStorageGb)} GB free / ${String.format(Locale.US, "%.1f", totalStorageGb)} GB total ($storageUsagePercent% used)
            |- Network: $networkStatus (Connected: $isNetworkConnected)
            |- System Uptime: $uptimeFormatted
            |- Tactical Flashlight: ${if (!hasFlashlight) "Unavailable on hardware" else if (isFlashlightOn) "ENGAGED" else "STANDBY"}
        """.trimMargin()
    }
}

class DeviceTelemetryRepository(private val context: Context) {

    private val cameraManager: CameraManager? =
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private var torchCameraId: String? = findTorchCameraId()
    private var torchEnabled: Boolean = false

    private fun findTorchCameraId(): String? {
        val manager = cameraManager ?: return null
        return try {
            manager.cameraIdList.firstOrNull { id ->
                val characteristics = manager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK
            } ?: manager.cameraIdList.firstOrNull { id ->
                manager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to inspect camera flash characteristics", e)
            null
        }
    }

    fun setFlashlight(enabled: Boolean): Result<Boolean> {
        val manager = cameraManager
        val cameraId = torchCameraId ?: findTorchCameraId().also { torchCameraId = it }
        if (manager == null || cameraId == null) {
            return Result.failure(IllegalStateException("Hardware flashlight module not detected on this device."))
        }
        return try {
            manager.setTorchMode(cameraId, enabled)
            torchEnabled = enabled
            Result.success(enabled)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle torch mode", e)
            Result.failure(e)
        }
    }

    fun readTelemetry(): SystemTelemetry {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPercent = if (level >= 0 && scale > 0) (level * 100) / scale else 100

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val chargingSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "WIRELESS"
            else -> "BATTERY"
        }

        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280) ?: 280
        val batteryTempCelsius = tempTenths / 10f

        val healthInt = batteryIntent?.getIntExtra(
            BatteryManager.EXTRA_HEALTH,
            BatteryManager.BATTERY_HEALTH_GOOD
        ) ?: BatteryManager.BATTERY_HEALTH_GOOD
        val healthLabel = when (healthInt) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "OPTIMAL"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "THERMAL HIGH"
            BatteryManager.BATTERY_HEALTH_DEAD -> "CRITICAL"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "OVER VOLTAGE"
            BatteryManager.BATTERY_HEALTH_COLD -> "LOW TEMP"
            else -> "NOMINAL"
        }

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)
        val bytesPerGb = 1024f * 1024f * 1024f
        val totalRamGb = memInfo.totalMem / bytesPerGb
        val availableRamGb = memInfo.availMem / bytesPerGb
        val ramUsagePercent = if (memInfo.totalMem > 0L) {
            (((memInfo.totalMem - memInfo.availMem).toDouble() / memInfo.totalMem) * 100).toInt()
        } else 0

        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        val availBytes = stat.availableBlocksLong * stat.blockSizeLong
        val totalStorageGb = totalBytes / bytesPerGb
        val availableStorageGb = availBytes / bytesPerGb
        val storageUsagePercent = if (totalBytes > 0L) {
            (((totalBytes - availBytes).toDouble() / totalBytes) * 100).toInt()
        } else 0

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetwork
        val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
        val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val networkStatus = when {
            capabilities == null -> "OFFLINE"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI LINK"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR 5G/LTE"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN TUNNEL"
            isConnected -> "ONLINE"
            else -> "DISCONNECTED"
        }

        val uptimeMs = SystemClock.elapsedRealtime()
        val totalMinutes = (uptimeMs / 60_000L).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        val uptimeFormatted = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

        return SystemTelemetry(
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            chargingSource = chargingSource,
            batteryTempCelsius = batteryTempCelsius,
            batteryHealthLabel = healthLabel,
            totalRamGb = totalRamGb,
            availableRamGb = availableRamGb,
            ramUsagePercent = ramUsagePercent.coerceIn(0, 100),
            totalStorageGb = totalStorageGb,
            availableStorageGb = availableStorageGb,
            storageUsagePercent = storageUsagePercent.coerceIn(0, 100),
            networkStatus = networkStatus,
            isNetworkConnected = isConnected,
            deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            uptimeFormatted = uptimeFormatted,
            hasFlashlight = torchCameraId != null,
            isFlashlightOn = torchEnabled
        )
    }
}
