package com.example.compose.rally.data.mock

import com.fitnow.vegas.core.QueryDataSource

/**
 * Mock implementation of QueryDataSource for Rally app.
 * Provides mock data for promotion evaluation.
 */
class MockVegasDataSource : QueryDataSource {
    
    // Mock user data
    val userTargets: Set<String> = setOf("free", "premium")
    val userDay: Int = 8800
    val userTrialState: Boolean = false
    val userDaysSinceAccountCreated: Int = 30
    
    // Mock promotion history data
    val promotionTimesShownMap: MutableMap<String, Int> = mutableMapOf()
    val promotionDaysSinceLastShownMap: MutableMap<String, Int> = mutableMapOf()
    
    // Mock configuration data
    val configurationMap: Map<String, Boolean> = mapOf(
        "androidPremiumTimerTest" to false,
        "showDashboardPromo" to true
    )
    
    // Mock survey history data
    val surveyDaysSinceLastShownMap: MutableMap<String, Int> = mutableMapOf()
    
    /**
     * Get times a promotion group has been shown.
     */
    fun getPromotionTimesShown(historyType: String, id: String): Int {
        return promotionTimesShownMap["$historyType:$id"] ?: 0
    }
    
    /**
     * Get days since a promotion was last shown.
     */
    fun getPromotionDaysSinceLastShown(historyType: String, id: String): Int {
        return promotionDaysSinceLastShownMap["$historyType:$id"] ?: 100
    }
    
    /**
     * Get days since a survey was last shown.
     */
    fun getSurveyDaysSinceLastShown(surveyName: String, stepName: String): Int {
        return surveyDaysSinceLastShownMap["$surveyName:$stepName"] ?: 9000
    }
    
    /**
     * Get configuration value.
     */
    fun getConfiguration(key: String): Boolean {
        return configurationMap[key] ?: false
    }
}
