package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "ride_history")
data class RideHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pickup: String,
    val destination: String,
    val vehicleType: String,
    val price: Double,
    val status: String,
    val timestamp: Long = System.currentTimeMillis(),
    val driverName: String,
    val driverPlate: String
)

@Entity(tableName = "food_order")
data class FoodOrder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val restaurantName: String,
    val itemsSummary: String,
    val totalPrice: Double,
    val status: String,
    val timestamp: Long = System.currentTimeMillis(),
    val driverName: String
)

@Entity(tableName = "saved_address")
data class SavedAddress(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val address: String
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM ride_history ORDER BY timestamp DESC")
    fun getAllRides(): Flow<List<RideHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRide(ride: RideHistory)

    @Query("SELECT * FROM food_order ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<FoodOrder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: FoodOrder)

    @Query("SELECT * FROM saved_address")
    fun getAllSavedAddresses(): Flow<List<SavedAddress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedAddress(address: SavedAddress)

    @Query("DELETE FROM saved_address WHERE id = :id")
    suspend fun deleteSavedAddress(id: Int)
}

@Database(entities = [RideHistory::class, FoodOrder::class, SavedAddress::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "grab_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class HistoryRepository(private val historyDao: HistoryDao) {
    val allRides: Flow<List<RideHistory>> = historyDao.getAllRides()
    val allOrders: Flow<List<FoodOrder>> = historyDao.getAllOrders()
    val allSavedAddresses: Flow<List<SavedAddress>> = historyDao.getAllSavedAddresses()

    suspend fun insertRide(ride: RideHistory) = historyDao.insertRide(ride)
    suspend fun insertOrder(order: FoodOrder) = historyDao.insertOrder(order)
    suspend fun insertSavedAddress(address: SavedAddress) = historyDao.insertSavedAddress(address)
    suspend fun deleteSavedAddress(id: Int) = historyDao.deleteSavedAddress(id)
}
