package com.fitnow.vegas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.fitnow.vegas.core.CardType.Banner
import com.fitnow.vegas.core.CardType.FullHeight
import com.fitnow.vegas.core.Creative
import com.fitnow.vegas.core.QueryDataSource

/**
 * A composable that displays a [Creative] in a card format with the relevant
 * text content and action buttons.
 *
 * @param vegasPromoter The [VegasPromoter] instance to use for displaying the creative.
 * @param modifier Modifier for the card
 * @param clickListener Callback interface for card interactions
 */
@Composable
fun <D : QueryDataSource> PromotionCreative(
    vegasPromoter: VegasPromoter<D>,
    clickListener: PromotionCreativeClickListener,
    modifier: Modifier = Modifier,
) {
    val response by vegasPromoter.currentPromotion.collectAsState(null)

    response?.let { selection ->
        val (group, promo, creative) = selection

        when (group.cardType) {
            Banner -> {
                PromotionCreativeBanner(
                    creative = creative,
                    isDismissible = promo.isDismissible,
                    modifier = modifier,
                    onShown = {
                        clickListener.onShown(selection)
                    },
                    onActionClick = {
                        clickListener.onOpenAction(promo.actionUrl)
                    },
                )
            }

            FullHeight -> {
                PromotionCreativeFullHeight(
                    creative = creative,
                    isDismissible = promo.isDismissible,
                    modifier = modifier,
                    onShown = {
                        clickListener.onShown(selection)
                    },
                    onActionClick = {
                        clickListener.onOpenAction(promo.actionUrl)
                    },
                    onDismissClick = {
                        vegasPromoter.onDismiss()
                        clickListener.onDismiss()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun PromotionCreativeBanner(
    creative: Creative,
    modifier: Modifier = Modifier,
    isDismissible: Boolean = false,
    onActionClick: () -> Unit = {},
    onDismissClick: () -> Unit = {},
    onShown: () -> Unit = {},
) {
    LaunchedEffect(creative.id) {
        onShown()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = { onActionClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = 2.dp
    ) {
        if (isDismissible) {
            // Dismissible layout with X button in upper right
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Text content
                    creative.bodyText?.let { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.body1.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 8.dp, bottom = 16.dp)
                        )
                    }

                    // Close button (X)
                    IconButton(
                        onClick = onDismissClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = creative.noThanksText ?: "Close",
                            tint = Color.Gray
                        )
                    }
                }
            }
        } else {
            // Standard layout with vertically centered arrow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                creative.bodyText?.let { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.body1.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = creative.buttonText.orEmpty(),
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/**
 * A composable that displays a [Creative] in a card format with the relevant
 * text content and action buttons.
 *
 * @param creative The creative data to display
 * @param modifier Modifier for the card
 * @param isDismissible Whether to show the close/dismiss button
 * @param onActionClick Callback when the primary action button is clicked
 * @param onDismissClick Callback when the dismiss/no thanks button is clicked
 */
@Composable
private fun PromotionCreativeFullHeight(
    creative: Creative,
    modifier: Modifier = Modifier,
    isDismissible: Boolean = true,
    onActionClick: () -> Unit = {},
    onDismissClick: () -> Unit = {},
    onShown: () -> Unit = {},
) {
    LaunchedEffect(creative.id) {
        onShown()
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (creative.cardTitle != null) {
                    Text(
                        text = creative.cardTitle!!.uppercase(),
                        style = MaterialTheme.typography.subtitle2.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        ),
                    )
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                if (isDismissible) {
                    IconButton(
                        onClick = onDismissClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = creative.noThanksText ?: "Close",
                            tint = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.Top
            ) {
                creative.imageUrl?.let { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    creative.titleText?.let { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.h6.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    creative.bodyText?.let { body ->
                        Text(
                            text = body,
                            style = MaterialTheme.typography.body1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer: Action Button
            creative.buttonText?.let { buttonText ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onActionClick,
                        elevation = ButtonDefaults.elevation(0.dp, 0.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = buttonText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PromotionCreativeFullHeightPreview() {
    MaterialTheme {
        PromotionCreativeFullHeight(
            creative = Creative(
                id = "preview-creative",
                imageUrl = "https://picsum.photos/400/200",
                titleText = "Free Nutrition Coaching",
                bodyText = "Reach your goals faster with 1-on-1 help from a certified dietitian, covered by insurance.",
                cardTitle = "Expert Guidance",
                buttonText = "Learn More",
                noThanksText = "No Thanks"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PromotionCreativeFullHeightMinimalPreview() {
    MaterialTheme {
        PromotionCreativeFullHeight(
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

@Preview(showBackground = true)
@Composable
private fun PromotionCreativeFullHeightNonDismissiblePreview() {
    MaterialTheme {
        PromotionCreativeFullHeight(
            creative = Creative(
                id = "non-dismissible-creative",
                imageUrl = "https://picsum.photos/400/200",
                titleText = "Important Update",
                bodyText = "This promotion cannot be dismissed until you take action.",
                cardTitle = "Action Required",
                buttonText = "Take Action"
            ),
            isDismissible = false,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PromotionCreativeBannerPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            PromotionCreativeBanner(
                creative = Creative(
                    id = "banner-creative",
                    bodyText = "Exclusive Black Friday Deals for Lifetime Members - Limited Time Only!",
                    buttonText = "Learn More"
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            PromotionCreativeBanner(
                creative = Creative(
                    id = "banner-creative-dismissible",
                    bodyText = "Exclusive Black Friday Deals for Lifetime Members - Limited Time Only!",
                    buttonText = "Learn More",
                    noThanksText = "Close"
                ),
                isDismissible = true
            )
        }
    }
}
