package com.esmanureral.pupilicahackathon.presentation.feature.reminder

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
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModelProvider
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.domain.model.ReminderModel
import com.esmanureral.pupilicahackathon.databinding.FragmentReminderBinding
import com.esmanureral.pupilicahackathon.presentation.extensions.showToast
import java.util.Locale

class ReminderFragment : Fragment() {

    private var _binding: FragmentReminderBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReminderViewModel by lazy {
        ViewModelProvider(
            this,
            ReminderViewModelFactory(requireContext())
        )[ReminderViewModel::class.java]
    }

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

    @RequiresApi(Build.VERSION_CODES.O)
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
        morningCard.alpha = 0.7f
        eveningCard.alpha = 0.7f
    }

    private fun setupNavigation() {
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
    }

    private fun setupQuickButtons() = with(binding) {
        imgSunrise.setOnClickListener { showTimePicker(true) }
        imgSunset.setOnClickListener { showTimePicker(false) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupSaveAndClearButtons() = with(binding) {
        btnSaveReminder.setOnClickListener { saveReminder() }
        btnClearSingleReminder.setOnClickListener { clearAllReminders() }
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
        } else {
            eveningHour = hour
            eveningMinute = minute
            isMorningSelected = false
            isEveningSelected = true
        }
        selectAllDays()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveReminder() {
        if (!validateInputs()) return
        if (!ensureReminderCapabilities()) return
        val reminder = createReminderModel()
        viewModel.saveReminder(reminder)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ensureReminderCapabilities(): Boolean {
        return checkNotificationPermission() && checkExactAlarmPermission()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkNotificationPermission(): Boolean {
        val ctx = requireContext()
        val notificationsEnabled = NotificationManagerCompat.from(ctx).areNotificationsEnabled()

        if (!notificationsEnabled) {
            showNotificationPermissionDialog()
            return false
        }
        return true
    }

    private fun checkExactAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val ctx = requireContext()
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (!am.canScheduleExactAlarms()) {
                showExactAlarmPermissionDialog()
                return false
            }
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showNotificationPermissionDialog() {
        showSettingsDialog(
            title = getString(R.string.notification_permission_message),
            message = getString(R.string.notification_permission_message),
        ) { openNotificationSettings() }
    }

    private fun showExactAlarmPermissionDialog() {
        showSettingsDialog(
            title = getString(R.string.exact_alarm_permission_title),
            message = getString(R.string.exact_alarm_permission_message),
        ) { openExactAlarmSettings() }
    }

    private fun showSettingsDialog(title: String, message: String, onPositive: () -> Unit) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.open_settings) { _, _ -> onPositive() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
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
        return validateDaySelection() && validateTimeSelection()
    }

    private fun validateDaySelection(): Boolean {
        if (selectedDays.isEmpty()) {
            showError(getString(R.string.error_no_day))
            return false
        }
        return true
    }

    private fun validateTimeSelection(): Boolean {
        if (!isMorningSelected && !isEveningSelected) {
            showError(getString(R.string.error_no_time))
            return false
        }
        return true
    }

    private fun createReminderModel(): ReminderModel {
        val timeData = getSelectedTimeData()
        val title = getReminderTitle(timeData.isMorning)

        return ReminderModel(
            id = generateReminderId(),
            title = title,
            description = getString(R.string.reminder_description),
            hour = timeData.hour,
            minute = timeData.minute,
            isEnabled = true,
            daysOfWeek = selectedDays.toList()
        )
    }

    private data class TimeData(val hour: Int, val minute: Int, val isMorning: Boolean)

    private fun getSelectedTimeData(): TimeData {
        val isMorning = isMorningSelected
        return TimeData(
            hour = if (isMorning) morningHour else eveningHour,
            minute = if (isMorning) morningMinute else eveningMinute,
            isMorning = isMorning
        )
    }

    private fun getReminderTitle(isMorning: Boolean): String {
        return if (isMorning) {
            getString(R.string.reminder_morning_title)
        } else {
            getString(R.string.reminder_evening_title)
        }
    }

    private fun generateReminderId(): Int {
        return System.currentTimeMillis().toInt()
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
            hideRemindersLayout()
        } else {
            showRemindersLayout(reminders)
        }
    }

    private fun hideRemindersLayout() = with(binding) {
        layoutActiveReminders.visibility = View.GONE
    }

    private fun showRemindersLayout(reminders: List<ReminderModel>) = with(binding) {
        layoutActiveReminders.visibility = View.VISIBLE
        tvActiveReminderItem.text = generateRemindersText(reminders)
    }

    private fun generateRemindersText(reminders: List<ReminderModel>): String {
        return reminders.joinToString("\n") { reminder ->
            "🕐 ${getTimeString(reminder)} - ${getDaysString(reminder)}"
        }
    }

    private fun getTimeString(reminder: ReminderModel): String {
        return String.format(Locale.getDefault(), "%02d:%02d", reminder.hour, reminder.minute)
    }

    private fun getDaysString(reminder: ReminderModel): String {
        val dayNames = listOf(
            getString(R.string.day_sunday),
            getString(R.string.day_monday),
            getString(R.string.day_tuesday),
            getString(R.string.day_wednesday),
            getString(R.string.day_thursday),
            getString(R.string.day_friday),
            getString(R.string.day_saturday)
        )

        return if (reminder.daysOfWeek.isEmpty()) {
            getString(R.string.every_day)
        } else {
            reminder.daysOfWeek
                .filter { it in 0..6 } // Safe bounds check
                .joinToString(", ") { dayNames[it] }
        }
    }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}