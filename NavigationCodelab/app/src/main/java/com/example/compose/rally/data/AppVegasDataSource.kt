package com.example.compose.rally.data

import com.example.compose.rally.generated.ConfigurationKeys
import com.example.compose.rally.generated.ConfigurationSource
import com.example.compose.rally.generated.GeneratedVegasDataSource
import com.example.compose.rally.generated.PromotionHistoryKeys
import com.example.compose.rally.generated.PromotionHistorySource
import com.example.compose.rally.generated.UserKeys
import com.example.compose.rally.generated.UserSource
import java.time.LocalDate
import java.util.Calendar

class AppVegasDataSource(
    private val config: Configuration = Configuration()
) : GeneratedVegasDataSource {

    override fun fetchUserStringSet(source: UserSource, key: UserKeys.UserStringSetSourceKey): Set<String>? {
        return when (key) {
            UserKeys.UserStringSetSourceKey.Target -> setOf("free")
        }
    }

    override fun fetchUserInt(source: UserSource, key: UserKeys.UserIntSourceKey): Int? {
        return when (key) {
            UserKeys.UserIntSourceKey.Day -> 9999
            UserKeys.UserIntSourceKey.DaysSinceAccountCreated -> 45
        }
    }

    override fun fetchUserBoolean(source: UserSource, key: UserKeys.UserBooleanSourceKey): Boolean? {
        return when (key) {
            UserKeys.UserBooleanSourceKey.TrialState -> false
        }
    }

    override fun fetchPromotionHistoryInt(
        source: PromotionHistorySource,
        key: PromotionHistoryKeys.PromotionHistoryIntSourceKey
    ): Int? {
        return when (key) {
            is PromotionHistoryKeys.PromotionHistoryIntSourceKey.TimesShown -> null

            is PromotionHistoryKeys.PromotionHistoryIntSourceKey.DaysSinceLastShown -> {
                // Access where clause parameters: key.historyType, key.id
                when {
                    key.historyType == "group" && key.id == "android-premium-flash-sale" -> 21
                    key.historyType == "type" && key.id == "sale" -> 10
                    key.historyType == "type" && key.id == "nonsale" -> 10
                    key.historyType == "group" && key.id == "new-users" -> 5
                    key.historyType == "group" && key.id == "reac-users" -> 5
                    else -> 100
                }
            }
        }
    }

    override fun fetchConfigurationBoolean(
        source: ConfigurationSource,
        key: ConfigurationKeys.ConfigurationBooleanSourceKey
    ): Boolean? {
        return config.getBoolean(key.raw)
    }
}
