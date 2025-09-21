package com.esmanureral.pupilicahackathon.utils

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

object AnalysisCounter {
    
    private const val PREFS_NAME = "analysis_counter_prefs"
    private const val KEY_DAILY_COUNT = "daily_count"
    private const val KEY_LAST_DATE = "last_date"
    private const val MAX_DAILY_ANALYSES = 3
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private fun getCurrentDateString(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}-${calendar.get(Calendar.DAY_OF_MONTH)}"
    }
    
    fun getRemainingAnalyses(context: Context): Int {
        val prefs = getPrefs(context)
        val currentDate = getCurrentDateString()
        val lastDate = prefs.getString(KEY_LAST_DATE, "")
        
        if (lastDate != currentDate) {
            resetDailyCount(context)
            return MAX_DAILY_ANALYSES
        }
        
        val currentCount = prefs.getInt(KEY_DAILY_COUNT, 0)
        return maxOf(0, MAX_DAILY_ANALYSES - currentCount)
    }
    
    fun getUsedAnalyses(context: Context): Int {
        val prefs = getPrefs(context)
        val currentDate = getCurrentDateString()
        val lastDate = prefs.getString(KEY_LAST_DATE, "")
        
        if (lastDate != currentDate) {
            resetDailyCount(context)
            return 0
        }
        
        return prefs.getInt(KEY_DAILY_COUNT, 0)
    }
    
    fun canPerformAnalysis(context: Context): Boolean {
        return getRemainingAnalyses(context) > 0
    }
    
    fun incrementAnalysisCount(context: Context) {
        val prefs = getPrefs(context)
        val currentDate = getCurrentDateString()
        val lastDate = prefs.getString(KEY_LAST_DATE, "")
        
        if (lastDate != currentDate) {
            resetDailyCount(context)
        }
        
        val currentCount = prefs.getInt(KEY_DAILY_COUNT, 0)
        prefs.edit()
            .putInt(KEY_DAILY_COUNT, currentCount + 1)
            .putString(KEY_LAST_DATE, currentDate)
            .apply()
    }
    
    private fun resetDailyCount(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit()
            .putInt(KEY_DAILY_COUNT, 0)
            .putString(KEY_LAST_DATE, getCurrentDateString())
            .apply()
    }
}
