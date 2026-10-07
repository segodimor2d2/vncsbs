package com.rec.vncsbs

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.test.platform.app.InstrumentationRegistry
import com.rec.vncsbs.ui.MenuSettingsStore
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class MenuOrientationSettingsTest {
    @Test fun portraitDefaultsAndResetDoNotOverwriteLandscapeAdjustments() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val preferenceName = "orientation_test_${UUID.randomUUID()}"
        val context = object : ContextWrapper(base) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                base.getSharedPreferences(preferenceName, mode)
        }
        try {
            val landscape = MenuSettingsStore(context, Configuration.ORIENTATION_LANDSCAPE)
            val portrait = MenuSettingsStore(context, Configuration.ORIENTATION_PORTRAIT)
            val savedLandscape = landscape.defaults().copy(generalPadding = 124, zoomPercent = 510)
            landscape.save(savedLandscape)
            val initial = portrait.load()
            assertEquals(0, initial.generalPadding)
            assertEquals(0, initial.outerSidePadding)
            assertEquals(0, initial.leftViewRightPadding)
            assertEquals(0, initial.rightViewLeftPadding)
            assertEquals(360, initial.panSensitivity)
            assertEquals(330, initial.zoomPercent)
            portrait.save(initial.copy(zoomPercent = 550, generalPadding = 12))
            assertEquals(550, portrait.load().zoomPercent)
            assertEquals(savedLandscape, landscape.load())
            portrait.save(portrait.defaults())
            assertEquals(initial, portrait.load())
            assertEquals(savedLandscape, landscape.load())
        } finally {
            base.getSharedPreferences(preferenceName, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }
}
