package com.example.compose.rally.data

class Configuration {

    fun getBoolean(key: String): Boolean? {
        return when (key) {
            "androidPremiumTimerTest" -> true
            else -> null
        }
    }
}