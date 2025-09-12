package com.esmanureral.pupilicahackathon.ui.badge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.esmanureral.pupilicahackathon.data.QuizRepository
import com.esmanureral.pupilicahackathon.data.local.QuizSharedPreferences
import com.esmanureral.pupilicahackathon.data.network.ApiClient
import com.esmanureral.pupilicahackathon.databinding.FragmentGameBadgeBinding
import com.esmanureral.pupilicahackathon.domain.GameBadgeList

class GameBadgeFragment : Fragment() {

    private var _binding: FragmentGameBadgeBinding? = null
    private val binding get() = _binding!!

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

        val repository = QuizRepository(
            apiService = ApiClient.provideApi(),
            prefs = QuizSharedPreferences(requireContext())
        )
        val score = repository.loadScore()

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = GameBadgeAdapter(GameBadgeList.badges, score)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


