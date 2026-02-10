package com.example.compose.rally.data.mock

import com.fitnow.vegas.core.*

/**
 * User source keys - access user profile data
 */
object UserTargetKey : StringSetSourceKey<MockVegasDataSource> {
    override fun resolve(dataSource: MockVegasDataSource): Set<String> {
        return dataSource.userTargets
    }
}

object UserDayKey : IntSourceKey<MockVegasDataSource> {
    override fun resolve(dataSource: MockVegasDataSource): Int {
        return dataSource.userDay
    }
}

object UserTrialStateKey : BooleanSourceKey<MockVegasDataSource> {
    override fun resolve(dataSource: MockVegasDataSource): Boolean {
        return dataSource.userTrialState
    }
}

object UserDaysSinceAccountCreatedKey : IntSourceKey<MockVegasDataSource> {
    override fun resolve(dataSource: MockVegasDataSource): Int {
        return dataSource.userDaysSinceAccountCreated
    }
}

/**
 * Promotion history source keys - access promotion display history
 */
data class PromotionTimesShownKey(
    val historyType: String,
    val id: String
) : IntSourceKey<MockVegasDataSource> {
    override fun resolve(dataSource: MockVegasDataSource): Int {
        return dataSource.getPromotionTimesShown(historyType, id)
    }
}

data class PromotionDaysSinceLastShownKey(
    val historyType: String,
    val id: String
) : IntSourceKey<MockVegasDataSource> {
    override fun resolve(dataSource: MockVegasDataSource): Int {
        return dataSource.getPromotionDaysSinceLastShown(historyType, id)
    }
}

/**
 * Configuration source keys - access app configuration flags
 */
data class ConfigurationFlagKey(val key: String) : BooleanSourceKey<MockVegasDataSource> {
    override fun resolve(dataSource: MockVegasDataSource): Boolean {
        return dataSource.getConfiguration(key)
    }
}

/**
 * Survey history source keys - access survey display history
 */
data class SurveyDaysSinceLastShownKey(
    val surveyName: String,
    val stepName: String
) : IntSourceKey<MockVegasDataSource> {
    override fun resolve(dataSource: MockVegasDataSource): Int {
        return dataSource.getSurveyDaysSinceLastShown(surveyName, stepName)
    }
}
