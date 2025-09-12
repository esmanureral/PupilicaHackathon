package com.esmanureral.pupilicahackathon.data.local

import android.content.Context
import android.content.SharedPreferences

private const val PREFS_NAME = "quiz_game"
private const val QUESTION_INDEX_KEY = "question_index"
private const val CURRENT_QUESTION_ID_KEY = "current_question_id"
private const val ANSWERED_QUESTIONS_KEY = "answered_questions"
private const val CURRENT_SELECTED_OPTION_KEY = "current_selected_option"
private const val SCORE_KEY = "score"

class QuizSharedPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadQuestionIndex(): Int {
        return prefs.getInt(QUESTION_INDEX_KEY, 1)
    }

    fun saveQuestionIndex(index: Int) {
        prefs.edit().putInt(QUESTION_INDEX_KEY, index).apply()
    }

    fun loadCurrentQuestionId(): String? {
        return prefs.getString(CURRENT_QUESTION_ID_KEY, null)
    }

    fun saveCurrentQuestionId(id: String?) {
        prefs.edit().putString(CURRENT_QUESTION_ID_KEY, id).apply()
    }

    fun loadAnsweredQuestions(): Set<String> {
        return prefs.getStringSet(ANSWERED_QUESTIONS_KEY, emptySet()) ?: emptySet()
    }

    fun saveAnsweredQuestions(ids: Set<String>) {
        prefs.edit().putStringSet(ANSWERED_QUESTIONS_KEY, ids).apply()
    }

    fun loadCurrentSelectedOption(): String? {
        return prefs.getString(CURRENT_SELECTED_OPTION_KEY, null)
    }

    fun saveCurrentSelectedOption(optionKey: String?) {
        prefs.edit().putString(CURRENT_SELECTED_OPTION_KEY, optionKey).apply()
    }

    fun loadScore(): Int {
        return prefs.getInt(SCORE_KEY, 0)
    }

    fun saveScore(score: Int) {
        prefs.edit().putInt(SCORE_KEY, score).apply()
    }
}