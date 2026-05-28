package io.fastpix.data.utils

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build

/**
 * Utility class to gather device information for the Android Data SDK
 */
class DeviceInfoUtility(
    private val context: Context
) {



    val deviceTypeMobile: String = "Mobile"
    val OSMobile: String = "Android"

    /**
     * Data class to hold device information
     */
    data class DeviceInfo(
        val os: String,
        val osVersion: String,
        val browser: String,
        val browserVersion: String,
        val deviceManufacturer: String,
        val deviceModel: String,
        val deviceName: String,
        val deviceType: String
    )

    /**
     * Get the operating system name
     * @return "Android" as the OS
     */

    fun getOS(): String {
        return OSMobile
    }

    /**
     * Get the Android OS version
     * @return Android version name (e.g., "Android 13")
     */
    fun getOSVersion(): String {
        return Build.VERSION.RELEASE
    }

    /**
     * Get the browser/app package name that is using this SDK
     * @return Package name of the app (e.g., "com.aaonxt.android")
     */
    fun getBrowser(): String {
        return context.applicationContext.packageName
    }

    /**
     * Get the browser/app version name that is using this SDK
     * @return Version name of the app (e.g., "com.aaonxt.android 2.1.4")
     */
    fun getBrowserVersion(): String? {
        return try {
            val packageInfo = context.applicationContext.packageManager.getPackageInfo(
                context.applicationContext.packageName,
                0
            )
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * Get the device manufacturer
     * @return Device manufacturer (e.g., "INFINIX")
     */
    fun getDeviceManufacturer(): String {
        return Build.MANUFACTURER
    }

    /**
     * Get the device model
     * @return Device model (e.g., "Infinix X6832")
     */
    fun getDeviceModel(): String {
        return Build.MODEL
    }

    /**
     * Get the device name
     * @return Device name (e.g., "Infinix")
     */
    fun getDeviceName(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    /**
     * Get the device type
     * @return "Mobile" as the device type
     */
    fun getDeviceType(): String {
        return deviceTypeMobile
    }

    /**
     * Check if device is a tablet
     * @return true if device is a tablet, false otherwise
     */
    fun isTablet(): Boolean {
        return context.resources.configuration.screenLayout and
                Configuration.SCREENLAYOUT_SIZE_MASK >=
                Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    /**
     * Get screen density DPI
     * @return Screen density DPI
     */
    fun getScreenDensity(): Int {
        return context.resources.displayMetrics.densityDpi
    }

    /**
     * Get screen resolution
     * @return Pair of width and height in pixels
     */
    fun getScreenResolution(): Pair<Int, Int> {
        val displayMetrics = context.resources.displayMetrics
        return Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }

    /**
     * Get device category
     */
    fun getDeviceCategory(): String {
        return getDeviceType()
    }

    /**
     * Get software name
     */
    fun getSoftwareName(): String {
        return getOS()
    }

    /**
     * Get software version
     */
    fun getSoftwareVersion(): String {
        return getOSVersion()
    }

    /**
     * Get OS name
     */
    fun getOSName(): String {
        return getOS()
    }
}
