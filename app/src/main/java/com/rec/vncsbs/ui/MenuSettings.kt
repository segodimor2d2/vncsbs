package com.rec.vncsbs.ui

import android.content.Context
import android.content.res.Configuration
import com.rec.vncsbs.R

data class MenuSettings(
    val generalPadding: Int,
    val outerSidePadding: Int,
    val leftViewRightPadding: Int,
    val rightViewLeftPadding: Int,
    val zoomPercent: Int,
    val panX: Int = 0,
    val panY: Int = 0,
    val panSensitivity: Int,
    val gyroPanEnabled: Boolean,
    val gyroQuietThreshold: Int,
    val gyroQuietTimeMs: Int,
    val gyroAutoCenterEnabled: Boolean,
    val mouseCaptureEnabled: Boolean
)

class MenuSettingsStore(
    context: Context,
    orientation: Int = context.resources.configuration.orientation
) {
    private val isPortrait = orientation == Configuration.ORIENTATION_PORTRAIT
    private val resources = context.resources
    private val preferences = context.getSharedPreferences("menu_settings", Context.MODE_PRIVATE)

    private fun displayKey(key: String) = if (isPortrait) "portrait_$key" else key

    private fun displayDefault(landscapeId: Int, portraitId: Int) =
        resources.getInteger(if (isPortrait) portraitId else landscapeId)

    fun defaults() = MenuSettings(
        displayDefault(R.integer.menu_default_general_padding,
            R.integer.menu_default_portrait_general_padding).coerceAtLeast(0),
        displayDefault(R.integer.menu_default_outer_side_padding,
            R.integer.menu_default_portrait_outer_side_padding).coerceAtLeast(0),
        displayDefault(R.integer.menu_default_left_view_right_padding,
            R.integer.menu_default_portrait_left_view_right_padding).coerceIn(0, 100),
        displayDefault(R.integer.menu_default_right_view_left_padding,
            R.integer.menu_default_portrait_right_view_left_padding).coerceIn(0, 100),
        displayDefault(R.integer.menu_default_zoom_percent,
            R.integer.menu_default_portrait_zoom_percent).coerceAtLeast(25),
        panSensitivity = displayDefault(R.integer.menu_default_pan_sensitivity,
            R.integer.menu_default_portrait_pan_sensitivity).coerceAtLeast(10),
        gyroPanEnabled = resources.getBoolean(R.bool.menu_default_gyro_pan_enabled),
        gyroQuietThreshold = resources.getInteger(R.integer.menu_default_gyro_quiet_threshold).coerceIn(1, 100),
        gyroQuietTimeMs = resources.getInteger(R.integer.menu_default_gyro_quiet_time_ms).coerceAtLeast(500),
        gyroAutoCenterEnabled = resources.getBoolean(R.bool.menu_default_gyro_auto_center_enabled),
        mouseCaptureEnabled = resources.getBoolean(R.bool.menu_default_mouse_capture_enabled)
    )

    fun load(): MenuSettings {
        val initial = defaults()
        return MenuSettings(
            preferences.getInt(displayKey("general"), initial.generalPadding).coerceAtLeast(0),
            preferences.getInt(displayKey("outer_sides"), initial.outerSidePadding).coerceAtLeast(0),
            preferences.getInt(displayKey("left_right"), initial.leftViewRightPadding).coerceIn(0, 100),
            preferences.getInt(displayKey("right_left"), initial.rightViewLeftPadding).coerceIn(0, 100),
            preferences.getInt(displayKey("zoom_percent"), initial.zoomPercent).coerceAtLeast(25),
            preferences.getInt(displayKey("pan_x"), initial.panX),
            preferences.getInt(displayKey("pan_y"), initial.panY),
            preferences.getInt(displayKey("pan_sensitivity"), initial.panSensitivity).coerceAtLeast(10),
            preferences.getBoolean("gyro_pan_enabled", initial.gyroPanEnabled),
            preferences.getInt("gyro_quiet_threshold", initial.gyroQuietThreshold).coerceIn(1, 100),
            preferences.getInt("gyro_quiet_time_ms", initial.gyroQuietTimeMs).coerceAtLeast(500),
            gyroAutoCenterEnabled = initial.gyroAutoCenterEnabled,
            mouseCaptureEnabled = initial.mouseCaptureEnabled
        )
    }

    fun save(settings: MenuSettings) {
        preferences.edit()
            .putInt(displayKey("general"), settings.generalPadding)
            .putInt(displayKey("outer_sides"), settings.outerSidePadding)
            .putInt(displayKey("left_right"), settings.leftViewRightPadding)
            .putInt(displayKey("right_left"), settings.rightViewLeftPadding)
            .putInt(displayKey("zoom_percent"), settings.zoomPercent)
            .putInt(displayKey("pan_x"), settings.panX)
            .putInt(displayKey("pan_y"), settings.panY)
            .putInt(displayKey("pan_sensitivity"), settings.panSensitivity)
            .putBoolean("gyro_pan_enabled", settings.gyroPanEnabled)
            .putInt("gyro_quiet_threshold", settings.gyroQuietThreshold)
            .putInt("gyro_quiet_time_ms", settings.gyroQuietTimeMs)
            .apply()
    }
}
