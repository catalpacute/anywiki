package org.wikipedia.anywiki.storage

import android.content.Context
import org.wikipedia.anywiki.mediawiki.ReaderThemeMode

class ReaderSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(ANYWIKI_PREFS, Context.MODE_PRIVATE)

    fun getThemeMode(): ReaderThemeMode {
        val raw = prefs.getString(KEY_THEME_MODE, ReaderThemeMode.SYSTEM.name).orEmpty()
        return runCatching { ReaderThemeMode.valueOf(raw) }.getOrDefault(ReaderThemeMode.SYSTEM)
    }

    fun setThemeMode(mode: ReaderThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun getFontScale(): Float {
        val scale = prefs.getFloat(KEY_FONT_SCALE, DEFAULT_FONT_SCALE)
        return scale.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
    }

    fun setFontScale(scale: Float) {
        prefs.edit().putFloat(KEY_FONT_SCALE, scale.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)).apply()
    }

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_FONT_SCALE = "font_scale"
        const val MIN_FONT_SCALE = 0.85f
        const val MAX_FONT_SCALE = 1.4f
        const val DEFAULT_FONT_SCALE = 1.0f
    }
}
