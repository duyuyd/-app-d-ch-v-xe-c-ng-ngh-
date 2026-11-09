package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.CardBrand
import com.example.data.LinkedPaymentMethod
import com.example.data.PaymentGatewayManager
import com.example.data.PaymentTransaction
import com.example.ui.theme.*

// Top-level Payment Method Selection Pill for Booking & Checkout Screens
@Composable
fun PaymentMethodSelector(
    viewModel: GrabViewModel,
    modifier: Modifier = Modifier
) {
    val activeMethod by viewModel.selectedPaymentMethod.collectAsStateWithLifecycle()
    var showSelectionSheet by remember { mutableStateOf(false) }
    var showAddCardDialog by remember { mutableStateOf(false) }
    var showLinkWalletDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { showSelectionSheet = true }
            .testTag("payment_method_selector"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, GrabGreen.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            when (activeMethod?.provider) {
                                "STRIPE_VISA", "STRIPE_MASTERCARD" -> Color(0xFF1A1F71)
                                "MOMO" -> Color(0xFFA50064)
                                "ZALOPAY" -> Color(0xFF0068FF)
                                "VNPAY" -> Color(0xFF004A99)
                                "CASH" -> Color(0xFF2E7D32)
                                else -> GrabGreenDark
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when (activeMethod?.type) {
                            "CREDIT_CARD" -> Icons.Filled.CreditCard
                            "CASH" -> Icons.Filled.Payments
                            else -> Icons.Filled.AccountBalanceWallet
                        },
                        contentDescription = "Payment Icon",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = activeMethod?.title ?: "Chọn phương thức thanh toán",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimaryLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(GrabGreenLight)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                if (activeMethod?.type == "CREDIT_CARD") "Stripe" else "Đang dùng",
                                fontSize = 9.sp,
                                color = GrabGreenDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = activeMethod?.subtitle ?: "Bấm để thay đổi thẻ hoặc ví",
                        fontSize = 11.sp,
                        color = TextSecondaryLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Đổi",
                    color = GrabGreenDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "Select",
                    tint = GrabGreenDark,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showSelectionSheet) {
        PaymentMethodSelectionSheet(
            viewModel = viewModel,
            onDismiss = { showSelectionSheet = false },
            onAddNewCard = {
                showSelectionSheet = false
                showAddCardDialog = true
            },
            onLinkWallet = {
                showSelectionSheet = false
                showLinkWalletDialog = true
            }
        )
    }

    if (showAddCardDialog) {
        AddCreditCardDialog(
            viewModel = viewModel,
            onDismiss = { showAddCardDialog = false }
        )
    }

    if (showLinkWalletDialog) {
        LinkWalletDialog(
            viewModel = viewModel,
            onDismiss = { showLinkWalletDialog = false }
        )
    }
}

// Modal Sheet for choosing payment method
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodSelectionSheet(
    viewModel: GrabViewModel,
    onDismiss: () -> Unit,
    onAddNewCard: () -> Unit,
    onLinkWallet: () -> Unit
) {
    val methods by viewModel.allPaymentMethods.collectAsStateWithLifecycle()
    val activeMethod by viewModel.selectedPaymentMethod.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Phương thức thanh toán",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = TextPrimaryLight
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Text(
                "Chọn thẻ tín dụng (Stripe) hoặc ví điện tử để thanh toán",
                fontSize = 12.sp,
                color = TextSecondaryLight
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(methods) { method ->
                    val isSelected = activeMethod?.id == method.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.selectPaymentMethod(method)
                                onDismiss()
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) GrabGreenLight.copy(alpha = 0.5f) else Color(0xFFF9FAFB)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) GrabGreen else Color.LightGray.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (method.provider) {
                                                "STRIPE_VISA", "STRIPE_MASTERCARD" -> Color(0xFF1A1F71)
                                                "MOMO" -> Color(0xFFA50064)
                                                "ZALOPAY" -> Color(0xFF0068FF)
                                                "VNPAY" -> Color(0xFF004A99)
                                                "CASH" -> Color(0xFF2E7D32)
                                                else -> GrabGreenDark
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        when (method.type) {
                                            "CREDIT_CARD" -> Icons.Filled.CreditCard
                                            "CASH" -> Icons.Filled.Payments
                                            else -> Icons.Filled.AccountBalanceWallet
                                        },
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        method.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimaryLight
                                    )
                                    Text(
                                        method.subtitle,
                                        fontSize = 11.sp,
                                        color = TextSecondaryLight
                                    )
                                }
                            }

                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    viewModel.selectPaymentMethod(method)
                                    onDismiss()
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = GrabGreen)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons to add cards or link wallets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onAddNewCard,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GrabGreenDark)
                ) {
                    Icon(Icons.Filled.AddCard, contentDescription = null, tint = GrabGreenDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Thêm thẻ Stripe", fontSize = 12.sp, color = GrabGreenDark, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onLinkWallet,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GrabAccentOrange)
                ) {
                    Icon(Icons.Filled.Link, contentDescription = null, tint = GrabAccentOrange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Liên kết ví", fontSize = 12.sp, color = GrabAccentOrange, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// 5. FULL PAYMENT MANAGEMENT SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentManagementScreen(viewModel: GrabViewModel, navController: NavController) {
    val methods by viewModel.allPaymentMethods.collectAsStateWithLifecycle()
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val walletBal by viewModel.walletBalance.collectAsStateWithLifecycle()

    var showAddCardDialog by remember { mutableStateOf(false) }
    var showLinkWalletDialog by remember { mutableStateOf(false) }
    var showTopUpDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Phương thức thanh toán, 1: Lịch sử giao dịch

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Cổng Thanh Toán & Ví 💳",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = TextPrimaryLight
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(LightBackground)
        ) {
            // WALLET MASTER CARD WITH TOP UP SHORTCUT
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF0F5132), Color(0xFF198754), Color(0xFF0D6EFD))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Ví GrabPay & Thẻ Liên Kết",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("PCI-DSS SECURED", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            formatVnd(walletBal),
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showTopUpDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = GrabGreenDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Nạp tiền ví", color = GrabGreenDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showAddCardDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.AddCard, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Thêm thẻ mới", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // TABS SELECTOR (PHƯƠNG THỨC / GIAO DỊCH)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = GrabGreenDark
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Phương thức (${methods.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Lịch sử giao dịch (${transactions.size})", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTab == 0) {
                // TAB 0: LINKED PAYMENT METHODS LIST
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Thẻ & Ví đã liên kết",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryLight
                            )
                            TextButton(onClick = { showLinkWalletDialog = true }) {
                                Icon(Icons.Filled.AddLink, null, tint = GrabAccentOrange, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Liên kết ví khác", fontSize = 12.sp, color = GrabAccentOrange, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    items(methods) { method ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    when (method.provider) {
                                                        "STRIPE_VISA", "STRIPE_MASTERCARD" -> Color(0xFF1A1F71)
                                                        "MOMO" -> Color(0xFFA50064)
                                                        "ZALOPAY" -> Color(0xFF0068FF)
                                                        "VNPAY" -> Color(0xFF004A99)
                                                        "CASH" -> Color(0xFF2E7D32)
                                                        else -> GrabGreenDark
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                when (method.type) {
                                                    "CREDIT_CARD" -> Icons.Filled.CreditCard
                                                    "CASH" -> Icons.Filled.Payments
                                                    else -> Icons.Filled.AccountBalanceWallet
                                                },
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    method.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                if (method.isDefault) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(GrabGreenLight)
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("Mặc định", fontSize = 10.sp, color = GrabGreenDark, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            Text(
                                                method.subtitle,
                                                fontSize = 11.sp,
                                                color = TextSecondaryLight
                                            )
                                            if (method.cardHolder.isNotBlank()) {
                                                Text(
                                                    method.cardHolder,
                                                    fontSize = 10.sp,
                                                    color = TextSecondaryLight.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                    }

                                    if (method.provider != "GRABPAY" && method.provider != "CASH") {
                                        IconButton(onClick = { viewModel.removePaymentMethod(method.id) }) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Xoá", tint = Color.Red.copy(alpha = 0.7f))
                                        }
                                    }
                                }

                                if (!method.isDefault) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = { viewModel.setDefaultPaymentMethod(method.id) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Đặt làm mặc định", fontSize = 12.sp, color = GrabGreenDark, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showAddCardDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, GrabGreen.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(GrabGreenLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, tint = GrabGreenDark)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Thêm thẻ tín dụng hoặc ghi nợ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Hỗ trợ Visa, MasterCard, JCB qua Stripe bảo mật", fontSize = 11.sp, color = TextSecondaryLight)
                                }
                            }
                        }
                    }
                }
            } else {
                // TAB 1: TRANSACTION HISTORY
                if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.ReceiptLong, null, tint = TextSecondaryLight.copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Chưa có giao dịch nào", fontWeight = FontWeight.Bold, color = TextPrimaryLight)
                            Text("Các giao dịch qua thẻ Stripe, MoMo hoặc Ví sẽ hiển thị ở đây.", fontSize = 12.sp, color = TextSecondaryLight, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(transactions) { txn ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (txn.serviceType) {
                                                        "RIDE" -> GrabGreenLight
                                                        "FOOD" -> Color(0xFFFFF3E0)
                                                        else -> Color(0xFFE8EAF6)
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                when (txn.serviceType) {
                                                    "RIDE" -> Icons.Filled.DirectionsCar
                                                    "FOOD" -> Icons.Filled.LocalDining
                                                    else -> Icons.Filled.AccountBalanceWallet
                                                },
                                                contentDescription = null,
                                                tint = when (txn.serviceType) {
                                                    "RIDE" -> GrabGreenDark
                                                    "FOOD" -> GrabAccentOrange
                                                    else -> Color(0xFF3F51B5)
                                                },
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                txn.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                "${txn.paymentMethodTitle} • ${formatTime(txn.timestamp)}",
                                                fontSize = 11.sp,
                                                color = TextSecondaryLight
                                            )
                                            Text(
                                                "Mã: ${txn.transactionId}",
                                                fontSize = 10.sp,
                                                color = TextSecondaryLight.copy(alpha = 0.7f)
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            (if (txn.serviceType == "TOPUP") "+" else "-") + formatVnd(txn.amount),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = if (txn.serviceType == "TOPUP") GrabGreenDark else TextPrimaryLight
                                        )
                                        Text(
                                            txn.status,
                                            fontSize = 10.sp,
                                            color = GrabGreenDark,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCardDialog) {
        AddCreditCardDialog(
            viewModel = viewModel,
            onDismiss = { showAddCardDialog = false }
        )
    }

    if (showLinkWalletDialog) {
        LinkWalletDialog(
            viewModel = viewModel,
            onDismiss = { showLinkWalletDialog = false }
        )
    }

    if (showTopUpDialog) {
        TopUpDialog(
            viewModel = viewModel,
            onDismiss = { showTopUpDialog = false }
        )
    }
}

// STRIPE CREDIT/DEBIT CARD TOKENIZATION MODAL WITH REAL-TIME PREVIEW
@Composable
fun AddCreditCardDialog(viewModel: GrabViewModel, onDismiss: () -> Unit) {
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var cardHolder by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val cleanNumber = cardNumber.replace(" ", "")
    val detectedBrand = PaymentGatewayManager.detectBrand(cleanNumber)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Liên kết thẻ (Stripe)", fontWeight = FontWeight.Black, fontSize = 17.sp)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Đóng")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // VISUAL CARD PREVIEW
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = when (detectedBrand) {
                                        CardBrand.VISA -> listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
                                        CardBrand.MASTERCARD -> listOf(Color(0xFF232526), Color(0xFF414345), Color(0xFFBA8B02))
                                        else -> listOf(Color(0xFF141E30), Color(0xFF243B55))
                                    }
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Gold Chip
                                Box(
                                    modifier = Modifier
                                        .size(34.dp, 24.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE5A93B))
                                )
                                Text(
                                    detectedBrand.displayName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Serif
                                )
                            }

                            // Card Number formatted
                            val formattedPreview = if (cleanNumber.isEmpty()) {
                                "•••• •••• •••• ••••"
                            } else {
                                cleanNumber.padEnd(16, '•').chunked(4).joinToString(" ")
                            }
                            Text(
                                formattedPreview,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("CHỦ THẺ", color = Color.White.copy(alpha = 0.6f), fontSize = 8.sp)
                                    Text(
                                        if (cardHolder.isBlank()) "NGUYEN VAN A" else cardHolder.uppercase(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("HẾT HẠN", color = Color.White.copy(alpha = 0.6f), fontSize = 8.sp)
                                    Text(
                                        if (expiry.isBlank()) "MM/YY" else expiry,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // INPUT FORM FIELDS
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(16)
                        cardNumber = digits.chunked(4).joinToString(" ")
                    },
                    label = { Text("Số thẻ tín dụng") },
                    placeholder = { Text("4242 4242 4242 4242") },
                    leadingIcon = { Icon(Icons.Filled.CreditCard, null, tint = GrabGreenDark) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_number_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GrabGreen)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = expiry,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }.take(4)
                            expiry = if (digits.length >= 3) {
                                "${digits.take(2)}/${digits.substring(2)}"
                            } else {
                                digits
                            }
                        },
                        label = { Text("Hạn (MM/YY)") },
                        placeholder = { Text("12/28") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_expiry_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = cvv,
                        onValueChange = { input ->
                            cvv = input.filter { it.isDigit() }.take(4)
                        },
                        label = { Text("CVV/CVC") },
                        placeholder = { Text("123") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_cvv_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = cardHolder,
                    onValueChange = { cardHolder = it.uppercase() },
                    label = { Text("Tên in trên thẻ (không dấu)") },
                    placeholder = { Text("NGUYEN VAN A") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_holder_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        errorMessage!!,
                        color = Color.Red,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        isProcessing = true
                        errorMessage = null
                        viewModel.linkCreditCard(
                            cardNumber = cardNumber,
                            expiry = expiry,
                            cvv = cvv,
                            cardHolder = cardHolder,
                            onSuccess = {
                                isProcessing = false
                                onDismiss()
                            },
                            onError = { err ->
                                isProcessing = false
                                errorMessage = err
                            }
                        )
                    },
                    enabled = !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_link_card_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = GrabGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đang token hoá bảo mật...", fontSize = 13.sp)
                    } else {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bảo Mật & Liên Kết Thẻ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// DIGITAL WALLET (MOMO, ZALOPAY, VNPAY) LINK MODAL
@Composable
fun LinkWalletDialog(viewModel: GrabViewModel, onDismiss: () -> Unit) {
    var selectedWallet by remember { mutableStateOf("MOMO") }
    var phoneNumber by remember { mutableStateOf("0988888888") }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Liên kết ví điện tử", fontWeight = FontWeight.Black, fontSize = 17.sp)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Đóng")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // PROVIDER SELECTOR BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("MOMO" to "Ví MoMo", "ZALOPAY" to "ZaloPay", "VNPAY" to "VNPay").forEach { (prov, name) ->
                        val isSelected = selectedWallet == prov
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) {
                                        when (prov) {
                                            "MOMO" -> Color(0xFFA50064)
                                            "ZALOPAY" -> Color(0xFF0068FF)
                                            else -> Color(0xFF004A99)
                                        }
                                    } else Color(0xFFF3F4F6)
                                )
                                .clickable { selectedWallet = prov }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                name,
                                color = if (isSelected) Color.White else TextPrimaryLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Số điện thoại đăng ký ví") },
                    leadingIcon = { Icon(Icons.Filled.PhoneAndroid, null, tint = GrabGreenDark) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage!!, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        isProcessing = true
                        errorMessage = null
                        viewModel.linkDigitalWallet(
                            provider = selectedWallet,
                            phoneNumber = phoneNumber,
                            onSuccess = {
                                isProcessing = false
                                onDismiss()
                            },
                            onError = { err ->
                                isProcessing = false
                                errorMessage = err
                            }
                        )
                    },
                    enabled = !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (selectedWallet) {
                            "MOMO" -> Color(0xFFA50064)
                            "ZALOPAY" -> Color(0xFF0068FF)
                            else -> Color(0xFF004A99)
                        }
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đang liên kết ví...", fontSize = 13.sp)
                    } else {
                        Text("Xác Nhận & Liên Kết Ngay", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// TOP UP WALLET DIALOG
@Composable
fun TopUpDialog(viewModel: GrabViewModel, onDismiss: () -> Unit) {
    val methods by viewModel.allPaymentMethods.collectAsStateWithLifecycle()
    var selectedAmount by remember { mutableStateOf(100000.0) }
    var selectedMethod by remember { mutableStateOf<LinkedPaymentMethod?>(null) }

    val externalMethods = methods.filter { it.provider != "GRABPAY" && it.provider != "CASH" }

    LaunchedEffect(externalMethods) {
        if (selectedMethod == null && externalMethods.isNotEmpty()) {
            selectedMethod = externalMethods.first()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Nạp tiền vào ví GrabPay", fontWeight = FontWeight.Black, fontSize = 17.sp)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Đóng")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Chọn số tiền muốn nạp:", fontSize = 12.sp, color = TextSecondaryLight)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(50000.0, 100000.0, 200000.0).forEach { amt ->
                        val isSel = selectedAmount == amt
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) GrabGreenLight else Color(0xFFF3F4F6))
                                .border(1.dp, if (isSel) GrabGreen else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { selectedAmount = amt }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                formatVnd(amt),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isSel) GrabGreenDark else TextPrimaryLight
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Thanh toán nguồn tiền từ:", fontSize = 12.sp, color = TextSecondaryLight)
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(externalMethods) { m ->
                        val isChosen = selectedMethod?.id == m.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isChosen) GrabGreenLight.copy(alpha = 0.5f) else Color(0xFFFAFAFA))
                                .clickable { selectedMethod = m }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (m.type == "CREDIT_CARD") Icons.Filled.CreditCard else Icons.Filled.AccountBalanceWallet,
                                    null,
                                    tint = GrabGreenDark,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(m.title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            RadioButton(
                                selected = isChosen,
                                onClick = { selectedMethod = m },
                                colors = RadioButtonDefaults.colors(selectedColor = GrabGreen)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val methodTitle = selectedMethod?.title ?: "Thẻ ngân hàng / Stripe"
                        viewModel.topUpWallet(selectedAmount, methodTitle)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GrabGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Nạp ngay ${formatVnd(selectedAmount)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
