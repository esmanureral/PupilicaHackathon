package com.esmanureral.pupilicahackathon.presentation.main

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.databinding.ActivityMainBinding
import com.esmanureral.pupilicahackathon.data.local.OnboardingPreferences
import com.esmanureral.pupilicahackathon.presentation.reminder.ReminderSystemManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        ReminderSystemManager(this).initialize()

        val navHostFragment = binding.fragmentContainerView.getFragment<NavHostFragment>()
        val navController = navHostFragment.navController

        checkOnboardingStatus(navController)

        binding.bottomNavigation.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.isVisible = when (destination.id) {
                R.id.homeFragment, R.id.chatFragment, R.id.reminderFragment, R.id.gameBadgeFragment -> true
                else -> false
            }
        }
    }

    private fun checkOnboardingStatus(navController: androidx.navigation.NavController) {
        val onboardingPrefs = OnboardingPreferences(this)
        if (!onboardingPrefs.isOnboardingCompleted) {
            navController.navigate(R.id.onboardingFragment)
        }
    }
}