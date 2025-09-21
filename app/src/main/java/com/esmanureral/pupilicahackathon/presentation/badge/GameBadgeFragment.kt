package com.esmanureral.pupilicahackathon.presentation.badge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.esmanureral.pupilicahackathon.data.remote.QuizRepository
import com.esmanureral.pupilicahackathon.data.local.QuizSharedPreferences
import com.esmanureral.pupilicahackathon.data.local.QuizPreferencesImpl
import com.esmanureral.pupilicahackathon.data.remote.ApiClient
import com.esmanureral.pupilicahackathon.databinding.FragmentGameBadgeBinding

class GameBadgeFragment : Fragment() {

    private var _binding: FragmentGameBadgeBinding? = null
    private val binding get() = _binding!!

    private val badgeAdapter = GameBadgeAdapter()

    private val viewModel: GameBadgeViewModel by viewModels {
        GameBadgeViewModelFactory(
            QuizRepository(
                apiService = ApiClient.provideApi(),
                preferences = QuizPreferencesImpl(QuizSharedPreferences(requireContext()))
            )
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameBadgeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBackButton()
        setupRecyclerView()
        setupObservers()
    }

    private fun setupBackButton() {
        with(binding) {
            toolbar.setNavigationOnClickListener {
                findNavController().popBackStack()
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.adapter = badgeAdapter
    }

    private fun setupObservers() = with(binding) {
        viewModel.uiState.observe(viewLifecycleOwner) { uiState ->
            badgeAdapter.submitList(uiState.badgesWithState)

            progressBar.visibility = if (uiState.isLoading) View.VISIBLE else View.GONE
            recyclerView.visibility =
                if (!uiState.isLoading && uiState.error == null) View.VISIBLE else View.GONE
            errorText.apply {
                text = uiState.error
                visibility = if (uiState.error != null) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        _binding = null
    }
}
