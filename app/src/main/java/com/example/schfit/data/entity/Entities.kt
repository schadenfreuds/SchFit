package com.example.schfit.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val muscleGroup: String,
    val isCustom: Boolean = false,
    val notes: String = ""
)

@Entity(tableName = "workout_routines")
data class WorkoutRoutineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "routine_exercises",
    primaryKeys = ["routineId", "exerciseId"],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutRoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["routineId"]),
        Index(value = ["exerciseId"])
    ]
)
data class RoutineExerciseEntity(
    val routineId: Long,
    val exerciseId: Long,
    val targetSets: Int,
    val restSeconds: Int,
    val orderIndex: Int
)

@Entity(
    tableName = "workout_logs",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutRoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["routineId"])
    ]
)
data class WorkoutLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routineId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 0
)

@Entity(
    tableName = "set_logs",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutLogId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["workoutLogId"]),
        Index(value = ["exerciseId"])
    ]
)
data class SetLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutLogId: Long,
    val exerciseId: Long,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "body_metrics")
data class BodyMetricEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val weightKg: Double,
    val timestamp: Long = System.currentTimeMillis()
)

data class RoutineExerciseWithDetails(
    val routineId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val muscleGroup: String,
    val targetSets: Int,
    val restSeconds: Int,
    val orderIndex: Int,
    val notes: String = ""
)

data class RoutineWithExercises(
    val routine: WorkoutRoutineEntity,
    val exercises: List<RoutineExerciseWithDetails>
)

data class DetailedSetLogModel(
    val setLogId: Long,
    val workoutLogId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val muscleGroup: String,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val timestamp: Long
)
