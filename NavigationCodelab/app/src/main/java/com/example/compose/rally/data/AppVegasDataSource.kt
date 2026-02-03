/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
            UserKeys.UserStringSetSourceKey.Target -> setOf("foo", "bar")
        }
    }

    override fun fetchUserInt(source: UserSource, key: UserKeys.UserIntSourceKey): Int? {
        return when (key) {
            UserKeys.UserIntSourceKey.Day -> LocalDate.now().dayOfYear
            UserKeys.UserIntSourceKey.DaysSinceAccountCreated -> 45
        }
    }

    override fun fetchUserBoolean(source: UserSource, key: UserKeys.UserBooleanSourceKey): Boolean? {
        return when (key) {
            UserKeys.UserBooleanSourceKey.TrialState -> true
        }
    }

    override fun fetchPromotionHistoryInt(
        source: PromotionHistorySource,
        key: PromotionHistoryKeys.PromotionHistoryIntSourceKey
    ): Int? {
        return when (key) {
            is PromotionHistoryKeys.PromotionHistoryIntSourceKey.TimesShown -> {
                // Access where clause parameters: key.historyType, key.id
                when {
                    key.historyType == "group" && key.id == "android-halloween-sale-premium" -> 1
                    else -> 0
                }
            }
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
