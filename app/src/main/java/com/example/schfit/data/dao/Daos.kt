package com.example.schfit.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.schfit.data.entity.BodyMetricEntity
import com.example.schfit.data.entity.DetailedSetLogModel
import com.example.schfit.data.entity.ExerciseEntity
import com.example.schfit.data.entity.RoutineExerciseEntity
import com.example.schfit.data.entity.RoutineExerciseWithDetails
import com.example.schfit.data.entity.SetLogEntity
import com.example.schfit.data.entity.WorkoutLogEntity
import com.example.schfit.data.entity.WorkoutRoutineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: Long): ExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllExercises(exercises: List<ExerciseEntity>)

    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteExerciseById(id: Long)

    @Query("DELETE FROM exercises")
    suspend fun deleteAllExercises()

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    suspend fun getAllExercisesList(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE muscleGroup = :muscleGroup ORDER BY name ASC")
    fun getExercisesByMuscleGroup(muscleGroup: String): Flow<List<ExerciseEntity>>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun countExercises(): Int
}

@Dao
interface WorkoutRoutineDao {
    @Query("SELECT * FROM workout_routines ORDER BY id DESC")
    fun getAllRoutines(): Flow<List<WorkoutRoutineEntity>>

    @Query("SELECT * FROM workout_routines ORDER BY id DESC")
    suspend fun getAllRoutinesList(): List<WorkoutRoutineEntity>

    @Query("SELECT * FROM workout_routines WHERE id = :id")
    suspend fun getRoutineById(id: Long): WorkoutRoutineEntity?

    @Query("SELECT * FROM workout_routines WHERE id = :id")
    fun getRoutineByIdFlow(id: Long): Flow<WorkoutRoutineEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: WorkoutRoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: WorkoutRoutineEntity)

    @Delete
    suspend fun deleteRoutine(routine: WorkoutRoutineEntity)

    @Query("DELETE FROM workout_routines WHERE id = :routineId")
    suspend fun deleteRoutineById(routineId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExercises(routineExercises: List<RoutineExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExercise(routineExercise: RoutineExerciseEntity)

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteRoutineExercises(routineId: Long)

    @Query("""
        SELECT 
            re.routineId AS routineId,
            re.exerciseId AS exerciseId,
            e.name AS exerciseName,
            e.muscleGroup AS muscleGroup,
            re.targetSets AS targetSets,
            re.restSeconds AS restSeconds,
            re.orderIndex AS orderIndex,
            COALESCE(e.notes, '') AS notes
        FROM routine_exercises re
        INNER JOIN exercises e ON re.exerciseId = e.id
        WHERE re.routineId = :routineId
        ORDER BY re.orderIndex ASC
    """)
    fun getRoutineExercisesWithDetails(routineId: Long): Flow<List<RoutineExerciseWithDetails>>

    @Query("""
        SELECT 
            re.routineId AS routineId,
            re.exerciseId AS exerciseId,
            e.name AS exerciseName,
            e.muscleGroup AS muscleGroup,
            re.targetSets AS targetSets,
            re.restSeconds AS restSeconds,
            re.orderIndex AS orderIndex,
            COALESCE(e.notes, '') AS notes
        FROM routine_exercises re
        INNER JOIN exercises e ON re.exerciseId = e.id
        WHERE re.routineId = :routineId
        ORDER BY re.orderIndex ASC
    """)
    suspend fun getRoutineExercisesList(routineId: Long): List<RoutineExerciseWithDetails>
}

@Dao
interface WorkoutLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutLog(workoutLog: WorkoutLogEntity): Long

    @Update
    suspend fun updateWorkoutLog(workoutLog: WorkoutLogEntity)

    @Query("SELECT * FROM workout_logs ORDER BY timestamp DESC")
    fun getAllWorkoutLogs(): Flow<List<WorkoutLogEntity>>

    @Query("SELECT * FROM workout_logs WHERE id = :id")
    suspend fun getWorkoutLogById(id: Long): WorkoutLogEntity?

    @Query("SELECT * FROM workout_logs WHERE id = :id")
    fun getWorkoutLogByIdFlow(id: Long): Flow<WorkoutLogEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetLog(setLog: SetLogEntity): Long

    @Query("SELECT * FROM set_logs WHERE workoutLogId = :workoutLogId ORDER BY timestamp ASC")
    fun getSetLogsForWorkout(workoutLogId: Long): Flow<List<SetLogEntity>>

    @Query("SELECT * FROM set_logs WHERE workoutLogId = :workoutLogId ORDER BY timestamp ASC")
    suspend fun getSetLogsListForWorkout(workoutLogId: Long): List<SetLogEntity>

    @Query("SELECT * FROM set_logs WHERE exerciseId = :exerciseId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSetForExercise(exerciseId: Long): SetLogEntity?

    @Query("SELECT * FROM set_logs WHERE exerciseId = :exerciseId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestSetForExerciseFlow(exerciseId: Long): Flow<SetLogEntity?>

    @Query("SELECT * FROM set_logs WHERE exerciseId = :exerciseId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSetsForExercise(exerciseId: Long, limit: Int = 5): List<SetLogEntity>

    @Query("SELECT * FROM set_logs WHERE exerciseId = :exerciseId ORDER BY timestamp ASC")
    fun getAllSetLogsForExerciseFlow(exerciseId: Long): Flow<List<SetLogEntity>>

    @Query("SELECT * FROM set_logs WHERE exerciseId = :exerciseId ORDER BY timestamp ASC")
    suspend fun getAllSetLogsForExercise(exerciseId: Long): List<SetLogEntity>

    @Query("""
        SELECT * FROM set_logs 
        WHERE exerciseId = :exerciseId AND workoutLogId != :currentWorkoutLogId
        AND workoutLogId = (
            SELECT workoutLogId FROM set_logs 
            WHERE exerciseId = :exerciseId AND workoutLogId != :currentWorkoutLogId 
            ORDER BY timestamp DESC LIMIT 1
        )
        ORDER BY setNumber ASC, timestamp ASC
    """)
    suspend fun getPreviousWorkoutSetsForExercise(exerciseId: Long, currentWorkoutLogId: Long): List<SetLogEntity>

    @Query("SELECT COALESCE(SUM(weightKg * reps), 0.0) FROM set_logs")
    fun getTotalVolumeFlow(): Flow<Double>

    @Query("SELECT COALESCE(SUM(weightKg * reps), 0.0) FROM set_logs WHERE workoutLogId = :workoutLogId")
    suspend fun getVolumeForWorkout(workoutLogId: Long): Double

    @Query("SELECT COUNT(*) FROM workout_logs")
    fun getTotalWorkoutsCount(): Flow<Int>

    @Query("DELETE FROM workout_logs WHERE id = :id")
    suspend fun deleteWorkoutLogById(id: Long)

    @Query("DELETE FROM workout_logs")
    suspend fun deleteAllWorkoutLogs()

    @Query("DELETE FROM set_logs WHERE workoutLogId = :workoutLogId")
    suspend fun deleteSetLogsForWorkout(workoutLogId: Long)

    @Query("DELETE FROM set_logs")
    suspend fun deleteAllSetLogs()

    @Query("""
        SELECT 
            sl.id AS setLogId,
            sl.workoutLogId AS workoutLogId,
            sl.exerciseId AS exerciseId,
            e.name AS exerciseName,
            e.muscleGroup AS muscleGroup,
            sl.setNumber AS setNumber,
            sl.weightKg AS weightKg,
            sl.reps AS reps,
            sl.timestamp AS timestamp
        FROM set_logs sl
        INNER JOIN exercises e ON sl.exerciseId = e.id
        WHERE sl.workoutLogId = :workoutLogId
        ORDER BY sl.setNumber ASC, sl.timestamp ASC
    """)
    suspend fun getDetailedSetLogsForWorkout(workoutLogId: Long): List<DetailedSetLogModel>
}

@Dao
interface BodyMetricDao {
    @Query("SELECT * FROM body_metrics ORDER BY timestamp DESC")
    fun getAllMetrics(): Flow<List<BodyMetricEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetric(metric: BodyMetricEntity): Long

    @Delete
    suspend fun deleteMetric(metric: BodyMetricEntity)

    @Query("DELETE FROM body_metrics WHERE id = :id")
    suspend fun deleteMetricById(id: Long)

    @Query("DELETE FROM body_metrics")
    suspend fun deleteAllMetrics()

    @Query("SELECT * FROM body_metrics ORDER BY timestamp ASC LIMIT 1")
    fun getFirstMetricFlow(): Flow<BodyMetricEntity?>

    @Query("SELECT * FROM body_metrics ORDER BY timestamp ASC LIMIT 1")
    suspend fun getFirstMetric(): BodyMetricEntity?

    @Query("SELECT * FROM body_metrics ORDER BY timestamp DESC LIMIT 1")
    fun getLatestMetricFlow(): Flow<BodyMetricEntity?>

    @Query("SELECT * FROM body_metrics ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMetric(): BodyMetricEntity?
}
