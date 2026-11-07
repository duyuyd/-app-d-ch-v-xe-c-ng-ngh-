package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import com.example.data.UserLocation
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.BorderStroke
import com.example.data.FoodOrder
import com.example.data.RideHistory
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// Helper to format currency VND
fun formatVnd(amount: Double): String {
    return String.format("%,.0f", amount).replace(",", ".") + "đ"
}

// Timestamp format
fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM - HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrabApp(viewModel: GrabViewModel) {
    val navController = rememberNavController()
    val rideState by viewModel.rideState.collectAsStateWithLifecycle()
    val foodState by viewModel.foodState.collectAsStateWithLifecycle()
    val cartItems by viewModel.cart.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        bottomBar = {
            NavigationBar(
                tonalElevation = 8.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

                NavigationBarItem(
                    selected = currentRoute == "home" || currentRoute == null,
                    onClick = { navController.navigate("home") { popUpTo("home") { inclusive = false } } },
                    icon = { Icon(if (currentRoute == "home" || currentRoute == null) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Trang chủ") },
                    label = { Text("Trang chủ", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = GrabGreen, selectedTextColor = GrabGreen)
                )

                NavigationBarItem(
                    selected = currentRoute == "booking",
                    onClick = { navController.navigate("booking") },
                    icon = { Icon(Icons.Filled.DirectionsCar, contentDescription = "Đặt xe") },
                    label = { Text("Đặt xe", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = GrabGreen, selectedTextColor = GrabGreen)
                )

                NavigationBarItem(
                    selected = currentRoute == "food" || currentRoute == "cart",
                    onClick = { navController.navigate("food") },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (cartItems.isNotEmpty()) {
                                    Badge(containerColor = GrabAccentOrange) {
                                        Text("${cartItems.sumOf { it.quantity }}")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.ShoppingCart, contentDescription = "GrabFood")
                        }
                    },
                    label = { Text("Ăn uống", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = GrabGreen, selectedTextColor = GrabGreen)
                )

                NavigationBarItem(
                    selected = currentRoute == "history",
                    onClick = { navController.navigate("history") },
                    icon = { Icon(if (currentRoute == "history") Icons.Filled.History else Icons.Outlined.History, contentDescription = "Lịch sử") },
                    label = { Text("Lịch sử", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = GrabGreen, selectedTextColor = GrabGreen)
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(viewModel, navController)
            }
            composable("booking") {
                BookingScreen(viewModel, navController)
            }
            composable("food") {
                FoodScreen(viewModel, navController)
            }
            composable("cart") {
                CartScreen(viewModel, navController)
            }
            composable("history") {
                HistoryScreen(viewModel)
            }
        }
    }
}

// 1. DASHBOARD SCREEN
@Composable
fun HomeScreen(viewModel: GrabViewModel, navController: NavController) {
    val walletBal by viewModel.walletBalance.collectAsStateWithLifecycle()
    val activeRide by viewModel.rideState.collectAsStateWithLifecycle()
    val activeFood by viewModel.foodState.collectAsStateWithLifecycle()
    val pastRides by viewModel.rideHistory.collectAsStateWithLifecycle()
    val pastOrders by viewModel.foodOrders.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        // TOP GREEN BANNER BLOCK
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(GrabGreen, GrabGreenDark)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
                        val pickupAddress by viewModel.pickupAddress.collectAsStateWithLifecycle()

                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.22f))
                                    .clickable { navController.navigate("booking") }
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.MyLocation,
                                    contentDescription = "GPS",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentLocation?.address ?: pickupAddress,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Xin chào, Bạn khỏe không?",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Person, contentDescription = "Profile", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // WALLET MINI CARD
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wallet_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Wallet,
                                    contentDescription = "Wallet",
                                    tint = GrabGreen,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Ví GrabPay",
                                        fontSize = 12.sp,
                                        color = TextSecondaryLight
                                    )
                                    Text(
                                        text = formatVnd(walletBal),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimaryLight
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.topUpWallet(50000.0) },
                                colors = ButtonDefaults.buttonColors(containerColor = GrabGreenLight),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = "Nạp ví",
                                        tint = GrabGreenDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Nạp 50k",
                                        color = GrabGreenDark,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // TRIGGER BAR FOR RUNNING TRIPS SIMULATORS (REAL-TIME FLOATING INDICATORS)
        item {
            AnimatedVisibility(
                visible = activeRide !is RideState.Idle,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { navController.navigate("booking") },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = GrabGreenLight.copy(alpha = 0.9f)),
                    border = BorderStroke(1.dp, GrabGreen)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.DirectionsCar,
                                contentDescription = "Active ride",
                                tint = GrabGreenDark,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Chuyến đi đang hoạt động",
                                    color = GrabGreenDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = when (val state = activeRide) {
                                        is RideState.Searching -> "Đang tìm tài xế..."
                                        is RideState.Arriving -> "Tài xế đang đến đón"
                                        is RideState.TripInProgress -> "Đang trên hành trình đi"
                                        is RideState.Completed -> "Hành trình hoàn thành!"
                                        else -> "Đang xử lý..."
                                    },
                                    color = TextPrimaryLight,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = "Bấm để xem",
                            tint = GrabGreenDark
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = activeFood !is FoodState.Idle,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { navController.navigate("food") },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    border = BorderStroke(1.dp, GrabAccentOrange)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.LocalDining,
                                contentDescription = "Active food",
                                tint = GrabAccentOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Đơn GrabFood đang giao",
                                    color = GrabAccentOrange,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = when (val state = activeFood) {
                                        is FoodState.Preparing -> "Nhà hàng đang chuẩn bị món ăn..."
                                        is FoodState.Delivering -> "Tài xế đang đi giao hàng"
                                        is FoodState.Delivered -> "Đã giao xong! Chúc ngon miệng"
                                        else -> "Đang chuẩn vị món ăn..."
                                    },
                                    color = TextPrimaryLight,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = "Bấm để xem",
                            tint = GrabAccentOrange
                        )
                    }
                }
            }
        }

        // TWO PRIMARY BOOKING SERVICE CARDS Grid
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                Text(
                    text = "Dịch vụ của chúng tôi",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryLight,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // BOOK RIDE CARD
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                            .testTag("book_ride_button")
                            .clickable { navController.navigate("booking") },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GrabGreenLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.DirectionsCar,
                                    contentDescription = "Đặt Xe",
                                    tint = GrabGreen,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Đặt Xe Ôtô/Bike",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryLight
                                )
                                Text(
                                    text = "Đầy đủ tài xế đưa đón",
                                    fontSize = 11.sp,
                                    color = TextSecondaryLight
                                )
                            }
                        }
                    }

                    // BOOK FOOD CARD
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                            .testTag("order_food_button")
                            .clickable { navController.navigate("food") },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFF3E0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.LocalDining,
                                    contentDescription = "Gọi Món",
                                    tint = GrabAccentOrange,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Giao Đồ Ăn",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryLight
                                )
                                Text(
                                    text = "GrabFood siêu tốc",
                                    fontSize = 11.sp,
                                    color = TextSecondaryLight
                                )
                            }
                        }
                    }
                }
            }
        }

        // PROMOTION BANNER SLIDER
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, start = 16.dp, end = 16.dp)
            ) {
                Text(
                    text = "Mã giảm giá ưu đãi hot 🔥",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryLight,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    item {
                        PromotionBannerCard(
                            themeColor = GrabGreen,
                            title = "Đồng Giá 12k",
                            subtitle = "Mã: GRABBIKE12",
                            desc = "Áp dụng các chuyến GrabBike dưới 3km"
                        )
                    }
                    item {
                        PromotionBannerCard(
                            themeColor = GrabAccentOrange,
                            title = "Miễn Phí Giao Hàng",
                            subtitle = "Mã: FREESHIP",
                            desc = "Freeship mọi đơn GrabFood từ 100k"
                        )
                    }
                    item {
                        PromotionBannerCard(
                            themeColor = GrabGreenDark,
                            title = "Giảm Tới 50k",
                            subtitle = "Mã: GRABCARD50",
                            desc = "Giảm 30% khi thanh toán bằng ví GrabPay"
                        )
                    }
                }
            }
        }

        // RECENT HISTORY LIST FROM COMPONENT DATA DB
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hoạt động gần đây",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryLight
                    )
                    Text(
                        text = "Xem tất cả",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrabGreen,
                        modifier = Modifier.clickable { navController.navigate("history") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (pastRides.isEmpty() && pastOrders.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.DirectionsCar,
                                contentDescription = "Empty icon",
                                tint = TextSecondaryLight.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Chưa có hoạt động nào",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryLight
                            )
                            Text(
                                "Hãy đặt chuyến hoặc gọi món để lưu lại lịch sử nhé!",
                                fontSize = 12.sp,
                                color = TextSecondaryLight,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Show top 3 recent items
                    val recentItems = mutableListOf<Any>()
                    recentItems.addAll(pastRides.take(2))
                    recentItems.addAll(pastOrders.take(2))

                    recentItems.sortByDescending {
                        when (it) {
                            is RideHistory -> it.timestamp
                            is FoodOrder -> it.timestamp
                            else -> 0L
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        recentItems.take(3).forEach { item ->
                            when (item) {
                                is RideHistory -> {
                                    RideHistoryItemRow(item)
                                }
                                is FoodOrder -> {
                                    FoodOrderItemRow(item)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PromotionBannerCard(themeColor: Color, title: String, subtitle: String, desc: String) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = themeColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.White)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(subtitle, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                }
            }
            Text(desc, fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

// Recent ride row view
@Composable
fun RideHistoryItemRow(ride: RideHistory) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GrabGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (ride.vehicleType == "GrabBike") Icons.Filled.Motorcycle else Icons.Filled.DirectionsCar,
                    contentDescription = "Ride icon",
                    tint = GrabGreenDark
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = ride.vehicleType,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimaryLight
                    )
                    Text(
                        text = formatVnd(ride.price),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GrabGreenDark
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Từ: ${ride.pickup}",
                    fontSize = 11.sp,
                    color = TextSecondaryLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Đến: ${ride.destination}",
                    fontSize = 11.sp,
                    color = TextPrimaryLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(ride.driverName + " • " + ride.driverPlate, fontSize = 10.sp, color = TextSecondaryLight)
                    Text(formatTime(ride.timestamp), fontSize = 10.sp, color = TextSecondaryLight)
                }
            }
        }
    }
}

// Recent order row view
@Composable
fun FoodOrderItemRow(order: FoodOrder) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF3E0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.LocalDining,
                    contentDescription = "Food icon",
                    tint = GrabAccentOrange
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = order.restaurantName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimaryLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(0.7f)
                    )
                    Text(
                        text = formatVnd(order.totalPrice),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GrabAccentOrange,
                        modifier = Modifier.weight(0.3f),
                        textAlign = TextAlign.End
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = order.itemsSummary,
                    fontSize = 11.sp,
                    color = TextSecondaryLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Shipper: ${order.driverName} • Giao thành công",
                        fontSize = 10.sp,
                        color = TextSecondaryLight
                    )
                    Text(formatTime(order.timestamp), fontSize = 10.sp, color = TextSecondaryLight)
                }
            }
        }
    }
}


// 2. RIDE BOOKING SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(viewModel: GrabViewModel, navController: NavController) {
    val savedAddresses by viewModel.savedAddresses.collectAsStateWithLifecycle()
    val pickup by viewModel.pickupAddress.collectAsStateWithLifecycle()
    val dest by viewModel.destinationAddress.collectAsStateWithLifecycle()
    val rideState by viewModel.rideState.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val isTracking by viewModel.isLocationTracking.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            viewModel.fetchCurrentLocation()
            viewModel.startTrackingCoordinates()
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.hasLocationPermission()) {
            viewModel.fetchCurrentLocation()
            viewModel.startTrackingCoordinates()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        // TOP ADDRESS FORM PANEL
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Bạn muốn đi bằng gì?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimaryLight,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // PICKUP FIELD WITH GPS BUTTON
                OutlinedTextField(
                    value = pickup,
                    onValueChange = { viewModel.pickupAddress.value = it },
                    label = { Text("Điểm đón") },
                    leadingIcon = { Icon(Icons.Filled.LocationOn, "pickup", tint = GrabGreen) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (viewModel.hasLocationPermission()) {
                                    viewModel.fetchCurrentLocation()
                                    viewModel.startTrackingCoordinates()
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.testTag("get_current_location_button")
                        ) {
                            Icon(
                                Icons.Filled.MyLocation,
                                contentDescription = "Lấy tọa độ GPS",
                                tint = GrabGreenDark
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pickup_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GrabGreen)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // DESTINATION FIELD
                OutlinedTextField(
                    value = dest,
                    onValueChange = {
                        viewModel.destinationAddress.value = it
                        if (it.isNotBlank()) {
                            viewModel.selectDestinations(pickup, it)
                        } else {
                            viewModel.cancelActiveRide()
                        }
                    },
                    label = { Text("Bạn muốn đi đâu?") },
                    leadingIcon = { Icon(Icons.Filled.LocationOn, "destination", tint = Color.Red) },
                    trailingIcon = {
                        if (dest.isNotEmpty()) {
                            IconButton(onClick = {
                                viewModel.destinationAddress.value = ""
                                viewModel.cancelActiveRide()
                            }) {
                                Icon(Icons.Filled.Clear, "clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("destination_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GrabGreen)
                )

                // LIVE GPS COORDINATE DISPLAY CARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            if (viewModel.hasLocationPermission()) {
                                viewModel.fetchCurrentLocation()
                                viewModel.startTrackingCoordinates()
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentLocation != null) GrabGreenLight.copy(alpha = 0.55f) else Color(0xFFF1F5F9)
                    ),
                    border = BorderStroke(1.dp, if (currentLocation != null) GrabGreen.copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(if (currentLocation != null) GrabGreen else TextSecondaryLight.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.GpsFixed,
                                    contentDescription = "GPS",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Google Play Location Tracker",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (currentLocation != null) GrabGreenDark else TextPrimaryLight
                                    )
                                    if (currentLocation != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(GrabGreen)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Live GPS", fontSize = 10.sp, color = GrabGreenDark, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                val locCoord = currentLocation?.let {
                                    String.format(Locale.US, "Tọa độ: %.5f° N, %.5f° E (±%.1fm)", it.latitude, it.longitude, it.accuracy)
                                } ?: "Chạm để tự động cập nhật tọa độ GPS thiết bị"
                                Text(
                                    locCoord,
                                    fontSize = 11.sp,
                                    color = TextSecondaryLight,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                if (viewModel.hasLocationPermission()) {
                                    viewModel.fetchCurrentLocation()
                                    viewModel.startTrackingCoordinates()
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Làm mới vị trí",
                                tint = GrabGreenDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // POPULAR SAVED LOCATIONS PRESET LIST FOR FAST TAP BOOKING
                if (dest.isBlank() && rideState is RideState.Idle) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("📍 Địa điểm yêu thích để đặt nhanh:", fontSize = 12.sp, color = TextSecondaryLight)
                    LazyRow(
                        modifier = Modifier.padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedAddresses) { sa ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GrabGreenLight)
                                    .clickable {
                                        focusManager.clearFocus()
                                        viewModel.selectDestinations(pickup, sa.address)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        when (sa.label) {
                                            "Nhà riêng" -> Icons.Filled.Home
                                            "Công ty" -> Icons.Filled.Work
                                            else -> Icons.Filled.School
                                        },
                                        contentDescription = sa.label,
                                        tint = GrabGreenDark,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(sa.label, fontSize = 12.sp, color = GrabGreenDark, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // MAP CONTAINER AND TICKING SIMULATOR
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Live animated vector road map drawn through Canvas
            InteractiveSimulatedMap(state = rideState, currentLocation = currentLocation)

            // OVERLAY SHEET IN CASE OF ACTIVE RUNNING JOURNEYS
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                when (val state = rideState) {
                    is RideState.Idle -> {
                        // Empty states message
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.9f))
                                .border(1.dp, TextSecondaryLight.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Vui lòng nhập điểm đến để lựa chọn xe cứu hộ / đặt xe Grab cực nhanh!",
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = TextPrimaryLight
                            )
                        }
                    }

                    is RideState.DetailsEntered -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Phương tiện phù hợp",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimaryLight,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                // Horizontal scrolling list of service types
                                LazyColumn(
                                    modifier = Modifier.height(130.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(viewModel.vehicleTypes) { vehicle ->
                                        val isSelected = state.selectedVehicle.code == vehicle.code
                                        val dist = state.distanceKm
                                        val calcPrice = vehicle.basePrice + (dist * 11000.0)

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) GrabGreenLight else Color.Transparent)
                                                .clickable { viewModel.selectVehicleForBooking(vehicle) }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    if (vehicle.code == "bike") Icons.Filled.Motorcycle else Icons.Filled.DirectionsCar,
                                                    contentDescription = null,
                                                    tint = if (isSelected) GrabGreenDark else TextSecondaryLight,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(vehicle.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text("Quãng đường: ${String.format("%.1f", dist)}km • ${vehicle.estimatedMinutes} phút", fontSize = 11.sp, color = TextSecondaryLight)
                                                }
                                            }
                                            Text(formatVnd(calcPrice), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = if (isSelected) GrabGreenDark else TextPrimaryLight)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = { viewModel.startRideBooking() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("confirm_booking_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = GrabGreen),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Xác nhận đặt ${state.selectedVehicle.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    is RideState.Searching -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = GrabGreen,
                                    trackColor = GrabGreenLight
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Đang kết nối với tài xế gần bạn...",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryLight
                                )
                                Text(
                                    "Hệ thống định vị thông minh của Grab đang lọc cuộn xe phù hợp nhất.",
                                    fontSize = 12.sp,
                                    color = TextSecondaryLight,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                TextButton(onClick = { viewModel.cancelActiveRide() }) {
                                    Text("Hủy đặt xe", color = Color.Red, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    is RideState.Arriving -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(GrabGreen),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Filled.Person, null, tint = Color.White)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(state.driverName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.Star, null, tint = GrabAccentYellow, modifier = Modifier.size(12.dp))
                                                Text(" 4.9 • ${state.selectedVehicle.name}", fontSize = 11.sp, color = TextSecondaryLight)
                                            }
                                        }
                                    }
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = GrabGreenLight),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            state.driverPlate,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = GrabGreenDark,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 12.dp))

                                Text(
                                    "Tài xế đang đến đón bạn • Sắp tới nơi",
                                    fontSize = 13.sp,
                                    color = GrabGreenDark,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                LinearProgressIndicator(
                                    progress = { state.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    color = GrabGreen,
                                    trackColor = GrabGreenLight
                                )
                            }
                        }
                    }

                    is RideState.TripInProgress -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Hành trình đang diễn ra 🚀",
                                    fontWeight = FontWeight.Bold,
                                    color = GrabGreenDark,
                                    fontSize = 14.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Điểm đón: ${state.pickup}", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Điểm đến: ${state.destination}", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)

                                LinearProgressIndicator(
                                    progress = { state.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    color = GrabGreen,
                                    trackColor = GrabGreenLight
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Tài xế: ${state.driverName}", fontSize = 11.sp)
                                    Text("Thanh toán: " + formatVnd(state.price), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    is RideState.Completed -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(GrabGreenLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Check, null, tint = GrabGreenDark, modifier = Modifier.size(32.dp))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Hoàn thành chuyến đi!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = GrabGreenDark
                                )
                                Text(
                                    "Bạn đã thanh toán thành công " + formatVnd(state.ride.price) + " bằng ví GrabPay. Cảm ơn đã sử dụng dịch vụ!",
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    color = TextSecondaryLight
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Interactive Map Canvas Component with Google Play Services GPS Location
@Composable
fun InteractiveSimulatedMap(
    state: RideState,
    currentLocation: UserLocation? = null
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE8F2EA))
        ) {
            val width = size.width
            val height = size.height

            // Draw styled road grid lines (Vietnamese typical intersections)
            val roadStroke = Stroke(width = 24f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(60f, 40f), 0f))
            val secondaryRoadStroke = Stroke(width = 12f)

            // Roads
            drawLine(Color(0xFFFFFFFF), Offset(0f, height * 0.3f), Offset(width, height * 0.3f), strokeWidth = 32f)
            drawLine(Color(0xFFE0E0E0), Offset(0f, height * 0.3f), Offset(width, height * 0.3f), strokeWidth = 2f)

            drawLine(Color(0xFFFFFFFF), Offset(0f, height * 0.7f), Offset(width, height * 0.7f), strokeWidth = 32f)

            drawLine(Color(0xFFFFFFFF), Offset(width * 0.25f, 0f), Offset(width * 0.25f, height), strokeWidth = 32f)
            drawLine(Color(0xFFFFFFFF), Offset(width * 0.75f, 0f), Offset(width * 0.75f, height), strokeWidth = 32f)

            // Draw active trip trajectory paths
            val pStart = Offset(width * 0.25f, height * 0.7f) // Pickup position
            val pEnd = Offset(width * 0.75f, height * 0.3f) // Destination position

            if (state !is RideState.Idle) {
                // Draw route line
                drawLine(
                    color = GrabGreen.copy(alpha = 0.6f),
                    start = pStart,
                    end = pEnd,
                    strokeWidth = 10f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                )

                // Draw pickup (Start) dot
                drawCircle(color = GrabGreen, radius = 10f * pulseScale, center = pStart)
                drawCircle(color = Color.White, radius = 4f, center = pStart)

                // Draw dropoff (End) dot
                drawCircle(color = Color(0xFFD32F2F), radius = 10f, center = pEnd)
                drawCircle(color = Color.White, radius = 4f, center = pEnd)

                // INTERPOLATE VEHICLE CAR/BIKE POSITION (Simulation)
                val driverPos = when (state) {
                    is RideState.Arriving -> {
                        // Driver approaching pickup dot
                        val pOutside = Offset(width * 0.1f, height * 0.2f)
                        val dx = pOutside.x + (pStart.x - pOutside.x) * state.progress
                        val dy = pOutside.y + (pStart.y - pOutside.y) * state.progress
                        Offset(dx, dy)
                    }
                    is RideState.TripInProgress -> {
                        // Driver moving from pickup to dropoff
                        val dx = pStart.x + (pEnd.x - pStart.x) * state.progress
                        val dy = pStart.y + (pEnd.y - pStart.y) * state.progress
                        Offset(dx, dy)
                    }
                    is RideState.Completed -> pEnd
                    else -> null
                }

                if (driverPos != null) {
                    // Outer cursor glow
                    drawCircle(color = GrabGreen.copy(alpha = 0.3f), radius = 24f * pulseScale, center = driverPos)
                    // Driver center mark
                    drawCircle(color = GrabGreenDark, radius = 14f, center = driverPos)
                    drawCircle(color = Color.White, radius = 6f, center = driverPos)
                }
            } else {
                // User's current location pin when idle
                drawCircle(color = GrabGreen.copy(alpha = 0.25f), radius = 28f * pulseScale, center = pStart)
                drawCircle(color = GrabGreen, radius = 10f, center = pStart)
                drawCircle(color = Color.White, radius = 4f, center = pStart)
            }
        }

        // Live Coordinate Floating Chip on Map
        currentLocation?.let { loc ->
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.92f),
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, GrabGreen.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(GrabGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        String.format(Locale.US, "GPS: %.4f, %.4f", loc.latitude, loc.longitude),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrabGreenDark
                    )
                }
            }
        }
    }
}


// 3. GRABFOOD RESTAURANT LISTINGS SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodScreen(viewModel: GrabViewModel, navController: NavController) {
    val searchQ by viewModel.foodSearchQuery.collectAsStateWithLifecycle()
    val activeCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val cartItems by viewModel.cart.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val deliveryAddress by viewModel.deliveryAddress.collectAsStateWithLifecycle()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            viewModel.fetchCurrentLocation(updatePickup = false, updateDelivery = true)
            viewModel.startTrackingCoordinates()
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.hasLocationPermission()) {
            viewModel.fetchCurrentLocation(updatePickup = false, updateDelivery = true)
            viewModel.startTrackingCoordinates()
        }
    }

    var activeRestaurantForDetail by remember { mutableStateOf<Restaurant?>(null) }

    // Filter restaurants criteria
    val filteredRestaurants = viewModel.mockRestaurants.filter { rest ->
        val matchesCategory = (activeCategory == "Tất cả" || rest.category == activeCategory)
        val matchesQuery = rest.name.lowercase().contains(searchQ.lowercase()) ||
                rest.items.any { it.name.lowercase().contains(searchQ.lowercase()) }
        matchesCategory && matchesQuery
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // SEARCH & CATEGORY SELECTOR HEADER PANEL
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GrabFood Giao Siêu Tốc 🛵",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = GrabAccentOrange,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // DELIVERY GPS LOCATION STRIP
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (viewModel.hasLocationPermission()) {
                                    viewModel.fetchCurrentLocation(updatePickup = false, updateDelivery = true)
                                    viewModel.startTrackingCoordinates()
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentLocation != null) Color(0xFFFFF3E0) else Color(0xFFF1F5F9)
                        ),
                        border = BorderStroke(1.dp, if (currentLocation != null) GrabAccentOrange.copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Filled.LocationOn,
                                    contentDescription = "GPS",
                                    tint = GrabAccentOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "Giao đến vị trí hiện tại (GPS)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GrabAccentOrange
                                        )
                                        if (currentLocation != null) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "• Tự động tính cước",
                                                fontSize = 10.sp,
                                                color = GrabGreenDark,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    Text(
                                        currentLocation?.let {
                                            "${it.address} (${String.format(Locale.US, "%.4f, %.4f", it.latitude, it.longitude)})"
                                        } ?: deliveryAddress,
                                        fontSize = 11.sp,
                                        color = TextPrimaryLight,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    if (viewModel.hasLocationPermission()) {
                                        viewModel.fetchCurrentLocation(updatePickup = false, updateDelivery = true)
                                        viewModel.startTrackingCoordinates()
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Filled.MyLocation, contentDescription = "Lấy GPS", tint = GrabAccentOrange, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchQ,
                        onValueChange = { viewModel.foodSearchQuery.value = it },
                        placeholder = { Text("Tìm trà sữa, bún phở bốc khói...") },
                        leadingIcon = { Icon(Icons.Filled.Search, "search", tint = GrabAccentOrange) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("food_search_bar"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GrabAccentOrange)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Categories slider list
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(viewModel.categories) { cat ->
                            val isSelected = cat == activeCategory
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) GrabAccentOrange else Color(0xFFF3F3F3))
                                    .clickable { viewModel.selectedCategory.value = cat }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White else TextPrimaryLight,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // LIST OF POPULAR RESTAURANTS
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredRestaurants.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.LocalDining, null, tint = TextSecondaryLight.copy(alpha = 0.5f), modifier = Modifier.size(56.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Không tìm thấy quán ăn nào phù hợp", fontWeight = FontWeight.Bold, color = TextPrimaryLight)
                                Text("Hãy thử từ khoá phở, bún, cơm chả hoặc phê la!", fontSize = 12.sp, color = TextSecondaryLight)
                            }
                        }
                    }
                } else {
                    items(filteredRestaurants) { rest ->
                        RestaurantCard(restaurant = rest, viewModel = viewModel, onClick = { activeRestaurantForDetail = rest })
                    }
                }
            }
        }

        // FLOATING CART ROW AT THE BOTTOM IF COMPONENT HAS ACTIVE FOOD ITEMS
        if (cartItems.isNotEmpty()) {
            val count = cartItems.sumOf { it.quantity }
            val total = cartItems.sumOf { it.menuItem.price * it.quantity }

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("floating_cart_bar")
                    .clickable { navController.navigate("cart") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = GrabGreen),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.ShoppingCart, "cart icon", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Đã chọn $count món",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatVnd(total),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Filled.ChevronRight, "arrow", tint = Color.White)
                    }
                }
            }
        }

        // RENDER RESTAURANT DETAIL DIALOG WITH MENU ITEMS & CLICK ACTIONS
        activeRestaurantForDetail?.let { rest ->
            RestaurantMenuDialog(
                restaurant = rest,
                viewModel = viewModel,
                onDismiss = { activeRestaurantForDetail = null }
            )
        }
    }
}

// RESTAURANT DISPLAY CARD
@Composable
fun RestaurantCard(
    restaurant: Restaurant,
    viewModel: GrabViewModel? = null,
    onClick: () -> Unit
) {
    val displayDist = viewModel?.getRestaurantDistanceKm(restaurant) ?: restaurant.distanceKm
    val displayFee = viewModel?.getRestaurantDeliveryFee(restaurant) ?: restaurant.deliveryFee

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // Gradient banner placeholder mimicking photo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(GrabAccentOrange.copy(alpha = 0.2f), GrabGreen.copy(alpha = 0.2f))
                        )
                    )
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(GrabAccentOrange)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .align(Alignment.BottomStart)
                ) {
                    Text(restaurant.category, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .align(Alignment.TopEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Star, null, tint = GrabAccentYellow, modifier = Modifier.size(12.dp))
                    Text(" ${restaurant.rating}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Info rows
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = restaurant.name,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = TextPrimaryLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📍 ${String.format(Locale.US, "%.1f", displayDist)} km", fontSize = 11.sp, color = TextSecondaryLight)
                    Text("⏱️ ${restaurant.deliveryTimeMin} phút", fontSize = 11.sp, color = TextSecondaryLight)
                    Text("🛵 Phí Giao: ${formatVnd(displayFee)}", fontSize = 11.sp, color = GrabGreenDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// RESTAURANT INLINE DIALOG DETAILS
@Composable
fun RestaurantMenuDialog(restaurant: Restaurant, viewModel: GrabViewModel, onDismiss: () -> Unit) {
    val cart by viewModel.cart.collectAsStateWithLifecycle()

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header details
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(GrabAccentOrange, Color(0xFFFFB300))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(0.9f)) {
                            Text(restaurant.name, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                            Text("${restaurant.category} • Bảo đảm giao nhanh", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                        }
                        IconButton(
                            onClick = { onDismiss() },
                            modifier = Modifier.weight(0.1f)
                        ) {
                            Icon(Icons.Filled.Close, "close", tint = Color.White)
                        }
                    }
                }

                // Scrollable Dish Selection
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(restaurant.items) { dish ->
                        val cartItem = cart.find { it.menuItem.id == dish.id }
                        val quantity = cartItem?.quantity ?: 0

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = LightBackground),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(0.6f)) {
                                    Text(dish.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(dish.description, fontSize = 10.sp, color = TextSecondaryLight, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(formatVnd(dish.price), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = GrabAccentOrange)
                                }

                                Row(
                                    modifier = Modifier.weight(0.4f),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (quantity > 0) {
                                        IconButton(onClick = { viewModel.updateCartQuantity(dish, quantity - 1) }) {
                                            Icon(Icons.Filled.Remove, "less", tint = Color.Red, modifier = Modifier.size(20.dp))
                                        }
                                        Text("$quantity", fontWeight = FontWeight.Bold, color = TextPrimaryLight, fontSize = 14.sp)
                                    }
                                    IconButton(
                                        onClick = { viewModel.addToCart(dish) },
                                        modifier = Modifier.testTag("add_item_${dish.id}")
                                    ) {
                                        Icon(Icons.Filled.Add, "add", tint = GrabGreenDark, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Check out summary trigger logic inside dialog
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = GrabGreen),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Xong & Xem Giỏ Hàng", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


// 4. CART CHECKOUT AND PROGRESS SIMULATOR
@Composable
fun CartScreen(viewModel: GrabViewModel, navController: NavController) {
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val activeFoodState by viewModel.foodState.collectAsStateWithLifecycle()
    val walletBal by viewModel.walletBalance.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val deliveryAddress by viewModel.deliveryAddress.collectAsStateWithLifecycle()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            viewModel.fetchCurrentLocation(updatePickup = false, updateDelivery = true)
            viewModel.startTrackingCoordinates()
        }
    }

    val totalProdPrice = cart.sumOf { it.menuItem.price * it.quantity }
    val firstItem = cart.firstOrNull()?.menuItem
    val activeRestaurant = viewModel.mockRestaurants.find { rest ->
        rest.items.any { it.id == firstItem?.id }
    } ?: viewModel.mockRestaurants.first()

    val dynamicDistKm = viewModel.getRestaurantDistanceKm(activeRestaurant)
    val dynamicDeliveryFee = viewModel.getRestaurantDeliveryFee(activeRestaurant)
    val checkoutTotal = totalProdPrice + dynamicDeliveryFee

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.navigate("food") }) {
                Icon(Icons.Filled.ArrowBack, "back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Xác Nhận Giỏ Hàng 🍔", fontWeight = FontWeight.Black, fontSize = 18.sp, color = TextPrimaryLight)
        }

        if (activeFoodState is FoodState.Idle) {
            if (cart.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.ShoppingCart, null, modifier = Modifier.size(64.dp), tint = TextSecondaryLight.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Giỏ hàng của bạn đang trống!", fontWeight = FontWeight.Bold, color = TextPrimaryLight)
                        Text("Hãy thêm các món phở bún nóng hổi bên GrabFood nhé", fontSize = 12.sp, color = TextSecondaryLight)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { navController.navigate("food") },
                            colors = ButtonDefaults.buttonColors(containerColor = GrabAccentOrange)
                        ) {
                            Text("Quay lại đặt món")
                        }
                    }
                }
            } else {
                // RENDER ITEM CARDS LIST & CHECKOUT FORM
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Delivery GPS Destination card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (viewModel.hasLocationPermission()) {
                                        viewModel.fetchCurrentLocation(updatePickup = false, updateDelivery = true)
                                        viewModel.startTrackingCoordinates()
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, if (currentLocation != null) GrabGreen.copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.LocationOn, contentDescription = "Delivery GPS", tint = GrabAccentOrange, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Địa chỉ nhận món (GPS)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimaryLight)
                                    }
                                    TextButton(
                                        onClick = {
                                            if (viewModel.hasLocationPermission()) {
                                                viewModel.fetchCurrentLocation(updatePickup = false, updateDelivery = true)
                                                viewModel.startTrackingCoordinates()
                                            } else {
                                                locationPermissionLauncher.launch(
                                                    arrayOf(
                                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                                    )
                                                )
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Filled.MyLocation, null, tint = GrabGreenDark, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (currentLocation != null) "Cập nhật" else "Lấy vị trí", fontSize = 11.sp, color = GrabGreenDark, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Text(
                                    deliveryAddress,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryLight
                                )

                                currentLocation?.let { loc ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(GrabGreen))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            String.format(Locale.US, "Tọa độ GPS: %.5f° N, %.5f° E (±%.1fm)", loc.latitude, loc.longitude, loc.accuracy),
                                            fontSize = 10.sp,
                                            color = GrabGreenDark,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Khoảng cách từ ${activeRestaurant.name}: ${String.format(Locale.US, "%.1f", dynamicDistKm)} km",
                                    fontSize = 11.sp,
                                    color = TextSecondaryLight
                                )
                            }
                        }
                    }

                    // Itemized elements
                    items(cart) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(item.menuItem.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        "${item.quantity} phần • " + formatVnd(item.menuItem.price * item.quantity),
                                        fontSize = 11.sp,
                                        color = GrabAccentOrange,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { viewModel.updateCartQuantity(item.menuItem, item.quantity - 1) }) {
                                        Icon(Icons.Filled.Remove, null, tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                    Text("${item.quantity}", fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { viewModel.updateCartQuantity(item.menuItem, item.quantity + 1) }) {
                                        Icon(Icons.Filled.Add, null, tint = GrabGreenDark, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Total pricing layout card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Bản ghi thanh toán", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Tổng tiền món ăn", fontSize = 12.sp, color = TextSecondaryLight)
                                    Text(formatVnd(totalProdPrice), fontSize = 12.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Phí giao hàng (${String.format(Locale.US, "%.1f", dynamicDistKm)} km)", fontSize = 12.sp, color = TextSecondaryLight)
                                    Text(formatVnd(dynamicDeliveryFee), fontSize = 12.sp)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Cần thanh toán", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = formatVnd(checkoutTotal),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = GrabAccentOrange,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // Payment mechanism selection row
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Wallet, "GrabPay", tint = GrabGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Ví điện tử GrabPay", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Số dư khả dụng: " + formatVnd(walletBal), fontSize = 10.sp, color = TextSecondaryLight)
                                    }
                                }

                                if (walletBal < checkoutTotal) {
                                    Text("Số dư không đủ!", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("Được chọn ✓", color = GrabGreenDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // BOTTOM ACTION CHEKOUT BUTTON
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    val balanceSufficient = walletBal >= checkoutTotal
                    Button(
                        onClick = {
                            if (balanceSufficient) {
                                viewModel.checkoutFood(activeRestaurant.name, dynamicDeliveryFee)
                            } else {
                                viewModel.topUpWallet(100000.0) // quick top up helper triggers
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("checkout_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (balanceSufficient) GrabAccentOrange else GrabGreen
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (balanceSufficient) "Đồng ý Đặt hàng • " + formatVnd(checkoutTotal) else "Nạp nhanh 100k vào GrabPay",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // RENDERS LIVE FOOD TICKING SIMULATOR DETAILS
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                when (val status = activeFoodState) {
                    is FoodState.Preparing -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = GrabAccentOrange)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Nhà hàng đang chuẩn bị món ăn...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = GrabAccentOrange
                                )
                                Text(
                                    status.itemsSummary,
                                    fontSize = 12.sp,
                                    color = TextSecondaryLight,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                Text(
                                    "Ước lượng hoàn thành trong: ${status.timerSeconds} giây",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                TextButton(onClick = { viewModel.cancelFoodOrder() }) {
                                    Text("Hủy đơn hàng", color = Color.Red)
                                }
                            }
                        }
                    }

                    is FoodState.Delivering -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Draw an active slider canvas representing moving delivery motorbike
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .background(GrabGreenLight, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    val progressOffset = 40.dp + (200.dp * status.progress)
                                    Icon(
                                        Icons.Filled.Motorcycle,
                                        "shipper",
                                        tint = GrabAccentOrange,
                                        modifier = Modifier.offset(x = progressOffset)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Tài xế đang giao thức ăn đến bạn vù vù!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = GrabGreenDark
                                )
                                Text(
                                    "Shipper thân yêu: ${status.driverName}",
                                    fontSize = 13.sp,
                                    color = TextPrimaryLight
                                )
                                Text(
                                    "Số tiền đã thanh toán: " + formatVnd(status.price),
                                    fontSize = 11.sp,
                                    color = TextSecondaryLight
                                )
                                LinearProgressIndicator(
                                    progress = { status.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    color = GrabAccentOrange,
                                    trackColor = Color(0xFFFFF3E0)
                                )
                            }
                        }
                    }

                    is FoodState.Delivered -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(GrabGreenLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.LocalDining, null, tint = GrabGreen, modifier = Modifier.size(28.dp))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Đã Giao Xong! Ngon miệng nhé!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = GrabGreenDark
                                )
                                Text(
                                    status.order.itemsSummary,
                                    fontSize = 12.sp,
                                    color = TextSecondaryLight,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "Đơn hàng được lưu tự động trong Lịch sử của bạn.",
                                    fontSize = 10.sp,
                                    color = TextSecondaryLight
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}


// 5. ACTIVITY HISTORY LOGS TAB SCREEN
@Composable
fun HistoryScreen(viewModel: GrabViewModel) {
    var selectedTabState by remember { mutableStateOf(0) } // 0: Rides, 1: Food Takeouts
    val pastRides by viewModel.rideHistory.collectAsStateWithLifecycle()
    val pastOrders by viewModel.foodOrders.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        // TAB CONTROLLER
        TabRow(
            selectedTabIndex = selectedTabState,
            containerColor = Color.White,
            contentColor = GrabGreen
        ) {
            Tab(
                selected = selectedTabState == 0,
                onClick = { selectedTabState = 0 },
                text = { Text("Chuyến xe đặt 🏍️", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTabState == 1,
                onClick = { selectedTabState = 1 },
                text = { Text("Đơn đồ ăn 🍔", fontWeight = FontWeight.Bold) }
            )
        }

        if (selectedTabState == 0) {
            // Render list of rides
            if (pastRides.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.DirectionsCar, null, modifier = Modifier.size(56.dp), tint = TextSecondaryLight.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Chưa có chuyến xe nào hoàn thành", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pastRides) { ride ->
                        RideHistoryItemRow(ride)
                    }
                }
            }
        } else {
            // Render list of foods
            if (pastOrders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.LocalDining, null, modifier = Modifier.size(56.dp), tint = TextSecondaryLight.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Chưa gọi món ăn nào", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pastOrders) { order ->
                        FoodOrderItemRow(order)
                    }
                }
            }
        }
    }
}
