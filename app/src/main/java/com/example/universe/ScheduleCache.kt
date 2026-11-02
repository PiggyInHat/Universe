package com.example.universe

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ScheduleCache {

    private const val PREFS_NAME = "schedule_cache"
    private const val KEY_SCHEDULE = "schedule_json"
    private const val KEY_DATE = "schedule_date"

    fun saveSchedule(context: Context, schedule: List<ScheduleItem>, date: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(schedule)
        prefs.edit()
            .putString(KEY_SCHEDULE, json)
            .putString(KEY_DATE, date)
            .apply()
    }

    fun loadSchedule(context: Context): List<ScheduleItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SCHEDULE, null) ?: return emptyList()
        val type = object : TypeToken<List<ScheduleItem>>() {}.type
        return try {
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getLastDate(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DATE, "") ?: ""
    }
}