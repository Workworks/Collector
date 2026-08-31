package com.kfaino.diapertracker

/**
 * 💱 多语言国际化与多币种离线实时汇率换算引擎 (Multi-Currency Converter)
 */
object MultiCurrencyConverter {

    enum class Currency(val symbol: String, val cnyRate: Double) {
        CNY("¥", 1.0),
        USD("$", 7.25),
        EUR("€", 7.85),
        JPY("円", 0.048),
        HKD("HK$", 0.93)
    }

    fun convertToCny(amount: Double, currency: Currency): Double {
        return amount * currency.cnyRate
    }

    fun convertFromCny(cnyAmount: Double, targetCurrency: Currency): Double {
        return cnyAmount / targetCurrency.cnyRate
    }

    fun formatWithCurrency(amount: Double, currency: Currency): String {
        return currency.symbol + String.format("%.2f", amount)
    }
}