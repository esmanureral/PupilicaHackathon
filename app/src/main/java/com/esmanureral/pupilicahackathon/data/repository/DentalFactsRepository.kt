package com.esmanureral.pupilicahackathon.data.repository

import android.content.Context
import com.esmanureral.pupilicahackathon.data.model.DentalFact
import com.esmanureral.pupilicahackathon.data.utils.DentalFactsLoader

class DentalFactsRepository(private val context: Context) {

    private val facts: List<DentalFact> by lazy {
        DentalFactsLoader.loadDentalFacts(context)
    }

    fun getRandomFacts(count: Int = 3): List<DentalFact> {
        return facts.shuffled().take(count)
    }
}