package com.example.car_maintenance.utils

object CurrencyUtils {
    
    data class Currency(
        val code: String,
        val symbol: String,
        val name: String
    )
    
    val currencies = listOf(
        Currency("USD", "$", "US Dollar"),
        Currency("EUR", "€", "Euro"),
        Currency("GBP", "£", "British Pound"),
        Currency("JPY", "¥", "Japanese Yen"),
        Currency("CNY", "¥", "Chinese Yuan"),
        Currency("AUD", "A$", "Australian Dollar"),
        Currency("CAD", "C$", "Canadian Dollar"),
        Currency("CHF", "Fr", "Swiss Franc"),
        Currency("INR", "₹", "Indian Rupee"),
        Currency("RUB", "₽", "Russian Ruble"),
        Currency("BRL", "R$", "Brazilian Real"),
        Currency("ZAR", "R", "South African Rand"),
        Currency("KRW", "₩", "South Korean Won"),
        Currency("MXN", "$", "Mexican Peso"),
        Currency("SGD", "S$", "Singapore Dollar"),
        Currency("HKD", "HK$", "Hong Kong Dollar"),
        Currency("NOK", "kr", "Norwegian Krone"),
        Currency("SEK", "kr", "Swedish Krona"),
        Currency("DKK", "kr", "Danish Krone"),
        Currency("PLN", "zł", "Polish Zloty"),
        Currency("THB", "฿", "Thai Baht"),
        Currency("TRY", "₺", "Turkish Lira"),
        Currency("AED", "د.إ", "UAE Dirham"),
        Currency("SAR", "﷼", "Saudi Riyal"),
        Currency("NZD", "NZ$", "New Zealand Dollar"),
        Currency("CZK", "Kč", "Czech Koruna"),
        Currency("HUF", "Ft", "Hungarian Forint"),
        Currency("ILS", "₪", "Israeli Shekel"),
        Currency("CLP", "$", "Chilean Peso"),
        Currency("PHP", "₱", "Philippine Peso")
    )
    
    fun getCurrencyByCode(code: String): Currency {
        return currencies.find { it.code == code } ?: currencies[0]
    }
    
    fun formatAmount(amount: Double, currencyCode: String): String {
        val currency = getCurrencyByCode(currencyCode)
        return "${currency.symbol} ${"%.2f".format(amount)}"
    }
}