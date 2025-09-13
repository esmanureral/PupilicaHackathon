package com.esmanureral.pupilicahackathon.ui.badge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.esmanureral.pupilicahackathon.data.QuizRepository
import com.esmanureral.pupilicahackathon.data.local.QuizSharedPreferences
import com.esmanureral.pupilicahackathon.data.network.ApiClient
import com.esmanureral.pupilicahackathon.databinding.FragmentGameBadgeBinding
import com.esmanureral.pupilicahackathon.domain.GameBadgeList

class GameBadgeFragment : Fragment() {

    private var _binding: FragmentGameBadgeBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: QuizRepository
    private var currentScore: Int = 0

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
        initializeRepository()
        loadCurrentScore()
        setupRecyclerView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initializeRepository() {
        repository = QuizRepository(
            apiService = ApiClient.provideApi(),
            prefs = QuizSharedPreferences(requireContext())
        )
    }

    private fun loadCurrentScore() {
        currentScore = repository.loadScore()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.adapter = GameBadgeAdapter(GameBadgeList.badges, currentScore)
    }
}
