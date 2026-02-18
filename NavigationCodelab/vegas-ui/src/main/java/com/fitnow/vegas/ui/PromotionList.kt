package com.fitnow.vegas.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitnow.vegas.core.Promotion
import com.fitnow.vegas.core.QueryDataSource
import com.fitnow.vegas.core.commonRulesPassed
import com.fitnow.vegas.core.promotionRulesPassed

/**
 * Displays a list of promotions with pass/fail indicators.
 * Clicking on a promotion expands it to show all its creatives.
 */
@Composable
fun <D : QueryDataSource> PromotionList(
    vegasPromoter: VegasPromoter<D>,
    dataSource: D,
    clickListener: PromotionCreativeClickListener,
    modifier: Modifier = Modifier,
) {
    val promotions = vegasPromoter.promotions

    Column(modifier = modifier) {
        promotions.forEachIndexed { index, promotion ->
            PromotionListItem(
                promotion = promotion,
                dataSource = dataSource,
                vegasPromoter = vegasPromoter,
                clickListener = clickListener,
            )
            if (index < promotions.lastIndex) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Displays a single promotion with a pass/fail indicator.
 * Clicking expands to show all creatives.
 */
@Composable
fun <D : QueryDataSource> PromotionListItem(
    promotion: Promotion<D>,
    dataSource: D,
    vegasPromoter: VegasPromoter<D>,
    clickListener: PromotionCreativeClickListener,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var rulesPassed by remember { mutableStateOf<Boolean?>(null) }

    // Evaluate rules for this promotion (both common rules and promotion-specific rules)
    // Re-evaluate when dataSource.version changes (e.g., control panel updates)
    LaunchedEffect(promotion, dataSource.version) {
        rulesPassed = try {
            val group = vegasPromoter.getPromotionGroup()
            
            // Check common rules first
            val commonRulesPassed = dataSource.commonRulesPassed(group)
            
            // Then check promotion-specific rules
            val promotionRulesPassed = dataSource.promotionRulesPassed(group, promotion)
            
            commonRulesPassed && promotionRulesPassed
        } catch (_: Exception) {
            false
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = 2.dp,
    ) {
        Column {
            // Header row with promotion info and pass/fail indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Pass/Fail indicator
                RuleStatusIndicator(
                    passed = rulesPassed,
                    modifier = Modifier.size(24.dp),
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Promotion info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = promotion.id,
                        style = MaterialTheme.typography.subtitle1.copy(
                            fontWeight = FontWeight.Bold
                        ),
                    )
                    promotion.category?.let { category ->
                        Text(
                            text = category,
                            style = MaterialTheme.typography.caption,
                            color = Color.Gray,
                        )
                    }
                    Text(
                        text = "${promotion.creativeTreatments.size} creative(s) • ${promotion.rules.size} rule(s)",
                        style = MaterialTheme.typography.caption,
                        color = Color.Gray,
                    )
                }

                // Expand/collapse indicator
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colors.primary,
                )
            }

            // Expanded content showing creatives
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                ) {
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Creatives",
                        style = MaterialTheme.typography.subtitle2.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    promotion.creativeTreatments.forEachIndexed { index, creative ->
                        val response = VegasResponse(
                            group = vegasPromoter.getPromotionGroup(),
                            promotion = promotion,
                            creative = creative,
                        )

                        CreativeResponse(
                            selection = response,
                            clickListener = clickListener,
                            modifier = Modifier.padding(bottom = if (index < promotion.creativeTreatments.lastIndex) 8.dp else 0.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Visual indicator showing whether rules passed or failed.
 */
@Composable
fun RuleStatusIndicator(
    passed: Boolean?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = when (passed) {
            true -> Color(0xFF4CAF50) // Green
            false -> Color(0xFFF44336) // Red
            null -> Color.Gray // Loading/Unknown
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (passed) {
                true -> Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Rules passed",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
                false -> Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Rules failed",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
                null -> {
                    // Loading state - just show gray circle
                }
            }
        }
    }
}
