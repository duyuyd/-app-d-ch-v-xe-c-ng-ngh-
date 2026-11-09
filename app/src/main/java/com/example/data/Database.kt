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

@Entity(tableName = "linked_payment_methods")
data class LinkedPaymentMethod(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "CREDIT_CARD", "E_WALLET", "CASH"
    val title: String,
    val subtitle: String,
    val provider: String, // "STRIPE_VISA", "STRIPE_MASTERCARD", "MOMO", "ZALOPAY", "VNPAY", "GRABPAY", "CASH"
    val lastFour: String = "",
    val cardHolder: String = "",
    val isDefault: Boolean = false,
    val token: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "payment_transactions")
data class PaymentTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val transactionId: String,
    val title: String,
    val amount: Double,
    val paymentMethodTitle: String,
    val status: String, // "Thành công", "Đang xử lý"
    val serviceType: String, // "RIDE", "FOOD", "TOPUP"
    val timestamp: Long = System.currentTimeMillis()
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

    // Linked Payment Methods
    @Query("SELECT * FROM linked_payment_methods ORDER BY isDefault DESC, timestamp DESC")
    fun getAllPaymentMethods(): Flow<List<LinkedPaymentMethod>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentMethod(method: LinkedPaymentMethod): Long

    @Query("DELETE FROM linked_payment_methods WHERE id = :id")
    suspend fun deletePaymentMethod(id: Int)

    @Query("UPDATE linked_payment_methods SET isDefault = 0")
    suspend fun clearDefaultPaymentMethods()

    @Query("UPDATE linked_payment_methods SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultPaymentMethod(id: Int)

    // Payment Transactions
    @Query("SELECT * FROM payment_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<PaymentTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: PaymentTransaction): Long
}

@Database(
    entities = [
        RideHistory::class,
        FoodOrder::class,
        SavedAddress::class,
        LinkedPaymentMethod::class,
        PaymentTransaction::class
    ],
    version = 2,
    exportSchema = false
)
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
    val allPaymentMethods: Flow<List<LinkedPaymentMethod>> = historyDao.getAllPaymentMethods()
    val allTransactions: Flow<List<PaymentTransaction>> = historyDao.getAllTransactions()

    suspend fun insertRide(ride: RideHistory) = historyDao.insertRide(ride)
    suspend fun insertOrder(order: FoodOrder) = historyDao.insertOrder(order)
    suspend fun insertSavedAddress(address: SavedAddress) = historyDao.insertSavedAddress(address)
    suspend fun deleteSavedAddress(id: Int) = historyDao.deleteSavedAddress(id)

    suspend fun insertPaymentMethod(method: LinkedPaymentMethod) = historyDao.insertPaymentMethod(method)
    suspend fun deletePaymentMethod(id: Int) = historyDao.deletePaymentMethod(id)
    suspend fun setDefaultPaymentMethod(id: Int) {
        historyDao.clearDefaultPaymentMethods()
        historyDao.setDefaultPaymentMethod(id)
    }
    suspend fun insertTransaction(transaction: PaymentTransaction) = historyDao.insertTransaction(transaction)
}
