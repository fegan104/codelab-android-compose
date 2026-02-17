/*
 * Copyright 2022 The Android Open Source Project
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

package com.example.compose.rally

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.compose.rally.ui.accounts.AccountsScreen
import com.example.compose.rally.ui.accounts.SingleAccountScreen
import com.example.compose.rally.ui.bills.BillsScreen
import com.example.compose.rally.ui.controlpanel.ControlPanelScreen
import com.example.compose.rally.ui.jsoninput.JsonInputScreen
import com.example.compose.rally.ui.overview.OverviewScreen

/**
 * Contract for information needed on every Rally navigation destination
 */
interface RallyDestination {
    val icon: ImageVector
    val route: String
    val screen: @Composable () -> Unit
}

class JsonInput(private val viewModel: RallyViewModel) : RallyDestination {
    override val icon = Icons.Filled.Edit
    override val route = "json input"
    override val screen: @Composable () -> Unit = {
        JsonInputScreen(viewModel)
    }
}

class ControlPanel(private val viewModel: RallyViewModel) : RallyDestination {
    override val icon = Icons.Filled.Tune
    override val route = "control panel"
    override val screen: @Composable () -> Unit = {
        ControlPanelScreen(
            entries = viewModel.sourceKeyEntries,
            dataSource = viewModel.dataSource,
            onResetAll = { viewModel.resetAllValues() }
        )
    }
}

// Screens to be displayed in the top RallyTabRow
// Note: JsonInput and ControlPanel require a viewModel parameter, so they're added dynamically
val rallyTabRowScreens = emptyList<RallyDestination>()
