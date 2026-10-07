package com.rec.vncsbs.ui

import android.content.Context
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

class MenuSettingsStore(context: Context) {
    private val resources = context.resources
    private val preferences = context.getSharedPreferences("menu_settings", Context.MODE_PRIVATE)

    fun defaults() = MenuSettings(
        resources.getInteger(R.integer.menu_default_general_padding).coerceAtLeast(0),
        resources.getInteger(R.integer.menu_default_outer_side_padding).coerceAtLeast(0),
        resources.getInteger(R.integer.menu_default_left_view_right_padding).coerceIn(0, 100),
        resources.getInteger(R.integer.menu_default_right_view_left_padding).coerceIn(0, 100),
        resources.getInteger(R.integer.menu_default_zoom_percent).coerceAtLeast(25),
        panSensitivity = resources.getInteger(R.integer.menu_default_pan_sensitivity).coerceAtLeast(10),
        gyroPanEnabled = resources.getBoolean(R.bool.menu_default_gyro_pan_enabled),
        gyroQuietThreshold = resources.getInteger(R.integer.menu_default_gyro_quiet_threshold).coerceIn(1, 100),
        gyroQuietTimeMs = resources.getInteger(R.integer.menu_default_gyro_quiet_time_ms).coerceAtLeast(500),
        gyroAutoCenterEnabled = resources.getBoolean(R.bool.menu_default_gyro_auto_center_enabled),
        mouseCaptureEnabled = resources.getBoolean(R.bool.menu_default_mouse_capture_enabled)
    )

    fun load(): MenuSettings {
        val initial = defaults()
        return MenuSettings(
            preferences.getInt("general", initial.generalPadding).coerceAtLeast(0),
            preferences.getInt("outer_sides", initial.outerSidePadding).coerceAtLeast(0),
            preferences.getInt("left_right", initial.leftViewRightPadding).coerceIn(0, 100),
            preferences.getInt("right_left", initial.rightViewLeftPadding).coerceIn(0, 100),
            preferences.getInt("zoom_percent", initial.zoomPercent).coerceAtLeast(25),
            preferences.getInt("pan_x", initial.panX),
            preferences.getInt("pan_y", initial.panY),
            preferences.getInt("pan_sensitivity", initial.panSensitivity).coerceAtLeast(10),
            preferences.getBoolean("gyro_pan_enabled", initial.gyroPanEnabled),
            preferences.getInt("gyro_quiet_threshold", initial.gyroQuietThreshold).coerceIn(1, 100),
            preferences.getInt("gyro_quiet_time_ms", initial.gyroQuietTimeMs).coerceAtLeast(500),
            gyroAutoCenterEnabled = initial.gyroAutoCenterEnabled,
            mouseCaptureEnabled = initial.mouseCaptureEnabled
        )
    }

    fun save(settings: MenuSettings) {
        preferences.edit()
            .putInt("general", settings.generalPadding)
            .putInt("outer_sides", settings.outerSidePadding)
            .putInt("left_right", settings.leftViewRightPadding)
            .putInt("right_left", settings.rightViewLeftPadding)
            .putInt("zoom_percent", settings.zoomPercent)
            .putInt("pan_x", settings.panX)
            .putInt("pan_y", settings.panY)
            .putInt("pan_sensitivity", settings.panSensitivity)
            .putBoolean("gyro_pan_enabled", settings.gyroPanEnabled)
            .putInt("gyro_quiet_threshold", settings.gyroQuietThreshold)
            .putInt("gyro_quiet_time_ms", settings.gyroQuietTimeMs)
            .apply()
    }
}
