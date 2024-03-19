package com.example.nudankmemes.helpers

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SharedPrefManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("favorite_memes", Context.MODE_PRIVATE)

    fun saveMeme(key: String) {
        val timestamp = SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.getDefault()).format(Date())
        val editor = sharedPreferences.edit()
        editor.putString(key, timestamp)
        editor.apply()
        Toast.makeText(context, "Added to Favourites", Toast.LENGTH_SHORT).show()
    }

    fun getMeme(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    fun getAllMemes(): Map<String, String> {
        return sharedPreferences.all as Map<String, String>
    }

    fun removeMeme(key: String) {
        val editor = sharedPreferences.edit()
        editor.remove(key)
        editor.apply()
    }
}