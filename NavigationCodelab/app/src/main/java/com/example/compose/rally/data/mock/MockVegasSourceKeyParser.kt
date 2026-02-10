package com.example.compose.rally.data.mock

import com.fitnow.vegas.core.SourceKey
import com.fitnow.vegas.core.SourceKeyParser

/**
 * Parser for resolving SourceKeys from JSON source/key names.
 * Maps string-based references to typed SourceKey instances.
 */
object MockVegasSourceKeyParser : SourceKeyParser<MockVegasDataSource> {
    
    override fun findKey(sourceName: String, raw: String): SourceKey<MockVegasDataSource, *>? {
        return when (sourceName) {
            "user" -> findUserKey(raw)
            "configuration" -> findConfigurationKey(raw)
            else -> null
        }
    }
    
    override fun createKeyWithWhere(
        sourceName: String,
        keyName: String,
        whereParams: Map<String, String>
    ): SourceKey<MockVegasDataSource, *>? {
        return when (sourceName) {
            "promotionHistory" -> createPromotionHistoryKey(keyName, whereParams)
            "surveyHistory" -> createSurveyHistoryKey(keyName, whereParams)
            else -> null
        }
    }
    
    private fun findUserKey(keyName: String): SourceKey<MockVegasDataSource, *>? {
        return when (keyName) {
            "target" -> UserTargetKey
            "day" -> UserDayKey
            "trialState" -> UserTrialStateKey
            "daysSinceAccountCreated" -> UserDaysSinceAccountCreatedKey
            else -> null
        }
    }
    
    private fun findConfigurationKey(keyName: String): SourceKey<MockVegasDataSource, Boolean> {
        // Configuration keys are dynamic, so we create them on demand
        return ConfigurationFlagKey(keyName)
    }
    
    private fun createPromotionHistoryKey(
        keyName: String,
        whereParams: Map<String, String>
    ): SourceKey<MockVegasDataSource, Int>? {
        val historyType = whereParams["historyType"] ?: return null
        val id = whereParams["id"] ?: return null
        
        return when (keyName) {
            "timesShown" -> PromotionTimesShownKey(historyType, id)
            "daysSinceLastShown" -> PromotionDaysSinceLastShownKey(historyType, id)
            else -> null
        }
    }
    
    private fun createSurveyHistoryKey(
        keyName: String,
        whereParams: Map<String, String>
    ): SourceKey<MockVegasDataSource, Int>? {
        val surveyName = whereParams["survey-name"] ?: return null
        val stepName = whereParams["step-name"] ?: return null
        
        return when (keyName) {
            "daysSinceLastShown" -> SurveyDaysSinceLastShownKey(surveyName, stepName)
            else -> null
        }
    }
}
