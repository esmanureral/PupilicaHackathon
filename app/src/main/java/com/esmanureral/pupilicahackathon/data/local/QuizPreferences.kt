package com.esmanureral.pupilicahackathon.data.local

interface QuizPreferences {
    fun loadQuestionIndex(): Int
    fun saveQuestionIndex(index: Int)

    fun loadCurrentQuestionId(): String?
    fun saveCurrentQuestionId(id: String?)

    fun loadAnsweredQuestions(): Set<String>
    fun saveAnsweredQuestions(ids: Set<String>)

    fun loadCurrentSelectedOption(): String?
    fun saveCurrentSelectedOption(optionKey: String?)

    fun loadScore(): Int
    fun saveScore(score: Int)
}

class QuizPreferencesImpl(
    private val prefs: QuizSharedPreferences
) : QuizPreferences {

    override fun loadQuestionIndex(): Int = prefs.loadQuestionIndex()
    override fun saveQuestionIndex(index: Int) = prefs.saveQuestionIndex(index)

    override fun loadCurrentQuestionId(): String? = prefs.loadCurrentQuestionId()
    override fun saveCurrentQuestionId(id: String?) = prefs.saveCurrentQuestionId(id)

    override fun loadAnsweredQuestions(): Set<String> = prefs.loadAnsweredQuestions()
    override fun saveAnsweredQuestions(ids: Set<String>) = prefs.saveAnsweredQuestions(ids)

    override fun loadCurrentSelectedOption(): String? = prefs.loadCurrentSelectedOption()
    override fun saveCurrentSelectedOption(optionKey: String?) =
        prefs.saveCurrentSelectedOption(optionKey)

    override fun loadScore(): Int = prefs.loadScore()
    override fun saveScore(score: Int) = prefs.saveScore(score)
}