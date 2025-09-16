package com.esmanureral.pupilicahackathon

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.esmanureral.pupilicahackathon.data.local.OnboardingPreferences
import com.esmanureral.pupilicahackathon.databinding.ActivityMainBinding
import com.esmanureral.pupilicahackathon.reminder.ReminderSystemManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        ReminderSystemManager(this).initialize()

        val navController = (supportFragmentManager
            .findFragmentById(R.id.fragmentContainerView) as NavHostFragment)
            .navController

        navController.graph = navController.navInflater.inflate(R.navigation.nav_graph).apply {
            setStartDestination(
                if (OnboardingPreferences(this@MainActivity).isOnboardingCompleted)
                    R.id.homeFragment
                else
                    R.id.onboardingFragment
            )
        }
        binding.bottomNavigation.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment,
                R.id.quizFragment -> {
                    binding.bottomNavigation.visibility = android.view.View.VISIBLE
                }

                else -> {
                    binding.bottomNavigation.visibility = android.view.View.GONE
                }
            }
        }
    }
}