package com.example.compose.rally.data

import com.example.compose.rally.generated.ConfigurationKeys
import com.example.compose.rally.generated.GeneratedVegasDataSource
import com.example.compose.rally.generated.PromotionHistoryKeys
import com.example.compose.rally.generated.SurveyHistoryKeys
import com.example.compose.rally.generated.UserKeys
import com.fitnow.vegas.core.Creative
import com.fitnow.vegas.core.Promotion
import com.fitnow.vegas.core.PromotionGroup
import com.fitnow.vegas.core.QueryDataSource
import java.time.LocalDate

class AppVegasDataSource(
    private val config: Configuration = Configuration()
) : GeneratedVegasDataSource {

    override fun <D : QueryDataSource> customPromotionRules(
        group: PromotionGroup<D>,
        promotion: Promotion<D>
    ): Boolean {
        return config.getBoolean("kill-switch-${group.id}-${promotion.id}") != true
    }

    override fun isValid(creative: Creative): Boolean {
        return config.getBoolean("kill-switch-${creative.id}") != true
    }

    override fun fetchSurveyHistoryInt(key: SurveyHistoryKeys.SurveyHistoryIntSourceKey): Int? {
        return when (key) {
            is SurveyHistoryKeys.SurveyHistoryIntSourceKey.DaysSinceLastShown -> {
                key.surveyName
                3
            }
        }
    }

    override fun fetchUserStringSet(key: UserKeys.UserStringSetSourceKey): Set<String>? {
        return when (key) {
            UserKeys.UserStringSetSourceKey.Target -> setOf("free")
        }
    }

    override fun fetchUserInt(key: UserKeys.UserIntSourceKey): Int? {
        return when (key) {
            UserKeys.UserIntSourceKey.Day -> 9999
            UserKeys.UserIntSourceKey.DaysSinceAccountCreated -> 45
        }
    }

    override fun fetchUserBoolean(key: UserKeys.UserBooleanSourceKey): Boolean? {
        return when (key) {
            UserKeys.UserBooleanSourceKey.TrialState -> false
        }
    }

    override fun fetchPromotionHistoryInt(
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
        key: ConfigurationKeys.ConfigurationBooleanSourceKey
    ): Boolean? {
        return config.getBoolean(key.raw)
    }
}
