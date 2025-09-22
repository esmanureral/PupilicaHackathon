package com.esmanureral.pupilicahackathon.presentation.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.domain.model.DentalFact
import com.esmanureral.pupilicahackathon.databinding.FragmentDentalFactBinding

class DentalFactFragment : Fragment() {

    private var _binding: FragmentDentalFactBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_FACT = "dental_fact"

        fun newInstance(fact: DentalFact): DentalFactFragment {
            val fragment = DentalFactFragment()
            val args = Bundle()
            args.putParcelable(ARG_FACT, fact)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDentalFactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fact = arguments?.getParcelable<DentalFact>(ARG_FACT)
        fact?.let { bindFact(it) }
    }

    private fun bindFact(fact: DentalFact) {
        binding.apply {
            tvFactTitle.text = fact.title
            tvFactDescription.text = fact.description
            ivFactIcon.setImageResource(fact.iconResId)
            tvFunFactBadge.text =
                getString(R.string.fun_fact_badge, getString(fact.category.stringResId))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}