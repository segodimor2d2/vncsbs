package com.rec.vncsbs.ui

import android.content.Context
import com.rec.vncsbs.R

data class MenuSettings(
    val generalPadding: Int,
    val outerSidePadding: Int,
    val leftViewRightPadding: Int,
    val rightViewLeftPadding: Int,
    val zoomPercent: Int
)

class MenuSettingsStore(context: Context) {
    private val resources = context.resources
    private val preferences = context.getSharedPreferences("menu_settings", Context.MODE_PRIVATE)

    fun defaults() = MenuSettings(
        resources.getInteger(R.integer.menu_default_general_padding).coerceIn(0, 100),
        resources.getInteger(R.integer.menu_default_outer_side_padding).coerceIn(0, 100),
        resources.getInteger(R.integer.menu_default_left_view_right_padding).coerceIn(0, 100),
        resources.getInteger(R.integer.menu_default_right_view_left_padding).coerceIn(0, 100),
        resources.getInteger(R.integer.menu_default_zoom_percent).coerceIn(25, 400)
    )

    fun load(): MenuSettings {
        val initial = defaults()
        return MenuSettings(
            preferences.getInt("general", initial.generalPadding).coerceIn(0, 100),
            preferences.getInt("outer_sides", initial.outerSidePadding).coerceIn(0, 100),
            preferences.getInt("left_right", initial.leftViewRightPadding).coerceIn(0, 100),
            preferences.getInt("right_left", initial.rightViewLeftPadding).coerceIn(0, 100),
            preferences.getInt("zoom_percent", initial.zoomPercent).coerceIn(25, 400)
        )
    }

    fun save(settings: MenuSettings) {
        preferences.edit()
            .putInt("general", settings.generalPadding)
            .putInt("outer_sides", settings.outerSidePadding)
            .putInt("left_right", settings.leftViewRightPadding)
            .putInt("right_left", settings.rightViewLeftPadding)
            .putInt("zoom_percent", settings.zoomPercent)
            .apply()
    }
}
