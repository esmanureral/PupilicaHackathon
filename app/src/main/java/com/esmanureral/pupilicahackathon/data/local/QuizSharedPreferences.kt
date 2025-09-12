package com.esmanureral.pupilicahackathon.data.local

import android.content.Context
import android.content.SharedPreferences

private const val PREFS_NAME = "quiz_game"
private const val QUESTION_INDEX_KEY = "question_index"

class QuizSharedPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadQuestionIndex(): Int {
        return prefs.getInt(QUESTION_INDEX_KEY, 1)
    }

    fun saveQuestionIndex(index: Int) {
        prefs.edit().putInt(QUESTION_INDEX_KEY, index).apply()
    }
}