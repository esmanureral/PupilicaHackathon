package com.esmanureral.pupilicahackathon.presentation.common.utils

import android.content.Context
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.domain.model.DentalFact
import com.esmanureral.pupilicahackathon.domain.model.DentalFactJson
import com.esmanureral.pupilicahackathon.domain.model.FactCategory
import com.google.gson.Gson
import java.io.IOException

object DentalFactsLoader {
    
    fun loadDentalFacts(context: Context): List<DentalFact> {
        return try {
            val jsonString = loadJsonFromAssets(context, "dental_facts.json")
            val dentalFactsArray = Gson().fromJson(jsonString, Array<DentalFactJson>::class.java)
            
            dentalFactsArray.mapNotNull { jsonFact ->
                try {
                    DentalFact(
                        id = jsonFact.id,
                        title = jsonFact.title,
                        description = jsonFact.description,
                        iconResId = getDrawableResourceId(context, jsonFact.iconResId),
                        category = FactCategory.valueOf(jsonFact.category)
                    )
                } catch (e: Exception) {
                    android.util.Log.e("DentalFactsLoader", "Error parsing fact: ${jsonFact.title}", e)
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DentalFactsLoader", "Error loading dental facts", e)
            getDefaultFacts(context)
        }
    }
    
    private fun getDefaultFacts(context: Context): List<DentalFact> {
        return listOf(
            DentalFact(
                id = 1,
                title = context.getString(R.string.default_fact_1_title),
                description = context.getString(R.string.default_fact_1_description),
                iconResId = R.drawable.tooth,
                category = FactCategory.BRUSHING
            ),
            DentalFact(
                id = 2,
                title = context.getString(R.string.default_fact_2_title),
                description = context.getString(R.string.default_fact_2_description),
                iconResId = R.drawable.tooth,
                category = FactCategory.CLEANING
            ),
            DentalFact(
                id = 3,
                title = context.getString(R.string.default_fact_3_title),
                description = context.getString(R.string.default_fact_3_description),
                iconResId = R.drawable.tooth,
                category = FactCategory.PROTECTION
            )
        )
    }
    
    private fun loadJsonFromAssets(context: Context, fileName: String): String {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            throw Exception("Could not load $fileName from assets", e)
        }
    }
    
    private fun getDrawableResourceId(context: Context, iconName: String): Int {
        return try {
            val resourceId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
            if (resourceId != 0) {
                resourceId
            } else {
                R.drawable.tooth
            }
        } catch (e: Exception) {
            R.drawable.tooth
        }
    }
}