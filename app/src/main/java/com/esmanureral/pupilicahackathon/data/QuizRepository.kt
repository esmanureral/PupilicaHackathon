package com.esmanureral.pupilicahackathon.data

import com.esmanureral.pupilicahackathon.data.local.QuizSharedPreferences
import com.esmanureral.pupilicahackathon.data.network.QuizApiService
import com.esmanureral.pupilicahackathon.data.network.QuizResponse

class QuizRepository(
    private val apiService: QuizApiService,
    private val prefs: QuizSharedPreferences
) {

    suspend fun fetchQuiz(): QuizResponse {
        return apiService.fetchQuiz()
    }

    fun loadQuestionIndex(): Int {
        return prefs.loadQuestionIndex()
    }

    fun saveQuestionIndex(index: Int) {
        prefs.saveQuestionIndex(index)
    }

    fun loadCurrentQuestionId(): String? {
        return prefs.loadCurrentQuestionId()
    }

    fun saveCurrentQuestionId(id: String?) {
        prefs.saveCurrentQuestionId(id)
    }

    fun loadAnsweredQuestions(): Set<String> {
        return prefs.loadAnsweredQuestions()
    }

    fun saveAnsweredQuestions(ids: Set<String>) {
        prefs.saveAnsweredQuestions(ids)
    }

    fun loadCurrentSelectedOption(): String? {
        return prefs.loadCurrentSelectedOption()
    }

    fun saveCurrentSelectedOption(optionKey: String?) {
        prefs.saveCurrentSelectedOption(optionKey)
    }

    fun loadScore(): Int {
        return prefs.loadScore()
    }

    fun saveScore(score: Int) {
        prefs.saveScore(score)
    }
}