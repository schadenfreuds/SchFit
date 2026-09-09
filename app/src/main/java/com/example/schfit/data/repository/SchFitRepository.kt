package com.example.schfit.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.schfit.data.database.AppDatabase
import com.example.schfit.data.entity.BodyMetricEntity
import com.example.schfit.data.entity.DetailedSetLogModel
import com.example.schfit.data.entity.ExerciseEntity
import com.example.schfit.data.entity.RoutineExerciseEntity
import com.example.schfit.data.entity.RoutineExerciseWithDetails
import com.example.schfit.data.entity.SetLogEntity
import com.example.schfit.data.entity.WorkoutLogEntity
import com.example.schfit.data.entity.WorkoutRoutineEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import org.json.JSONArray
import org.json.JSONObject

data class ActiveWorkoutSessionData(
    val routineId: Long,
    val routineName: String,
    val workoutLogId: Long,
    val currentExerciseIndex: Int,
    val currentSetNumber: Int,
    val startTimeMillis: Long,
    val restEndTimeMillis: Long? = null,
    val totalRestDuration: Int = 90
)

data class ConfigModel(
    val id: String,
    val name: String,
    val description: String,
    val isDefault: Boolean = false,
    val isDev: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class SchFitRepository(
    private val context: Context? = null
) {
    private val globalPrefs: SharedPreferences? =
        context?.getSharedPreferences("schfit_global_preferences", Context.MODE_PRIVATE)

    private val _themeModeFlow = MutableStateFlow(globalPrefs?.getString("theme_mode", "system") ?: "system")
    private val _appLanguageFlow = MutableStateFlow(globalPrefs?.getString("app_language", "system") ?: "system")
    private val _isDeveloperModeFlow = MutableStateFlow(globalPrefs?.getBoolean("is_developer_mode", false) ?: false)

    // Config Management State
    private val _availableConfigsFlow = MutableStateFlow(loadConfigsFromPrefs())
    private val _activeConfigFlow = MutableStateFlow(
        globalPrefs?.getString("active_config_id", "default") ?: "default"
    )

    private val _activeSessionFlow = MutableStateFlow<ActiveWorkoutSessionData?>(null)
    private val _targetWeightFlow = MutableStateFlow<Double?>(null)

    init {
        updateConfigState(getActiveConfigId())
    }

    private fun getDb(): AppDatabase {
        val ctx = context ?: throw IllegalStateException("Context is required for database access")
        return AppDatabase.getDatabase(ctx, _activeConfigFlow.value)
    }

    private fun getConfigPrefs(): SharedPreferences? {
        val configId = _activeConfigFlow.value
        return context?.getSharedPreferences("schfit_prefs_$configId", Context.MODE_PRIVATE)
    }

    // --- CONFIG & PROFILE SANDBOX MANAGEMENT ---
    fun getActiveConfigId(): String = _activeConfigFlow.value

    fun getActiveConfigFlow(): Flow<String> = _activeConfigFlow

    fun getAvailableConfigsFlow(): Flow<List<ConfigModel>> = _availableConfigsFlow

    fun getAvailableConfigs(): List<ConfigModel> = _availableConfigsFlow.value

    fun switchConfig(configId: String) {
        val exists = _availableConfigsFlow.value.any { it.id == configId }
        if (!exists) return

        globalPrefs?.edit()?.putString("active_config_id", configId)?.apply()
        _activeConfigFlow.value = configId
        updateConfigState(configId)
    }

    fun createConfig(name: String, description: String = ""): ConfigModel {
        val trimmedName = name.trim().ifBlank { "Yeni Profil" }
        val id = "cfg_" + System.currentTimeMillis()
        val newConfig = ConfigModel(
            id = id,
            name = trimmedName,
            description = description.trim().ifBlank { "Özel Veri Ortamı" },
            isDefault = false,
            isDev = false
        )

        val updatedList = _availableConfigsFlow.value + newConfig
        saveConfigsToPrefs(updatedList)
        _availableConfigsFlow.value = updatedList
        switchConfig(id)
        return newConfig
    }

    fun deleteConfig(configId: String) {
        if (configId == "default" || configId == "dev_test") return // Do not delete default/dev configs

        val updatedList = _availableConfigsFlow.value.filter { it.id != configId }
        saveConfigsToPrefs(updatedList)
        _availableConfigsFlow.value = updatedList

        if (_activeConfigFlow.value == configId) {
            switchConfig("default")
        }
    }

    private fun updateConfigState(configId: String) {
        val p = getConfigPrefs()
        _targetWeightFlow.value = if (p?.contains("target_weight_kg") == true) {
            p.getFloat("target_weight_kg", 0f).toDouble()
        } else null
        _activeSessionFlow.value = readActiveWorkoutSession()
    }

    private fun loadConfigsFromPrefs(): List<ConfigModel> {
        val defaultList = listOf(
            ConfigModel(
                id = "default",
                name = "Kişisel (Ana)",
                description = "🎯 Gerçek antrenman ve kilo kayıtların",
                isDefault = true,
                isDev = false
            ),
            ConfigModel(
                id = "dev_test",
                name = "Geliştirici (Test)",
                description = "🛠️ Test, deneme ve mock veriler ortamı",
                isDefault = false,
                isDev = true
            )
        )

        val jsonString = globalPrefs?.getString("schfit_config_list", null) ?: return defaultList
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<ConfigModel>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ConfigModel(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        description = obj.optString("description", ""),
                        isDefault = obj.optBoolean("isDefault", false),
                        isDev = obj.optBoolean("isDev", false)
                    )
                )
            }
            if (list.none { it.id == "default" }) defaultList else list
        } catch (_: Exception) {
            defaultList
        }
    }

    private fun saveConfigsToPrefs(list: List<ConfigModel>) {
        val jsonArray = JSONArray()
        for (cfg in list) {
            val obj = JSONObject()
            obj.put("id", cfg.id)
            obj.put("name", cfg.name)
            obj.put("description", cfg.description)
            obj.put("isDefault", cfg.isDefault)
            obj.put("isDev", cfg.isDev)
            jsonArray.put(obj)
        }
        globalPrefs?.edit()?.putString("schfit_config_list", jsonArray.toString())?.apply()
    }

    // --- Developer Mode Options ---
    fun isDeveloperMode(): Boolean = _isDeveloperModeFlow.value

    fun getIsDeveloperModeFlow(): Flow<Boolean> = _isDeveloperModeFlow

    fun enableDeveloperMode() {
        globalPrefs?.edit()?.putBoolean("is_developer_mode", true)?.apply()
        _isDeveloperModeFlow.value = true
    }

    fun disableDeveloperMode() {
        globalPrefs?.edit()?.putBoolean("is_developer_mode", false)?.apply()
        _isDeveloperModeFlow.value = false
        if (_activeConfigFlow.value != "default") {
            switchConfig("default")
        }
    }

    // --- Target Weight & User Info Preferences (Stored per-config) ---
    fun getTargetWeight(): Double? = _targetWeightFlow.value

    fun getTargetWeightFlow(): Flow<Double?> = _targetWeightFlow

    fun setTargetWeight(targetWeight: Double?) {
        val p = getConfigPrefs()
        if (targetWeight == null || targetWeight <= 0.0) {
            p?.edit()?.remove("target_weight_kg")?.apply()
            _targetWeightFlow.value = null
        } else {
            p?.edit()?.putFloat("target_weight_kg", targetWeight.toFloat())?.apply()
            _targetWeightFlow.value = targetWeight
        }
    }

    fun getWeeklyWorkoutGoal(): Int = getConfigPrefs()?.getInt("weekly_workout_goal", 4) ?: 4

    fun setWeeklyWorkoutGoal(goal: Int) {
        getConfigPrefs()?.edit()?.putInt("weekly_workout_goal", goal)?.apply()
    }

    fun getUserName(): String = getConfigPrefs()?.getString("user_name", "Can") ?: "Can"

    fun setUserName(name: String) {
        getConfigPrefs()?.edit()?.putString("user_name", name)?.apply()
    }

    fun getUserHeight(): Int = getConfigPrefs()?.getInt("user_height", 185) ?: 185

    fun setUserHeight(height: Int) {
        getConfigPrefs()?.edit()?.putInt("user_height", height)?.apply()
    }

    fun getUserGoal(): String = getConfigPrefs()?.getString("user_goal", "Hipertrofi & Kas Kazanımı") ?: "Hipertrofi & Kas Kazanımı"

    fun setUserGoal(goal: String) {
        getConfigPrefs()?.edit()?.putString("user_goal", goal)?.apply()
    }

    // Theme & Sound Global Preferences
    fun getThemeMode(): String = globalPrefs?.getString("theme_mode", "system") ?: "system"

    fun getThemeModeFlow(): Flow<String> = _themeModeFlow

    fun setThemeMode(theme: String) {
        globalPrefs?.edit()?.putString("theme_mode", theme)?.apply()
        _themeModeFlow.value = theme
    }

    fun getAppLanguage(): String = globalPrefs?.getString("app_language", "tr") ?: "tr"

    fun getAppLanguageFlow(): Flow<String> = _appLanguageFlow

    fun setAppLanguage(languageCode: String) {
        globalPrefs?.edit()?.putString("app_language", languageCode)?.apply()
        _appLanguageFlow.value = languageCode
    }

    fun getSoundType(): String = globalPrefs?.getString("sound_type", "beep") ?: "beep"

    fun setSoundType(sound: String) {
        globalPrefs?.edit()?.putString("sound_type", sound)?.apply()
    }

    fun isVibrationEnabled(): Boolean = globalPrefs?.getBoolean("vibration_enabled", true) ?: true

    fun setVibrationEnabled(enabled: Boolean) {
        globalPrefs?.edit()?.putBoolean("vibration_enabled", enabled)?.apply()
    }

    fun isKeepScreenOnEnabled(): Boolean = globalPrefs?.getBoolean("keep_screen_on", true) ?: true

    fun setKeepScreenOnEnabled(enabled: Boolean) {
        globalPrefs?.edit()?.putBoolean("keep_screen_on", enabled)?.apply()
    }

    suspend fun resetAllUserData() {
        val db = getDb()
        db.workoutLogDao().deleteAllSetLogs()
        db.workoutLogDao().deleteAllWorkoutLogs()
        db.bodyMetricDao().deleteAllMetrics()
        clearActiveWorkoutSession()
        setWeeklyWorkoutGoal(4)
        setTargetWeight(null)
    }

    // --- DEVELOPER TOOLS & MOCK DATA SEEDER ---
    suspend fun seedMockDataForDev() {
        val db = getDb()
        seedDefaultExercisesIfNeeded()
        val allExercises = db.exerciseDao().getAllExercisesList()
        val benchPress = allExercises.find { it.name.contains("Bench Press", ignoreCase = true) } ?: allExercises.firstOrNull()
        val latPull = allExercises.find { it.name.contains("Lat Pull", ignoreCase = true) } ?: allExercises.firstOrNull()
        val deadlift = allExercises.find { it.name.contains("Deadlift", ignoreCase = true) } ?: allExercises.firstOrNull()
        val shoulderPress = allExercises.find { it.name.contains("Shoulder", ignoreCase = true) } ?: allExercises.firstOrNull()
        val bicepCurl = allExercises.find { it.name.contains("Curl", ignoreCase = true) } ?: allExercises.firstOrNull()

        val chestTricepsRoutineId = createRoutineWithExercises(
            name = "Göğüs & Triceps Canavarı ⚡",
            exercises = listOf(
                (benchPress?.id ?: 1L) to (4 to 90),
                (allExercises.find { it.name.contains("Chest Press", ignoreCase = true) }?.id ?: 1L) to (3 to 60),
                (allExercises.find { it.name.contains("Pec Dec", ignoreCase = true) }?.id ?: 1L) to (3 to 60),
                (allExercises.find { it.name.contains("Triceps Extension", ignoreCase = true) }?.id ?: 1L) to (3 to 60)
            )
        )

        val backBicepsRoutineId = createRoutineWithExercises(
            name = "Sırt & Biceps Güç 🔥",
            exercises = listOf(
                (latPull?.id ?: 1L) to (4 to 90),
                (deadlift?.id ?: 1L) to (4 to 120),
                (bicepCurl?.id ?: 1L) to (3 to 60)
            )
        )

        val now = System.currentTimeMillis()
        val dayMillis = 24L * 60L * 60L * 1000L

        val mockSessions = listOf(
            Triple(now - (28L * dayMillis), 55, chestTricepsRoutineId to listOf(Pair(benchPress?.id ?: 1L, listOf(60.0 to 10, 70.0 to 8, 75.0 to 6)))),
            Triple(now - (25L * dayMillis), 50, backBicepsRoutineId to listOf(Pair(latPull?.id ?: 1L, listOf(50.0 to 12, 55.0 to 10, 60.0 to 8)))),
            Triple(now - (22L * dayMillis), 60, chestTricepsRoutineId to listOf(Pair(benchPress?.id ?: 1L, listOf(70.0 to 10, 80.0 to 8, 85.0 to 5)))),
            Triple(now - (19L * dayMillis), 52, backBicepsRoutineId to listOf(Pair(deadlift?.id ?: 1L, listOf(80.0 to 8, 100.0 to 6, 120.0 to 4)))),
            Triple(now - (16L * dayMillis), 58, chestTricepsRoutineId to listOf(Pair(benchPress?.id ?: 1L, listOf(75.0 to 10, 85.0 to 7, 90.0 to 5)))),
            Triple(now - (13L * dayMillis), 48, backBicepsRoutineId to listOf(Pair(latPull?.id ?: 1L, listOf(60.0 to 10, 65.0 to 8, 70.0 to 6)))),
            Triple(now - (10L * dayMillis), 62, chestTricepsRoutineId to listOf(Pair(benchPress?.id ?: 1L, listOf(80.0 to 8, 90.0 to 6, 95.0 to 4)))),
            Triple(now - (7L * dayMillis), 54, backBicepsRoutineId to listOf(Pair(deadlift?.id ?: 1L, listOf(100.0 to 8, 120.0 to 5, 140.0 to 3)))),
            Triple(now - (5L * dayMillis), 50, chestTricepsRoutineId to listOf(Pair(shoulderPress?.id ?: 1L, listOf(30.0 to 10, 35.0 to 8, 42.5 to 6)))),
            Triple(now - (3L * dayMillis), 56, chestTricepsRoutineId to listOf(Pair(benchPress?.id ?: 1L, listOf(85.0 to 8, 95.0 to 5, 100.0 to 3)))),
            Triple(now - (1L * dayMillis), 65, backBicepsRoutineId to listOf(Pair(latPull?.id ?: 1L, listOf(65.0 to 10, 70.0 to 8, 75.0 to 6))))
        )

        for ((timestamp, duration, routineAndExercises) in mockSessions) {
            val logId = db.workoutLogDao().insertWorkoutLog(
                WorkoutLogEntity(
                    routineId = routineAndExercises.first,
                    timestamp = timestamp,
                    durationMinutes = duration
                )
            )

            for ((exId, sets) in routineAndExercises.second) {
                sets.forEachIndexed { setIdx, (w, r) ->
                    db.workoutLogDao().insertSetLog(
                        SetLogEntity(
                            workoutLogId = logId,
                            exerciseId = exId,
                            setNumber = setIdx + 1,
                            weightKg = w,
                            reps = r,
                            timestamp = timestamp + (setIdx * 120_000L)
                        )
                    )
                }
            }
        }

        val weightHistory = listOf(
            86.5 to (now - 28L * dayMillis),
            86.0 to (now - 24L * dayMillis),
            85.2 to (now - 20L * dayMillis),
            84.8 to (now - 16L * dayMillis),
            84.0 to (now - 12L * dayMillis),
            83.4 to (now - 8L * dayMillis),
            82.7 to (now - 5L * dayMillis),
            82.0 to (now - 2L * dayMillis),
            81.5 to now
        )

        for ((weight, time) in weightHistory) {
            db.bodyMetricDao().insertMetric(BodyMetricEntity(weightKg = weight, timestamp = time))
        }

        setTargetWeight(75.0)
    }

    suspend fun flushAllDatabaseForDev() {
        val db = getDb()
        db.workoutLogDao().deleteAllSetLogs()
        db.workoutLogDao().deleteAllWorkoutLogs()
        db.bodyMetricDao().deleteAllMetrics()
        val routines = db.workoutRoutineDao().getAllRoutinesList()
        for (r in routines) {
            deleteRoutine(r.id)
        }
        clearActiveWorkoutSession()
        setTargetWeight(null)
    }

    // --- Active Workout Session Persistence ---
    fun getActiveWorkoutSessionFlow(): Flow<ActiveWorkoutSessionData?> = _activeSessionFlow

    fun getActiveWorkoutSession(): ActiveWorkoutSessionData? = _activeSessionFlow.value ?: readActiveWorkoutSession()

    fun saveActiveWorkoutSession(session: ActiveWorkoutSessionData) {
        val p = getConfigPrefs()
        val editor = p?.edit()
        if (editor != null) {
            editor.putLong("active_routine_id", session.routineId)
            editor.putString("active_routine_name", session.routineName)
            editor.putLong("active_workout_log_id", session.workoutLogId)
            editor.putInt("active_current_ex_idx", session.currentExerciseIndex)
            editor.putInt("active_current_set_num", session.currentSetNumber)
            editor.putLong("active_start_time", session.startTimeMillis)
            if (session.restEndTimeMillis != null) {
                editor.putLong("active_rest_end_time", session.restEndTimeMillis)
                editor.putInt("active_total_rest_duration", session.totalRestDuration)
            } else {
                editor.remove("active_rest_end_time")
            }
            editor.apply()
        }

        _activeSessionFlow.value = session
    }

    fun clearActiveWorkoutSession() {
        val p = getConfigPrefs()
        p?.edit()
            ?.remove("active_routine_id")
            ?.remove("active_routine_name")
            ?.remove("active_workout_log_id")
            ?.remove("active_current_ex_idx")
            ?.remove("active_current_set_num")
            ?.remove("active_start_time")
            ?.remove("active_rest_end_time")
            ?.remove("active_total_rest_duration")
            ?.apply()

        _activeSessionFlow.value = null
    }

    private fun readActiveWorkoutSession(): ActiveWorkoutSessionData? {
        val p = getConfigPrefs() ?: return null
        if (!p.contains("active_workout_log_id")) return null

        val routineId = p.getLong("active_routine_id", 0L)
        val routineName = p.getString("active_routine_name", "Antrenman") ?: "Antrenman"
        val logId = p.getLong("active_workout_log_id", 0L)
        val exIdx = p.getInt("active_current_ex_idx", 0)
        val setNum = p.getInt("active_current_set_num", 1)
        val startTime = p.getLong("active_start_time", System.currentTimeMillis())
        val restEndTime = if (p.contains("active_rest_end_time")) p.getLong("active_rest_end_time", 0L) else null
        val totalRest = p.getInt("active_total_rest_duration", 90)

        return ActiveWorkoutSessionData(
            routineId = routineId,
            routineName = routineName,
            workoutLogId = logId,
            currentExerciseIndex = exIdx,
            currentSetNumber = setNum,
            startTimeMillis = startTime,
            restEndTimeMillis = restEndTime,
            totalRestDuration = totalRest
        )
    }

    // --- Exercises (Bound to active config) ---
    fun getAllExercises(): Flow<List<ExerciseEntity>> = _activeConfigFlow.flatMapLatest { configId ->
        AppDatabase.getDatabase(context!!, configId).exerciseDao().getAllExercises()
    }

    suspend fun addCustomExercise(name: String, muscleGroup: String, notes: String = ""): Long {
        val exercise = ExerciseEntity(
            name = name,
            muscleGroup = muscleGroup,
            notes = notes,
            isCustom = true
        )
        return getDb().exerciseDao().insertExercise(exercise)
    }

    suspend fun updateExercise(exercise: ExerciseEntity) {
        getDb().exerciseDao().updateExercise(exercise)
    }

    suspend fun deleteExercise(exerciseId: Long) {
        getDb().exerciseDao().deleteExerciseById(exerciseId)
    }

    suspend fun seedDefaultExercisesIfNeeded() {
        val db = getDb()
        val count = db.exerciseDao().countExercises()
        if (count == 0) {
            db.exerciseDao().insertAllExercises(AppDatabase.DEFAULT_EXERCISES)
        }
    }

    // --- Routines (Bound to active config) ---
    fun getAllRoutines(): Flow<List<WorkoutRoutineEntity>> = _activeConfigFlow.flatMapLatest { configId ->
        AppDatabase.getDatabase(context!!, configId).workoutRoutineDao().getAllRoutines()
    }

    fun getRoutineById(id: Long): Flow<WorkoutRoutineEntity?> = _activeConfigFlow.flatMapLatest { configId ->
        AppDatabase.getDatabase(context!!, configId).workoutRoutineDao().getRoutineByIdFlow(id)
    }

    fun getRoutineExercises(routineId: Long): Flow<List<RoutineExerciseWithDetails>> = _activeConfigFlow.flatMapLatest { configId ->
        AppDatabase.getDatabase(context!!, configId).workoutRoutineDao().getRoutineExercisesWithDetails(routineId)
    }

    suspend fun getRoutineExercisesList(routineId: Long): List<RoutineExerciseWithDetails> =
        getDb().workoutRoutineDao().getRoutineExercisesList(routineId)

    suspend fun createRoutineWithExercises(
        name: String,
        exercises: List<Pair<Long, Pair<Int, Int>>>
    ): Long {
        val db = getDb()
        val routineId = db.workoutRoutineDao().insertRoutine(WorkoutRoutineEntity(name = name))
        val routineExercises = exercises.mapIndexed { index, pair ->
            RoutineExerciseEntity(
                routineId = routineId,
                exerciseId = pair.first,
                targetSets = pair.second.first,
                restSeconds = pair.second.second,
                orderIndex = index
            )
        }
        db.workoutRoutineDao().insertRoutineExercises(routineExercises)
        return routineId
    }

    suspend fun updateRoutineWithExercises(
        routineId: Long,
        name: String,
        exercises: List<Pair<Long, Pair<Int, Int>>>
    ) {
        val db = getDb()
        db.workoutRoutineDao().updateRoutine(WorkoutRoutineEntity(id = routineId, name = name))
        db.workoutRoutineDao().deleteRoutineExercises(routineId)
        val routineExercises = exercises.mapIndexed { index, pair ->
            RoutineExerciseEntity(
                routineId = routineId,
                exerciseId = pair.first,
                targetSets = pair.second.first,
                restSeconds = pair.second.second,
                orderIndex = index
            )
        }
        db.workoutRoutineDao().insertRoutineExercises(routineExercises)
    }

    suspend fun deleteRoutine(routineId: Long) {
        val db = getDb()
        db.workoutRoutineDao().deleteRoutineExercises(routineId)
        db.workoutRoutineDao().deleteRoutineById(routineId)
    }

    // --- Workout Logs & Sets (Bound to active config) ---
    suspend fun createWorkoutLog(routineId: Long?): Long {
        val log = WorkoutLogEntity(
            routineId = routineId,
            timestamp = System.currentTimeMillis(),
            durationMinutes = 0
        )
        return getDb().workoutLogDao().insertWorkoutLog(log)
    }

    suspend fun updateWorkoutLog(workoutLog: WorkoutLogEntity) {
        getDb().workoutLogDao().updateWorkoutLog(workoutLog)
    }

    suspend fun getWorkoutLogById(id: Long): WorkoutLogEntity? =
        getDb().workoutLogDao().getWorkoutLogById(id)

    fun getAllWorkoutLogs(): Flow<List<WorkoutLogEntity>> = _activeConfigFlow.flatMapLatest { configId ->
        AppDatabase.getDatabase(context!!, configId).workoutLogDao().getAllWorkoutLogs()
    }

    suspend fun insertSetLog(
        workoutLogId: Long,
        exerciseId: Long,
        setNumber: Int,
        weightKg: Double,
        reps: Int
    ): Long {
        val setLog = SetLogEntity(
            workoutLogId = workoutLogId,
            exerciseId = exerciseId,
            setNumber = setNumber,
            weightKg = weightKg,
            reps = reps,
            timestamp = System.currentTimeMillis()
        )
        return getDb().workoutLogDao().insertSetLog(setLog)
    }

    fun getSetLogsForWorkout(workoutLogId: Long): Flow<List<SetLogEntity>> = _activeConfigFlow.flatMapLatest { configId ->
        AppDatabase.getDatabase(context!!, configId).workoutLogDao().getSetLogsForWorkout(workoutLogId)
    }

    suspend fun getSetLogsListForWorkout(workoutLogId: Long): List<SetLogEntity> =
        getDb().workoutLogDao().getSetLogsListForWorkout(workoutLogId)

    suspend fun getLatestSetForExercise(exerciseId: Long): SetLogEntity? =
        getDb().workoutLogDao().getLatestSetForExercise(exerciseId)

    fun getLatestSetForExerciseFlow(exerciseId: Long): Flow<SetLogEntity?> = _activeConfigFlow.flatMapLatest { configId ->
        AppDatabase.getDatabase(context!!, configId).workoutLogDao().getLatestSetForExerciseFlow(exerciseId)
    }

    fun getAllSetLogsForExerciseFlow(exerciseId: Long): Flow<List<SetLogEntity>> = _activeConfigFlow.flatMapLatest { configId ->
        AppDatabase.getDatabase(context!!, configId).workoutLogDao().getAllSetLogsForExerciseFlow(exerciseId)
    }

    suspend fun getAllSetLogsForExercise(exerciseId: Long): List<SetLogEntity> =
        getDb().workoutLogDao().getAllSetLogsForExercise(exerciseId)

    suspend fun getPreviousWorkoutSetsForExercise(exerciseId: Long, currentWorkoutLogId: Long): List<SetLogEntity> =
        getDb().workoutLogDao().getPreviousWorkoutSetsForExercise(exerciseId, currentWorkoutLogId)

    fun getTotalVolumeFlow(): Flow<Double> = _activeConfigFlow.flatMapLatest { configId ->
        AppDatabase.getDatabase(context!!, configId).workoutLogDao().getTotalVolumeFlow()
    }

    suspend fun getVolumeForWorkout(workoutLogId: Long): Double =
        getDb().workoutLogDao().getVolumeForWorkout(workoutLogId)

    fun getTotalWorkoutsCount(): Flow<Int> = _activeConfigFlow.flatMapLatest { configId ->
        AppDatabase.getDatabase(context!!, configId).workoutLogDao().getTotalWorkoutsCount()
    }

    suspend fun deleteWorkout(workoutLogId: Long) {
        val db = getDb()
        db.workoutLogDao().deleteSetLogsForWorkout(workoutLogId)
        db.workoutLogDao().deleteWorkoutLogById(workoutLogId)
    }

    suspend fun getDetailedSetLogsForWorkout(workoutLogId: Long): List<DetailedSetLogModel> =
        getDb().workoutLogDao().getDetailedSetLogsForWorkout(workoutLogId)

    // --- Body Metrics (Bound to active config) ---
    fun getAllMetrics(): Flow<List<BodyMetricEntity>> = _activeConfigFlow.flatMapLatest { configId ->
        AppDatabase.getDatabase(context!!, configId).bodyMetricDao().getAllMetrics()
    }

    fun getFirstMetricFlow(): Flow<BodyMetricEntity?> = _activeConfigFlow.flatMapLatest { configId ->
        AppDatabase.getDatabase(context!!, configId).bodyMetricDao().getFirstMetricFlow()
    }

    fun getLatestMetricFlow(): Flow<BodyMetricEntity?> = _activeConfigFlow.flatMapLatest { configId ->
        AppDatabase.getDatabase(context!!, configId).bodyMetricDao().getLatestMetricFlow()
    }

    suspend fun addBodyMetric(weightKg: Double): Long {
        val metric = BodyMetricEntity(weightKg = weightKg, timestamp = System.currentTimeMillis())
        return getDb().bodyMetricDao().insertMetric(metric)
    }

    suspend fun deleteBodyMetric(id: Long) {
        getDb().bodyMetricDao().deleteMetricById(id)
    }
}
