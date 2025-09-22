package com.esmanureral.pupilicahackathon.domain.repository

import android.content.Context
import com.esmanureral.pupilicahackathon.domain.model.DentalFact
import com.esmanureral.pupilicahackathon.presentation.common.utils.DentalFactsLoader

class DentalFactsRepository(private val context: Context) {

    private val facts: List<DentalFact> by lazy {
        DentalFactsLoader.loadDentalFacts(context)
    }

    fun getRandomFacts(count: Int = 3): List<DentalFact> {
        return facts.shuffled().take(count)
    }
}