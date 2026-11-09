package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

data class MenuItem(
    val id: Int,
    val name: String,
    val description: String,
    val price: Double
)

data class Restaurant(
    val id: Int,
    val name: String,
    val category: String,
    val rating: Double,
    val distanceKm: Double,
    val deliveryTimeMin: Int,
    val deliveryFee: Double,
    val items: List<MenuItem>,
    val latitude: Double = 21.0285,
    val longitude: Double = 105.8542
)

data class CartItem(
    val menuItem: MenuItem,
    val quantity: Int
)

data class VehicleType(
    val code: String,
    val name: String,
    val basePrice: Double,
    val estimatedMinutes: Int,
    val iconType: String // "bike", "car_4", "car_7", "premium"
)

sealed interface RideState {
    object Idle : RideState
    data class DetailsEntered(
        val pickup: String,
        val destination: String,
        val selectedVehicle: VehicleType,
        val price: Double,
        val distanceKm: Double
    ) : RideState
    data class Searching(
        val pickup: String,
        val destination: String,
        val selectedVehicle: VehicleType,
        val price: Double
    ) : RideState
    data class Arriving(
        val pickup: String,
        val destination: String,
        val selectedVehicle: VehicleType,
        val price: Double,
        val driverName: String,
        val driverPlate: String,
        val progress: Float // 0f .. 1f
    ) : RideState
    data class TripInProgress(
        val pickup: String,
        val destination: String,
        val selectedVehicle: VehicleType,
        val price: Double,
        val driverName: String,
        val driverPlate: String,
        val progress: Float // 0f .. 1f
    ) : RideState
    data class Completed(
        val ride: RideHistory
    ) : RideState
}

sealed interface FoodState {
    object Idle : FoodState
    data class Preparing(
        val restaurantName: String,
        val itemsSummary: String,
        val price: Double,
        val timerSeconds: Int
    ) : FoodState
    data class Delivering(
        val restaurantName: String,
        val itemsSummary: String,
        val price: Double,
        val driverName: String,
        val progress: Float
    ) : FoodState
    data class Delivered(
        val order: FoodOrder
    ) : FoodState
}

class GrabViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = HistoryRepository(db.historyDao())

    // Observables from Database
    val rideHistory: StateFlow<List<RideHistory>> = repository.allRides
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val foodOrders: StateFlow<List<FoodOrder>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedAddresses: StateFlow<List<SavedAddress>> = repository.allSavedAddresses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val paymentGateway = PaymentGatewayManager()

    val allPaymentMethods: StateFlow<List<LinkedPaymentMethod>> = repository.allPaymentMethods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<PaymentTransaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPaymentMethod = MutableStateFlow<LinkedPaymentMethod?>(null)
    val selectedPaymentMethod: StateFlow<LinkedPaymentMethod?> = _selectedPaymentMethod.asStateFlow()

    // UI state states
    private val _rideState = MutableStateFlow<RideState>(RideState.Idle)
    val rideState: StateFlow<RideState> = _rideState.asStateFlow()

    private val _foodState = MutableStateFlow<FoodState>(FoodState.Idle)
    val foodState: StateFlow<FoodState> = _foodState.asStateFlow()

    // Cart state
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    val locationHelper = LocationHelper(application)

    // Current device coordinates & tracking
    private val _currentLocation = MutableStateFlow<UserLocation?>(null)
    val currentLocation: StateFlow<UserLocation?> = _currentLocation.asStateFlow()

    private val _isLocationTracking = MutableStateFlow(false)
    val isLocationTracking: StateFlow<Boolean> = _isLocationTracking.asStateFlow()

    // Interactive address inputs for ride and food delivery
    val pickupAddress = MutableStateFlow("Vị trí của bạn (32 Đường Láng, Hà Nội)")
    val destinationAddress = MutableStateFlow("")
    val deliveryAddress = MutableStateFlow("Vị trí giao hàng (32 Đường Láng, Hà Nội)")

    // Selected items for food UI
    val selectedCategory = MutableStateFlow("Tất cả")
    val foodSearchQuery = MutableStateFlow("")

    // Simulated balance
    private val _walletBalance = MutableStateFlow(250000.0) // 250,000 VND
    val walletBalance: StateFlow<Double> = _walletBalance.asStateFlow()

    // Static Vietnamese mock configurations
    val categories = listOf("Tất cả", "Phở & Bún", "Cơm & Xôi", "Trà sữa", "Ăn vặt")

    val vehicleTypes = listOf(
        VehicleType("bike", "GrabBike", 12000.0, 6, "bike"),
        VehicleType("car_4", "GrabCar 4 chỗ", 28000.0, 9, "car_4"),
        VehicleType("car_7", "GrabCar 7 chỗ", 36000.0, 11, "car_7"),
        VehicleType("premium", "GrabPremium", 55000.0, 8, "premium")
    )

    val mockRestaurants = listOf(
        Restaurant(
            id = 1,
            name = "Phở Thìn Bờ Hồ - Lò Đúc",
            category = "Phở & Bún",
            rating = 4.8,
            distanceKm = 1.2,
            deliveryTimeMin = 15,
            deliveryFee = 16000.0,
            latitude = 21.0185,
            longitude = 105.8562,
            items = listOf(
                MenuItem(101, "Phở Tái Lăn", "Nước béo ngậy gừng tỏi với thịt bò xào tái lăn", 65000.0),
                MenuItem(102, "Phở Bò Chín nạm", "Thịt chín mềm ngọt lịm đậm vị Hà Nội xưa", 55000.0),
                MenuItem(103, "Quẩy nóng giòn (cặp)", "Quẩy ăn kèm nước phở siêu hợp", 8000.0)
            )
        ),
        Restaurant(
            id = 2,
            name = "Bún Chả Sinh Từ - Bà Triệu",
            category = "Phở & Bún",
            rating = 4.7,
            distanceKm = 2.4,
            deliveryTimeMin = 20,
            deliveryFee = 20000.0,
            latitude = 21.0210,
            longitude = 105.8505,
            items = listOf(
                MenuItem(201, "Suất Bún Chả đặc biệt", "Nước chấm đậm đà, chả viên thơm lừng xá xíu tươi kẹp dưa góp", 62000.0),
                MenuItem(202, "Nem Cua Bể giòn", "Nem nhân béo ngậy bọc cua biển bánh đa chiên ròn rụm", 15000.0)
            )
        ),
        Restaurant(
            id = 3,
            name = "Trà Sữa Phê La - Cầu Giấy",
            category = "Trà sữa",
            rating = 4.9,
            distanceKm = 0.9,
            deliveryTimeMin = 10,
            deliveryFee = 12000.0,
            latitude = 21.0335,
            longitude = 105.7950,
            items = listOf(
                MenuItem(301, "Trà Ô Long Sữa Phê La", "Vị trà đậm hương sữa béo dịu thơm lừng đậm đà", 55000.0),
                MenuItem(302, "Trà Phù Vân Khói", "Hương khói tinh tế hoà quyện kem mặn sánh đặc", 65000.0),
                MenuItem(303, "Trân Châu Trà Ô Long", "Trân châu dẻo dai làm thủ công từ lá trà nhai vui miệng", 10000.0)
            )
        ),
        Restaurant(
            id = 4,
            name = "Bánh Mì Huỳnh Hoa - Sài Gòn",
            category = "Ăn vặt",
            rating = 4.9,
            distanceKm = 3.5,
            deliveryTimeMin = 25,
            deliveryFee = 28000.0,
            latitude = 21.0250,
            longitude = 105.8450,
            items = listOf(
                MenuItem(401, "Bánh Mì Ruốc Pate Đặc Biệt", "Kẹp 5 loại chả tươi, pate béo mềm thần thánh", 58000.0),
                MenuItem(402, "Xôi mặn Phúc Kiến", "Thịt lợn nướng, lạp xưởng chiên bốc khói nghi ngút", 42000.0)
            )
        ),
        Restaurant(
            id = 5,
            name = "Cơm Tấm Phúc Lộc Thọ - Láng",
            category = "Cơm & Xôi",
            rating = 4.6,
            distanceKm = 1.7,
            deliveryTimeMin = 18,
            deliveryFee = 15000.0,
            latitude = 21.0120,
            longitude = 105.8150,
            items = listOf(
                MenuItem(501, "Cơm Sườn Cọng Nướng Kim Chi", "Sườn nướng mật ong vàng ruộm kèm cơm tấm xốp dẻo mỡ hành", 55000.0),
                MenuItem(502, "Cơm Ba Chỉ Sốt Trứng Muối", "Thịt ba chỉ chiên kẹp sốt trứng muối mặn bùi ngon khó cưỡng", 49000.0)
            )
        )
    )

    private val drivers = listOf("Nguyễn Văn Tự", "Phạm Hoàng Nam", "Lê Minh Vương", "Đặng Thanh Lâm", "Trần Công Phượng")
    private val plates = listOf("29A-888.88", "30H-112.45", "51K-999.01", "43C-334.78", "75B-168.68")

    private var simulationJob: Job? = null

    init {
        // Pre-populate database with default addresses if empty
        viewModelScope.launch {
            savedAddresses.first().let { current ->
                if (current.isEmpty()) {
                    repository.insertSavedAddress(SavedAddress(label = "Nhà riêng", address = "32 Đường Láng, Hà Nội"))
                    repository.insertSavedAddress(SavedAddress(label = "Công ty", address = "Lotte Mall Tây Hồ, Hà Nội"))
                    repository.insertSavedAddress(SavedAddress(label = "Trường học", address = "Đại học Bách Khoa Hà Nội"))
                }
            }
        }

        // Pre-populate default payment methods if empty
        viewModelScope.launch {
            allPaymentMethods.first().let { current ->
                if (current.isEmpty()) {
                    repository.insertPaymentMethod(
                        LinkedPaymentMethod(
                            type = "E_WALLET",
                            title = "Ví GrabPay",
                            subtitle = "Số dư ví khả dụng",
                            provider = "GRABPAY",
                            isDefault = true
                        )
                    )
                    repository.insertPaymentMethod(
                        LinkedPaymentMethod(
                            type = "CREDIT_CARD",
                            title = "Visa •••• 4242",
                            subtitle = "Hết hạn: 12/28 (Stripe)",
                            provider = "STRIPE_VISA",
                            lastFour = "4242",
                            cardHolder = "NGUYEN VAN A",
                            token = "pm_stripe_tok_4242"
                        )
                    )
                    repository.insertPaymentMethod(
                        LinkedPaymentMethod(
                            type = "E_WALLET",
                            title = "Ví MoMo",
                            subtitle = "Liên kết: 0988***888",
                            provider = "MOMO",
                            lastFour = "8888",
                            cardHolder = "0988888888",
                            token = "ewallet_momo_link"
                        )
                    )
                    repository.insertPaymentMethod(
                        LinkedPaymentMethod(
                            type = "CASH",
                            title = "Tiền mặt (COD)",
                            subtitle = "Thanh toán trực tiếp khi hoàn thành",
                            provider = "CASH"
                        )
                    )
                }
            }
        }

        viewModelScope.launch {
            allPaymentMethods.collect { list ->
                if (_selectedPaymentMethod.value == null && list.isNotEmpty()) {
                    _selectedPaymentMethod.value = list.find { it.isDefault } ?: list.first()
                }
            }
        }
    }

    // Payment Gateway management operations
    fun selectPaymentMethod(method: LinkedPaymentMethod) {
        _selectedPaymentMethod.value = method
    }

    fun setDefaultPaymentMethod(id: Int) {
        viewModelScope.launch {
            repository.setDefaultPaymentMethod(id)
        }
    }

    fun removePaymentMethod(id: Int) {
        viewModelScope.launch {
            repository.deletePaymentMethod(id)
            if (_selectedPaymentMethod.value?.id == id) {
                _selectedPaymentMethod.value = allPaymentMethods.value.firstOrNull { it.id != id }
            }
        }
    }

    fun linkCreditCard(
        cardNumber: String,
        expiry: String,
        cvv: String,
        cardHolder: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = paymentGateway.tokenizeAndLinkCard(cardNumber, expiry, cvv, cardHolder)
            result.onSuccess { method ->
                repository.insertPaymentMethod(method)
                _selectedPaymentMethod.value = method
                onSuccess()
            }.onFailure { err ->
                onError(err.message ?: "Không thể liên kết thẻ")
            }
        }
    }

    fun linkDigitalWallet(
        provider: String,
        phoneNumber: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = paymentGateway.linkDigitalWallet(provider, phoneNumber)
            result.onSuccess { method ->
                repository.insertPaymentMethod(method)
                _selectedPaymentMethod.value = method
                onSuccess()
            }.onFailure { err ->
                onError(err.message ?: "Không thể liên kết ví")
            }
        }
    }

    // Wallet simulation
    fun topUpWallet(amount: Double, paymentMethodTitle: String = "Thẻ Visa / Stripe Gateway") {
        _walletBalance.value += amount
        viewModelScope.launch {
            repository.insertTransaction(
                PaymentTransaction(
                    transactionId = "TXN-TOP-${System.currentTimeMillis().toString().takeLast(6)}",
                    title = "Nạp tiền ví GrabPay",
                    amount = amount,
                    paymentMethodTitle = paymentMethodTitle,
                    status = "Thành công",
                    serviceType = "TOPUP"
                )
            )
        }
    }

    fun spendWallet(amount: Double): Boolean {
        if (_walletBalance.value >= amount) {
            _walletBalance.value -= amount
            return true
        }
        return false
    }

    // Location operations using Google Play Services
    fun hasLocationPermission(): Boolean = locationHelper.hasLocationPermission()

    fun fetchCurrentLocation(
        updatePickup: Boolean = true,
        updateDelivery: Boolean = true,
        onComplete: ((UserLocation) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val loc = locationHelper.getCurrentLocation()
            _currentLocation.value = loc
            if (updatePickup) {
                pickupAddress.value = "Vị trí của bạn (${loc.address})"
            }
            if (updateDelivery) {
                deliveryAddress.value = "Giao đến: ${loc.address}"
            }
            onComplete?.invoke(loc)
        }
    }

    fun startTrackingCoordinates() {
        if (_isLocationTracking.value) return
        _isLocationTracking.value = true
        viewModelScope.launch {
            locationHelper.getLocationUpdates(4000L).collect { loc ->
                _currentLocation.value = loc
            }
        }
    }

    // Dynamic distance and fee calculated from device coordinates
    fun getRestaurantDistanceKm(restaurant: Restaurant): Double {
        val userLoc = _currentLocation.value
        return if (userLoc != null) {
            val dist = locationHelper.calculateDistanceKm(
                userLoc.latitude, userLoc.longitude,
                restaurant.latitude, restaurant.longitude
            )
            Math.round(dist * 10.0) / 10.0
        } else {
            restaurant.distanceKm
        }
    }

    fun getRestaurantDeliveryFee(restaurant: Restaurant): Double {
        val dist = getRestaurantDistanceKm(restaurant)
        val fee = 12000.0 + (dist * 3500.0)
        return Math.round(fee / 1000.0) * 1000.0
    }

    // Ride booking flow operations
    fun selectDestinations(pickup: String, dest: String) {
        pickupAddress.value = pickup
        destinationAddress.value = dest
        calculateInitialRideState(pickup, dest, vehicleTypes.first())
    }

    fun selectVehicleForBooking(vehicle: VehicleType) {
        val current = _rideState.value
        if (current is RideState.DetailsEntered) {
            val dist = current.distanceKm
            val newPrice = vehicle.basePrice + (dist * 11000.0)
            _rideState.value = RideState.DetailsEntered(
                pickup = current.pickup,
                destination = current.destination,
                selectedVehicle = vehicle,
                price = newPrice,
                distanceKm = dist
            )
        }
    }

    private fun calculateInitialRideState(pickup: String, dest: String, vehicle: VehicleType) {
        if (pickup.isBlank() || dest.isBlank()) return
        // Generate pseudo distance based on string length
        val distance = ((pickup.length + dest.length) % 8) + 2.4
        val price = vehicle.basePrice + (distance * 11000.0) // 11,000 VND per km
        _rideState.value = RideState.DetailsEntered(
            pickup = pickup,
            destination = dest,
            selectedVehicle = vehicle,
            price = price,
            distanceKm = distance
        )
    }

    fun startRideBooking() {
        val current = _rideState.value
        if (current is RideState.DetailsEntered) {
            simulationJob?.cancel()
            simulationJob = viewModelScope.launch {
                // Step 1: Searching for driver
                _rideState.value = RideState.Searching(
                    pickup = current.pickup,
                    destination = current.destination,
                    selectedVehicle = current.selectedVehicle,
                    price = current.price
                )
                delay(3000) // 3 seconds search

                // Pick randomized driver
                val dIndex = Random.nextInt(drivers.size)
                val drName = drivers[dIndex]
                val drPlate = plates[dIndex]

                // Step 2: Driver Arriving (simulation ticks)
                var arrivalProgress = 0f
                while (arrivalProgress < 1.0f) {
                    _rideState.value = RideState.Arriving(
                        pickup = current.pickup,
                        destination = current.destination,
                        selectedVehicle = current.selectedVehicle,
                        price = current.price,
                        driverName = drName,
                        driverPlate = drPlate,
                        progress = arrivalProgress
                    )
                    delay(500)
                    arrivalProgress += 0.2f
                }

                // Step 3: Trip In Progress (ticks)
                var tripProgress = 0f
                val activeMethod = _selectedPaymentMethod.value
                    ?: allPaymentMethods.value.firstOrNull { it.isDefault }
                    ?: allPaymentMethods.value.firstOrNull()

                if (activeMethod != null) {
                    if (activeMethod.provider == "GRABPAY") {
                        spendWallet(current.price)
                    }
                    val txnTitle = "Chuyến đi ${current.selectedVehicle.name}"
                    val chargeRes = paymentGateway.charge(current.price, activeMethod, txnTitle, "RIDE")
                    chargeRes.onSuccess { txn ->
                        repository.insertTransaction(txn)
                    }
                }
                while (tripProgress < 1.0f) {
                    _rideState.value = RideState.TripInProgress(
                        pickup = current.pickup,
                        destination = current.destination,
                        selectedVehicle = current.selectedVehicle,
                        price = current.price,
                        driverName = drName,
                        driverPlate = drPlate,
                        progress = tripProgress
                    )
                    delay(500)
                    tripProgress += 0.2f
                }

                // Step 4: Finished! Persist inside Database
                val finalRide = RideHistory(
                    pickup = current.pickup,
                    destination = current.destination,
                    vehicleType = current.selectedVehicle.name,
                    price = current.price,
                    status = "Hoàn thành",
                    driverName = drName,
                    driverPlate = drPlate
                )
                repository.insertRide(finalRide)
                _rideState.value = RideState.Completed(finalRide)

                // Back to idle after 3 seconds
                delay(3000)
                _rideState.value = RideState.Idle
                destinationAddress.value = "" // clear destination focus
            }
        }
    }

    fun cancelActiveRide() {
        simulationJob?.cancel()
        _rideState.value = RideState.Idle
    }

    // Food Cart operations
    fun addToCart(item: MenuItem) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.menuItem.id == item.id }
        if (index != -1) {
            currentList[index] = currentList[index].copy(quantity = currentList[index].quantity + 1)
        } else {
            currentList.add(CartItem(item, 1))
        }
        _cart.value = currentList
    }

    fun updateCartQuantity(item: MenuItem, newQuantity: Int) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.menuItem.id == item.id }
        if (index != -1) {
            if (newQuantity <= 0) {
                currentList.removeAt(index)
            } else {
                currentList[index] = currentList[index].copy(quantity = newQuantity)
            }
            _cart.value = currentList
        }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    // Food Order flow operations
    fun checkoutFood(restaurantName: String, deliveryFee: Double) {
        val cartItems = _cart.value
        if (cartItems.isEmpty()) return
        val itemsSummary = cartItems.joinToString(", ") { "${it.quantity}x ${it.menuItem.name}" }
        val priceSubtotal = cartItems.sumOf { it.menuItem.price * it.quantity }
        val totalPrice = priceSubtotal + deliveryFee

        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            _cart.value = emptyList() // Clear cart immediately

            // Step 1: Restaurant Preparing
            _foodState.value = FoodState.Preparing(
                restaurantName = restaurantName,
                itemsSummary = itemsSummary,
                price = totalPrice,
                timerSeconds = 5
            )
            for (sec in 4 downTo 1) {
                delay(1000)
                val current = _foodState.value
                if (current is FoodState.Preparing) {
                    _foodState.value = current.copy(timerSeconds = sec)
                }
            }

            // Pick driver for delivery
            val dIndex = Random.nextInt(drivers.size)
            val driverName = drivers[dIndex]

            // Step 2: Delivering
            var progress = 0.0f
            val activeMethod = _selectedPaymentMethod.value
                ?: allPaymentMethods.value.firstOrNull { it.isDefault }
                ?: allPaymentMethods.value.firstOrNull()

            if (activeMethod != null) {
                if (activeMethod.provider == "GRABPAY") {
                    spendWallet(totalPrice)
                }
                val txnTitle = "Đơn món ăn: $restaurantName"
                val chargeRes = paymentGateway.charge(totalPrice, activeMethod, txnTitle, "FOOD")
                chargeRes.onSuccess { txn ->
                    repository.insertTransaction(txn)
                }
            }
            while (progress <= 1.0f) {
                _foodState.value = FoodState.Delivering(
                    restaurantName = restaurantName,
                    itemsSummary = itemsSummary,
                    price = totalPrice,
                    driverName = driverName,
                    progress = progress
                )
                delay(800)
                progress += 0.2f
            }

            // Step 3: Complete & save to DB
            val savedOrder = FoodOrder(
                restaurantName = restaurantName,
                itemsSummary = itemsSummary,
                totalPrice = totalPrice,
                status = "Đã giao hàng",
                driverName = driverName
            )
            repository.insertOrder(savedOrder)
            _foodState.value = FoodState.Delivered(savedOrder)

            delay(3500)
            _foodState.value = FoodState.Idle
        }
    }

    fun cancelFoodOrder() {
        simulationJob?.cancel()
        _foodState.value = FoodState.Idle
    }

    // Add normal Saved locations
    fun addAddress(label: String, address: String) {
        viewModelScope.launch {
            repository.insertSavedAddress(SavedAddress(label = label, address = address))
        }
    }

    fun removeAddress(id: Int) {
        viewModelScope.launch {
            repository.deleteSavedAddress(id)
        }
    }
}
