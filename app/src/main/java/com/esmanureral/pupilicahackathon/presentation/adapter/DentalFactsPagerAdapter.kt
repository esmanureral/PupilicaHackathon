package com.esmanureral.pupilicahackathon.presentation.home.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.esmanureral.pupilicahackathon.domain.model.DentalFact
import com.esmanureral.pupilicahackathon.presentation.feature.home.DentalFactFragment

class DentalFactsPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val facts: List<DentalFact>
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = facts.size

    override fun createFragment(position: Int): Fragment {
        return DentalFactFragment.newInstance(facts[position])
    }
}