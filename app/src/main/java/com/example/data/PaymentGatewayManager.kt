package com.example.data

import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.UUID

enum class CardBrand(val displayName: String, val prefixes: List<String>, val lengths: List<Int>) {
    VISA("Visa", listOf("4"), listOf(13, 16, 19)),
    MASTERCARD("Mastercard", listOf("51", "52", "53", "54", "55", "22", "23", "24", "25", "26", "27"), listOf(16)),
    JCB("JCB", listOf("35"), listOf(16)),
    AMEX("American Express", listOf("34", "37"), listOf(15)),
    UNKNOWN("Thẻ quốc tế", emptyList(), listOf(16))
}

data class CardValidationResult(
    val isValid: Boolean,
    val brand: CardBrand,
    val errorMessage: String? = null
)

class PaymentGatewayManager {

    companion object {
        fun detectBrand(cleanedNumber: String): CardBrand {
            for (brand in CardBrand.values()) {
                if (brand == CardBrand.UNKNOWN) continue
                if (brand.prefixes.any { cleanedNumber.startsWith(it) }) {
                    return brand
                }
            }
            return CardBrand.UNKNOWN
        }

        fun isValidLuhn(cleanedNumber: String): Boolean {
            if (cleanedNumber.length < 13) return false
            var sum = 0
            var alternate = false
            for (i in cleanedNumber.length - 1 downTo 0) {
                var n = cleanedNumber[i].digitToIntOrNull() ?: return false
                if (alternate) {
                    n *= 2
                    if (n > 9) n -= 9
                }
                sum += n
                alternate = !alternate
            }
            return sum % 10 == 0
        }

        fun validateCard(
            cardNumber: String,
            expiry: String, // MM/YY
            cvv: String,
            holderName: String
        ): Pair<Boolean, String?> {
            val cleanNum = cardNumber.replace(" ", "").replace("-", "")
            if (cleanNum.length !in 13..19 || !cleanNum.all { it.isDigit() }) {
                return false to "Số thẻ không hợp lệ (cần 15-16 chữ số)"
            }
            if (!isValidLuhn(cleanNum)) {
                return false to "Số thẻ sai định dạng kiểm tra (Luhn checksum)"
            }

            // Validate Expiry (MM/YY)
            val expiryClean = expiry.replace("/", "").replace(" ", "")
            if (expiryClean.length != 4 || !expiryClean.all { it.isDigit() }) {
                return false to "Ngày hết hạn phải theo định dạng MM/YY"
            }
            val month = expiryClean.substring(0, 2).toIntOrNull() ?: 0
            val year = 2000 + (expiryClean.substring(2, 4).toIntOrNull() ?: 0)
            if (month !in 1..12) {
                return false to "Tháng hết hạn không hợp lệ (01-12)"
            }

            val cal = Calendar.getInstance()
            val currentYear = cal.get(Calendar.YEAR)
            val currentMonth = cal.get(Calendar.MONTH) + 1
            if (year < currentYear || (year == currentYear && month < currentMonth)) {
                return false to "Thẻ đã hết hạn sử dụng"
            }

            // Validate CVV
            val cleanCvv = cvv.trim()
            if (cleanCvv.length !in 3..4 || !cleanCvv.all { it.isDigit() }) {
                return false to "Mã bảo mật CVV phải gồm 3 hoặc 4 chữ số"
            }

            // Validate Holder
            if (holderName.trim().length < 3) {
                return false to "Vui lòng nhập tên chủ thẻ không dấu"
            }

            return true to null
        }
    }

    /**
     * Simulates Stripe payment method tokenization and card linking
     */
    suspend fun tokenizeAndLinkCard(
        cardNumber: String,
        expiry: String,
        cvv: String,
        holderName: String
    ): Result<LinkedPaymentMethod> {
        val (isValid, errorMsg) = validateCard(cardNumber, expiry, cvv, holderName)
        if (!isValid) {
            return Result.failure(IllegalArgumentException(errorMsg ?: "Thông tin thẻ không hợp lệ"))
        }

        // Simulate network call to Stripe API
        delay(900)

        val cleanNum = cardNumber.replace(" ", "")
        val brand = detectBrand(cleanNum)
        val last4 = cleanNum.takeLast(4)
        val token = "pm_stripe_${UUID.randomUUID().toString().take(12)}"

        val method = LinkedPaymentMethod(
            type = "CREDIT_CARD",
            title = "${brand.displayName} •••• $last4",
            subtitle = "Hết hạn: $expiry",
            provider = if (brand == CardBrand.MASTERCARD) "STRIPE_MASTERCARD" else "STRIPE_VISA",
            lastFour = last4,
            cardHolder = holderName.trim().uppercase(),
            isDefault = false,
            token = token
        )
        return Result.success(method)
    }

    /**
     * Simulates linking digital e-wallets (MoMo, ZaloPay, VNPay)
     */
    suspend fun linkDigitalWallet(
        provider: String,
        phoneNumber: String
    ): Result<LinkedPaymentMethod> {
        val cleanPhone = phoneNumber.replace(" ", "").trim()
        if (cleanPhone.length < 10 || !cleanPhone.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("Số điện thoại liên kết ví không hợp lệ"))
        }

        delay(800)

        val (title, provKey) = when (provider.uppercase()) {
            "MOMO" -> "Ví MoMo" to "MOMO"
            "ZALOPAY" -> "Ví ZaloPay" to "ZALOPAY"
            "VNPAY" -> "Cổng VNPAY-QR" to "VNPAY"
            else -> "Ví điện tử" to "E_WALLET"
        }

        val method = LinkedPaymentMethod(
            type = "E_WALLET",
            title = "$title ($cleanPhone)",
            subtitle = "Liên kết tự động thanh toán",
            provider = provKey,
            lastFour = cleanPhone.takeLast(4),
            cardHolder = "Chủ ví: $cleanPhone",
            isDefault = false,
            token = "ewallet_${provKey.lowercase()}_${UUID.randomUUID().toString().take(8)}"
        )
        return Result.success(method)
    }

    /**
     * Simulates processing transaction through Stripe or linked digital wallet
     */
    suspend fun charge(
        amount: Double,
        method: LinkedPaymentMethod,
        orderTitle: String,
        serviceType: String
    ): Result<PaymentTransaction> {
        delay(1100) // Gateway latency

        val txnId = "TXN-${method.provider.take(4)}-${UUID.randomUUID().toString().take(8).uppercase()}"
        val transaction = PaymentTransaction(
            transactionId = txnId,
            title = orderTitle,
            amount = amount,
            paymentMethodTitle = method.title,
            status = "Thành công",
            serviceType = serviceType,
            timestamp = System.currentTimeMillis()
        )
        return Result.success(transaction)
    }
}
