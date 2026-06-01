package io.fastpix.data.pref

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class ViewerPrefs(context: Context) {

    companion object {
        private const val KEY_VIEWER_NAME = "viewer_name"
        private const val KEY_SDK_URL = "sdk_url"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("viewer_prefs", Context.MODE_PRIVATE)

    // Save a simple string (e.g., viewer name)
    fun viewerId(viewerName: String) {
        prefs.edit { putString(KEY_VIEWER_NAME, viewerName) }
    }

    // Get the stored string (viewer name)`
    fun getViewerId(): String? {
        return prefs.getString(KEY_VIEWER_NAME, null)
    }

    // Clear the stored string
    fun clearViewerName() {
        prefs.edit { remove(KEY_VIEWER_NAME) }
    }

    // Save SDK URL for persistence across process restarts
    fun saveSdkUrl(sdkUrl: String) {
        prefs.edit { putString(KEY_SDK_URL, sdkUrl) }
    }

    // Get the stored SDK URL
    fun getSdkUrl(): String? {
        return prefs.getString(KEY_SDK_URL, null)
    }

    // Clear the stored SDK URL
    fun clearSdkUrl() {
        prefs.edit { remove(KEY_SDK_URL) }
    }
}
