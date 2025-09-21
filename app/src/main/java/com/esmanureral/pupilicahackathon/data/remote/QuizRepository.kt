package com.esmanureral.pupilicahackathon.data.remote

import com.esmanureral.pupilicahackathon.data.local.QuizPreferences
import com.esmanureral.pupilicahackathon.data.model.QuizResponse

class QuizRepository(
    private val apiService: QuizApiService,
    private val preferences: QuizPreferences
) {

    suspend fun fetchQuiz(): QuizResponse {
        return apiService.fetchQuiz()
    }

    fun loadQuestionIndex(): Int {
        return preferences.loadQuestionIndex()
    }

    fun saveQuestionIndex(index: Int) {
        preferences.saveQuestionIndex(index)
    }

    fun loadCurrentQuestionId(): String? {
        return preferences.loadCurrentQuestionId()
    }

    fun saveCurrentQuestionId(id: String?) {
        preferences.saveCurrentQuestionId(id)
    }

    fun loadAnsweredQuestions(): Set<String> {
        return preferences.loadAnsweredQuestions()
    }

    fun saveAnsweredQuestions(ids: Set<String>) {
        preferences.saveAnsweredQuestions(ids)
    }

    fun loadCurrentSelectedOption(): String? {
        return preferences.loadCurrentSelectedOption()
    }

    fun saveCurrentSelectedOption(optionKey: String?) {
        preferences.saveCurrentSelectedOption(optionKey)
    }

    fun loadScore(): Int {
        return preferences.loadScore()
    }

    fun saveScore(score: Int) {
        preferences.saveScore(score)
    }
}