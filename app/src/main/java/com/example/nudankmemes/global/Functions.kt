package com.example.nudankmemes.global

import android.app.Activity

class Functions {

    companion object {
        fun updateCompatModeSharedPrefs(activity: Activity) {
            val sharedPref = activity.getSharedPreferences("sharedPrefs", 0)
            val editor = sharedPref?.edit()
            editor?.putBoolean("config_compatibility_mode_buttons",
                Variables.config_compatibility_mode_buttons
            )
            editor?.apply()
        }

        fun loadCompatModeSharedPrefs(activity: Activity) {
            val sharedPref = activity.getSharedPreferences("sharedPrefs", 0)
            Variables.config_compatibility_mode_buttons = sharedPref?.getBoolean("config_compatibility_mode_buttons", false)!!
        }
    }

}