package com.example

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "calculation_history")
data class CalculationHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface CalculationDao {
    @Query("SELECT * FROM calculation_history ORDER BY timestamp DESC")
    fun getAllCalculations(): Flow<List<CalculationHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalculation(calc: CalculationHistory)

    @Query("DELETE FROM calculation_history")
    suspend fun clearAllCalculations()

    @Query("DELETE FROM calculation_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Int)
}

@Database(entities = [CalculationHistory::class], version = 1, exportSchema = false)
abstract class CalculationDatabase : RoomDatabase() {
    abstract fun calculationDao(): CalculationDao

    companion object {
        @Volatile
        private var INSTANCE: CalculationDatabase? = null

        fun getDatabase(context: Context): CalculationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CalculationDatabase::class.java,
                    "calculation_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class CalculationRepository(private val dao: CalculationDao) {
    val allCalculations: Flow<List<CalculationHistory>> = dao.getAllCalculations()

    suspend fun insert(calc: CalculationHistory) {
        dao.insertCalculation(calc)
    }

    suspend fun clearAll() {
        dao.clearAllCalculations()
    }

    suspend fun deleteById(id: Int) {
        dao.deleteHistoryById(id)
    }
}
