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

package com.fitnow.vegas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.fitnow.vegas.core.Creative

/**
 * A composable that displays a [Creative] in a card format with the relevant
 * text content and action buttons.
 *
 * @param creative The creative data to display
 * @param modifier Modifier for the card
 * @param onActionClick Callback when the primary action button is clicked
 * @param onDismissClick Callback when the dismiss/no thanks button is clicked
 */
@Composable
fun PromotionCreative(
    creative: Creative,
    modifier: Modifier = Modifier,
    onActionClick: () -> Unit = {},
    onDismissClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column {
            // Hero image (if available)
            creative.heroImageUrl?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Title text
                creative.titleText?.let { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.h6
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Body text
                creative.bodyText?.let { body ->
                    Text(
                        text = body,
                        style = MaterialTheme.typography.body1
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Action text (supplementary text, if different from body)
                creative.actionText?.let { action ->
                    Text(
                        text = action,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Buttons row
                if (creative.buttonText != null || creative.noThanksText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        // Dismiss button
                        creative.noThanksText?.let { dismissText ->
                            TextButton(onClick = onDismissClick) {
                                Text(text = dismissText)
                            }
                        }

                        // Primary action button
                        creative.buttonText?.let { buttonText ->
                            Button(onClick = onActionClick) {
                                Text(text = buttonText)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PromotionCreativePreview() {
    MaterialTheme {
        PromotionCreative(
            creative = Creative(
                id = "preview-creative",
                heroImageUrl = "https://picsum.photos/400/200",
                titleText = "Special Offer!",
                bodyText = "Get 50% off your first month of premium membership.",
                actionText = "Limited time only",
                buttonText = "Claim Now",
                noThanksText = "No Thanks"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PromotionCreativeMinimalPreview() {
    MaterialTheme {
        PromotionCreative(
            creative = Creative(
                id = "minimal-creative",
                titleText = "Quick Update",
                bodyText = "We've improved your experience!",
                buttonText = "Got it"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
