package com.example.schfit.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.schfit.data.dao.BodyMetricDao
import com.example.schfit.data.dao.ExerciseDao
import com.example.schfit.data.dao.WorkoutLogDao
import com.example.schfit.data.dao.WorkoutRoutineDao
import com.example.schfit.data.entity.BodyMetricEntity
import com.example.schfit.data.entity.ExerciseEntity
import com.example.schfit.data.entity.RoutineExerciseEntity
import com.example.schfit.data.entity.SetLogEntity
import com.example.schfit.data.entity.WorkoutLogEntity
import com.example.schfit.data.entity.WorkoutRoutineEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutRoutineEntity::class,
        RoutineExerciseEntity::class,
        WorkoutLogEntity::class,
        SetLogEntity::class,
        BodyMetricEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutRoutineDao(): WorkoutRoutineDao
    abstract fun workoutLogDao(): WorkoutLogDao
    abstract fun bodyMetricDao(): BodyMetricDao

    companion object {
        private val instances = ConcurrentHashMap<String, AppDatabase>()

        val DEFAULT_EXERCISES = listOf(
            // SIRT
            ExerciseEntity(name = "Lat Pull Down Machine", muscleGroup = "Sırt", isCustom = false),
            ExerciseEntity(name = "Low Row", muscleGroup = "Sırt", isCustom = false),
            ExerciseEntity(name = "Deadlift", muscleGroup = "Sırt", isCustom = false),

            // GÖĞÜS
            ExerciseEntity(name = "Pec Dec Fly", muscleGroup = "Göğüs", isCustom = false),
            ExerciseEntity(name = "Chest Press", muscleGroup = "Göğüs", isCustom = false),
            ExerciseEntity(name = "Bench Press", muscleGroup = "Göğüs", isCustom = false),

            // OMUZ
            ExerciseEntity(name = "Shoulder Press", muscleGroup = "Omuz", isCustom = false),
            ExerciseEntity(name = "L raise", muscleGroup = "Omuz", isCustom = false),

            // BICEPS
            ExerciseEntity(name = "Biceps Curl", muscleGroup = "Biceps", isCustom = false),
            ExerciseEntity(name = "Hammer Curl", muscleGroup = "Biceps", isCustom = false),

            // TRICEPS
            ExerciseEntity(name = "Triceps Extension", muscleGroup = "Triceps", isCustom = false),
            ExerciseEntity(name = "Triceps Machine", muscleGroup = "Triceps", isCustom = false),

            // KARIN
            ExerciseEntity(name = "Crunch", muscleGroup = "Karın", isCustom = false),

            // BACAK
            ExerciseEntity(name = "Leg Press", muscleGroup = "Bacak", isCustom = false),
            ExerciseEntity(name = "Leg Curl", muscleGroup = "Bacak", isCustom = false),
            ExerciseEntity(name = "Leg Extension", muscleGroup = "Bacak", isCustom = false),
            ExerciseEntity(name = "Squad", muscleGroup = "Bacak", isCustom = false)
        )

        fun getDatabase(
            context: Context,
            configId: String = "default",
            scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
        ): AppDatabase {
            val sanitized = if (configId.isBlank() || configId == "default") "default" else configId.trim().lowercase().replace(Regex("[^a-z0-9_]"), "_")
            return instances.computeIfAbsent(sanitized) { key ->
                val dbName = if (key == "default") "schfit_database" else "schfit_database_$key"
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    dbName
                )
                    .addCallback(AppDatabaseCallback(scope, key))
                    .fallbackToDestructiveMigration()
                    .build()
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope,
            private val configKey: String
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                scope.launch(Dispatchers.IO) {
                    val database = instances[configKey]
                    database?.let {
                        it.exerciseDao().insertAllExercises(DEFAULT_EXERCISES)
                    }
                }
            }
        }
    }
}
