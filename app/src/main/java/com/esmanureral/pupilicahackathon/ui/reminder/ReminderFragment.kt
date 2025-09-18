package com.esmanureral.pupilicahackathon.ui.reminder

import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.core.app.NotificationManagerCompat
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.data.model.ReminderModel
import com.esmanureral.pupilicahackathon.databinding.FragmentReminderBinding
import com.esmanureral.pupilicahackathon.showToast

class ReminderFragment : Fragment() {

    private var _binding: FragmentReminderBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReminderViewModel by viewModels()

    private var morningHour = 8
    private var morningMinute = 0
    private var eveningHour = 21
    private var eveningMinute = 0

    private var selectedDays = mutableSetOf<Int>()
    private var isMorningSelected = false
    private var isEveningSelected = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReminderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeUI()
        setupNavigation()
        setupQuickButtons()
        setupSaveAndClearButtons()
        setupDayCheckboxes()
        setupObservers()
        loadExistingReminders()
    }

    private val dayCheckboxes by lazy {
        with(binding) {
            listOf(
                checkboxSunday,
                checkboxMonday,
                checkboxTuesday,
                checkboxWednesday,
                checkboxThursday,
                checkboxFriday,
                checkboxSaturday
            )
        }
    }

    private fun initializeUI() = with(binding) {
        btnQuickMorning.alpha = 0.7f
        btnQuickEvening.alpha = 0.7f
        updateButtonTexts()
    }

    private fun setupNavigation() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
    }

    private fun setupQuickButtons() = with(binding) {
        btnQuickMorning.setOnClickListener { showTimePicker(true) }
        btnQuickEvening.setOnClickListener { showTimePicker(false) }
    }

    private fun setupSaveAndClearButtons() = with(binding) {
        btnSaveReminder.setOnClickListener { saveReminder() }
        btnClearReminders.setOnClickListener { clearAllReminders() }
    }

    private fun setupDayCheckboxes() {
        val checkboxes = dayCheckboxes.mapIndexed { index, checkbox -> checkbox to index }
        setupEverydayCheckbox(checkboxes)
        setupIndividualDayCheckboxes(checkboxes)
    }

    private fun setupEverydayCheckbox(checkboxes: List<Pair<CheckBox, Int>>) {
        binding.checkboxEveryday.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkboxes.forEach { (checkbox, day) ->
                    checkbox.isChecked = true
                    selectedDays.add(day)
                }
            } else {
                checkboxes.forEach { (checkbox, day) ->
                    checkbox.isChecked = false
                    selectedDays.remove(day)
                }
            }
        }
    }

    private fun setupIndividualDayCheckboxes(checkboxes: List<Pair<CheckBox, Int>>) {
        checkboxes.forEach { (checkbox, day) ->
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedDays.add(day) else selectedDays.remove(day)
                binding.checkboxEveryday.isChecked = selectedDays.size == 7
            }
        }
    }

    private fun showTimePicker(isMorning: Boolean) {
        val initialHour = if (isMorning) morningHour else eveningHour
        val initialMinute = if (isMorning) morningMinute else eveningMinute

        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute -> handleTimeSelected(isMorning, hourOfDay, minute) },
            initialHour,
            initialMinute,
            true
        ).show()
    }

    private fun handleTimeSelected(isMorning: Boolean, hour: Int, minute: Int) {
        if (isMorning) {
            morningHour = hour
            morningMinute = minute
            isMorningSelected = true
            isEveningSelected = false
            highlightQuickButton(binding.btnQuickMorning)
        } else {
            eveningHour = hour
            eveningMinute = minute
            isMorningSelected = false
            isEveningSelected = true
            highlightQuickButton(binding.btnQuickEvening)
        }

        selectAllDays()
        updateButtonTexts()
    }

    private fun saveReminder() {
        if (!validateInputs()) return
        if (!ensureReminderCapabilities()) return
        val reminder = createReminderModel()
        viewModel.saveReminder(reminder)
    }

    private fun ensureReminderCapabilities(): Boolean {
        val ctx = requireContext()

        val notificationsEnabled = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
        if (!notificationsEnabled) {
            showSettingsDialog(
                title = getString(R.string.notification_permission_message),
                message = getString(R.string.notification_permission_message),
            ) { openNotificationSettings() }
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                showSettingsDialog(
                    title = getString(R.string.exact_alarm_permission_title),
                    message = getString(R.string.exact_alarm_permission_message),
                ) { openExactAlarmSettings() }
                return false
            }
        }

        return true
    }

    private fun showSettingsDialog(title: String, message: String, onPositive: () -> Unit) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.open_settings) { _, _ -> onPositive() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun openNotificationSettings() {
        val ctx = requireContext()
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
        }
        startActivity(intent)
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            startActivity(intent)
        }
    }

    private fun validateInputs(): Boolean {
        return when {
            selectedDays.isEmpty() -> {
                showError(getString(R.string.error_no_day)); false
            }

            !isMorningSelected && !isEveningSelected -> {
                showError(getString(R.string.error_no_time)); false
            }

            else -> true
        }
    }

    private fun createReminderModel(): ReminderModel {
        val isMorning = isMorningSelected
        val hour = if (isMorning) morningHour else eveningHour
        val minute = if (isMorning) morningMinute else eveningMinute
        val title = if (isMorning) getString(R.string.reminder_morning_title)
        else getString(R.string.reminder_evening_title)

        return ReminderModel(
            id = System.currentTimeMillis().toInt(),
            title = title,
            description = getString(R.string.reminder_description),
            hour = hour,
            minute = minute,
            isEnabled = true,
            daysOfWeek = selectedDays.toList()
        )
    }

    private fun setupObservers() {
        observeReminderSaved()
        observeErrors()
        observeActiveReminders()
    }

    private fun observeReminderSaved() {
        viewModel.reminderSaved.observe(viewLifecycleOwner) { isSaved ->
            if (isSaved) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.reminder_saved),
                    Toast.LENGTH_SHORT
                ).show()
                loadExistingReminders()
            }
        }
    }

    private fun loadExistingReminders() {
        viewModel.loadActiveReminders()
    }

    private fun observeErrors() {
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            showError(message)
        }
    }

    private fun observeActiveReminders() {
        viewModel.activeReminders.observe(viewLifecycleOwner) { reminders ->
            displayReminders(reminders)
        }
    }

    private fun displayReminders(reminders: List<ReminderModel>) = with(binding) {
        if (reminders.isEmpty()) {
            layoutActiveReminders.visibility = View.GONE
        } else {
            layoutActiveReminders.visibility = View.VISIBLE
            tvActiveReminders.text = generateRemindersText(reminders)
        }
    }

    private fun generateRemindersText(reminders: List<ReminderModel>): String {
        return reminders.joinToString("\n") { reminder ->
            "🕐 ${reminder.getTimeString()} - ${reminder.getDaysString(requireContext())}"
        }
    }

    private fun updateButtonTexts() = with(binding) {
        btnQuickMorning.text =
            getString(R.string.morning_time, formatTime(morningHour, morningMinute))
        btnQuickEvening.text =
            getString(R.string.evening_time, formatTime(eveningHour, eveningMinute))
    }

    private fun formatTime(hour: Int, minute: Int): String =
        "%02d:%02d".format(hour, minute)

    private fun selectAllDays() {
        binding.checkboxEveryday.isChecked = true
        selectedDays.clear()
        selectedDays.addAll(0..6)
        dayCheckboxes.forEach { it.isChecked = true }
    }

    private fun clearAllReminders() {
        viewModel.clearAllReminders()
        requireContext().showToast(getString(R.string.reminders_cleared))
    }

    private fun showError(message: String) = with(binding.tvError) {
        text = message
        visibility = View.VISIBLE
        postDelayed({ binding.tvError.visibility = View.GONE }, 3000)
    }

    private fun highlightQuickButton(selectedButton: android.widget.Button) = with(binding) {
        btnQuickMorning.alpha = 0.7f
        btnQuickEvening.alpha = 0.7f
        selectedButton.alpha = 1.0f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}