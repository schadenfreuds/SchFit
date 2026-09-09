package com.example.schfit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.schfit.data.entity.WorkoutLogEntity
import com.example.schfit.data.repository.ActiveWorkoutSessionData
import com.example.schfit.data.repository.SchFitRepository
import com.example.schfit.util.LocaleHelper
import com.example.schfit.util.MotivationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class HomeMetricsIntermediate(
    val firstWeight: Double?,
    val currentWeight: Double?,
    val allLogs: List<WorkoutLogEntity>,
    val targetWeight: Double?
)

data class HomeUiControlsIntermediate(
    val weeklyGoal: Int,
    val isGoalDialogOpen: Boolean,
    val monthOffset: Int,
    val activeSession: ActiveWorkoutSessionData?,
    val appLanguage: String
)

data class CalendarDay(
    val dayOfMonth: Int,
    val dateMillis: Long,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isFuture: Boolean,
    val isWorkoutDone: Boolean,
    val workoutCount: Int,
    val formattedDateStr: String
)

data class MonthCalendarState(
    val year: Int,
    val month: Int,
    val monthName: String,
    val days: List<CalendarDay>,
    val totalWorkoutsInMonth: Int,
    val isCurrentRealMonth: Boolean
)

data class HomeUiState(
    val initialWeight: Double? = null,
    val currentWeight: Double? = null,
    val weightDifference: Double? = null,
    val targetWeight: Double? = null,
    val remainingToGoal: Double? = null,
    val totalWorkouts: Int = 0,
    val weeklyWorkoutsCount: Int = 0,
    val weeklyGoal: Int = 4,
    val isGoalDialogOpen: Boolean = false,
    val monthCalendar: MonthCalendarState = MonthCalendarState(
        year = 2026,
        month = 7,
        monthName = "",
        days = emptyList(),
        totalWorkoutsInMonth = 0,
        isCurrentRealMonth = true
    ),
    val streakWeeks: Int = 0,
    val activeWorkoutSession: ActiveWorkoutSessionData? = null,
    val motivationalQuote: String = "Bugün kendinin en iyi versiyonu ol!",
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val repository: SchFitRepository
) : ViewModel() {

    private val _weeklyGoal = MutableStateFlow(repository.getWeeklyWorkoutGoal())
    private val _isGoalDialogOpen = MutableStateFlow(false)
    private val _monthOffset = MutableStateFlow(0) // 0: güncel cihaz ayı, -1: önceki ay, +1: sonraki ay

    init {
        viewModelScope.launch {
            repository.seedDefaultExercisesIfNeeded()
        }
    }

    private fun getStartOfWeekMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.setFirstDayOfWeek(Calendar.MONDAY)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private val metricsFlow = combine(
        repository.getFirstMetricFlow(),
        repository.getLatestMetricFlow(),
        repository.getAllWorkoutLogs(),
        repository.getTargetWeightFlow()
    ) { firstMetric, latestMetric, allLogs, targetWeight ->
        HomeMetricsIntermediate(firstMetric?.weightKg, latestMetric?.weightKg, allLogs, targetWeight)
    }

    private val uiControlsFlow = combine(
        _weeklyGoal,
        _isGoalDialogOpen,
        _monthOffset,
        repository.getActiveWorkoutSessionFlow(),
        repository.getAppLanguageFlow()
    ) { weeklyGoal, isGoalDialogOpen, monthOffset, activeSession, appLanguage ->
        HomeUiControlsIntermediate(weeklyGoal, isGoalDialogOpen, monthOffset, activeSession, appLanguage)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        metricsFlow,
        uiControlsFlow
    ) { metrics, controls ->
        val firstWeight = metrics.firstWeight
        val currentWeight = metrics.currentWeight
        val targetWeight = metrics.targetWeight
        val diff = if (firstWeight != null && currentWeight != null) {
            currentWeight - firstWeight
        } else null

        val remainingToGoal = if (currentWeight != null && targetWeight != null) {
            targetWeight - currentWeight
        } else null

        val weeklyGoal = controls.weeklyGoal
        val isGoalDialogOpen = controls.isGoalDialogOpen
        val monthOffset = controls.monthOffset
        val activeSession = controls.activeSession
        val appLanguage = controls.appLanguage

        val startOfWeek = getStartOfWeekMillis()
        val completedLogs = metrics.allLogs.filter { it.durationMinutes > 0 }
        val totalWorkouts = completedLogs.size
        val weeklyWorkouts = completedLogs.count { it.timestamp >= startOfWeek }

        // GERÇEK ZAMANLI AYLIK TAKVİM OLUŞTURMA
        val currentCal = Calendar.getInstance()
        val realTodayYear = currentCal.get(Calendar.YEAR)
        val realTodayMonth = currentCal.get(Calendar.MONTH)
        val realTodayDay = currentCal.get(Calendar.DAY_OF_MONTH)

        val daySeed = (realTodayYear * 372L + realTodayMonth * 31L + realTodayDay.toLong())
        val motivation = MotivationProvider.getDailyQuote(appLanguage, daySeed)

        val activeLocale = LocaleHelper.getLocale(appLanguage)
        val monthFormatter = SimpleDateFormat("MMMM yyyy", activeLocale)
        val dateFormatter = SimpleDateFormat("d MMMM yyyy", activeLocale)

        val targetCal = Calendar.getInstance().apply {
            add(Calendar.MONTH, monthOffset)
        }
        val targetYear = targetCal.get(Calendar.YEAR)
        val targetMonth = targetCal.get(Calendar.MONTH)

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, targetYear)
            set(Calendar.MONTH, targetMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val leadingEmptyDays = when (firstDayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }

        val monthName = monthFormatter.format(cal.time).replaceFirstChar { it.titlecase(activeLocale) }

        val dayList = mutableListOf<CalendarDay>()

        for (i in 0 until leadingEmptyDays) {
            dayList.add(
                CalendarDay(
                    dayOfMonth = 0,
                    dateMillis = 0L,
                    isCurrentMonth = false,
                    isToday = false,
                    isFuture = false,
                    isWorkoutDone = false,
                    workoutCount = 0,
                    formattedDateStr = ""
                )
            )
        }

        val dayMillis = 24L * 60L * 60L * 1000L
        var monthWorkoutsCount = 0

        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val dayStart = cal.timeInMillis
            val dayEnd = dayStart + dayMillis

            val isToday = (targetYear == realTodayYear && targetMonth == realTodayMonth && day == realTodayDay)
            val isFuture = if (targetYear > realTodayYear) {
                true
            } else if (targetYear == realTodayYear && targetMonth > realTodayMonth) {
                true
            } else if (targetYear == realTodayYear && targetMonth == realTodayMonth && day > realTodayDay) {
                true
            } else {
                false
            }

            val workoutsOnDay = completedLogs.count { it.timestamp in dayStart until dayEnd }
            if (workoutsOnDay > 0) {
                monthWorkoutsCount += workoutsOnDay
            }

            dayList.add(
                CalendarDay(
                    dayOfMonth = day,
                    dateMillis = dayStart,
                    isCurrentMonth = true,
                    isToday = isToday,
                    isFuture = isFuture,
                    isWorkoutDone = (workoutsOnDay > 0),
                    workoutCount = workoutsOnDay,
                    formattedDateStr = dateFormatter.format(cal.time)
                )
            )
        }

        val monthCalendarState = MonthCalendarState(
            year = targetYear,
            month = targetMonth,
            monthName = monthName,
            days = dayList,
            totalWorkoutsInMonth = monthWorkoutsCount,
            isCurrentRealMonth = (monthOffset == 0)
        )

        // Haftalık seri (Streak) hesaplama
        var streak = 0
        val checkWeekCalendar = Calendar.getInstance().apply {
            setFirstDayOfWeek(Calendar.MONDAY)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        while (true) {
            val weekStart = checkWeekCalendar.timeInMillis
            val weekEnd = weekStart + (7L * dayMillis)
            val workoutsInWeek = completedLogs.count { it.timestamp in weekStart until weekEnd }
            if (workoutsInWeek > 0) {
                streak++
                checkWeekCalendar.add(Calendar.WEEK_OF_YEAR, -1)
            } else {
                if (streak == 0) {
                    checkWeekCalendar.add(Calendar.WEEK_OF_YEAR, -1)
                    val prevWeekStart = checkWeekCalendar.timeInMillis
                    val prevWeekEnd = prevWeekStart + (7L * dayMillis)
                    if (completedLogs.count { it.timestamp in prevWeekStart until prevWeekEnd } > 0) {
                        streak++
                        checkWeekCalendar.add(Calendar.WEEK_OF_YEAR, -1)
                        continue
                    }
                }
                break
            }
        }

        HomeUiState(
            initialWeight = firstWeight,
            currentWeight = currentWeight,
            weightDifference = diff,
            targetWeight = targetWeight,
            remainingToGoal = remainingToGoal,
            totalWorkouts = totalWorkouts,
            weeklyWorkoutsCount = weeklyWorkouts,
            weeklyGoal = weeklyGoal,
            isGoalDialogOpen = isGoalDialogOpen,
            monthCalendar = monthCalendarState,
            streakWeeks = maxOf(streak, if (totalWorkouts > 0) 1 else 0),
            activeWorkoutSession = activeSession,
            motivationalQuote = motivation,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun previousMonth() {
        _monthOffset.update { it - 1 }
    }

    fun nextMonth() {
        _monthOffset.update { it + 1 }
    }

    fun resetToCurrentMonth() {
        _monthOffset.value = 0
    }

    fun openGoalDialog() {
        _isGoalDialogOpen.value = true
    }

    fun closeGoalDialog() {
        _isGoalDialogOpen.value = false
    }

    fun setWeeklyGoal(newGoal: Int) {
        val coerced = newGoal.coerceIn(1, 7)
        repository.setWeeklyWorkoutGoal(coerced)
        _weeklyGoal.value = coerced
        closeGoalDialog()
    }

    fun updateWeeklyGoal(newGoal: Int) {
        setWeeklyGoal(newGoal)
    }

    fun cancelActiveWorkoutSession() {
        repository.clearActiveWorkoutSession()
    }
}
