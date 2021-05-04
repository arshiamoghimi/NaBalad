package ir.sambal.nabalad

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate


object ThemeHelper {
    const val LIGHT_MODE = "light"
    const val DARK_MODE = "dark"
    var currentMode = LIGHT_MODE

    fun applyTheme(themePref: String) {
        when (themePref) {
            LIGHT_MODE -> {
                currentMode = LIGHT_MODE
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            DARK_MODE -> {
                currentMode = DARK_MODE
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                }
            }
        }
    }
}