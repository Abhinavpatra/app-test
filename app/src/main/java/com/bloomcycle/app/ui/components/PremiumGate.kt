package com.bloomcycle.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bloomcycle.app.domain.model.PremiumFeature
import com.bloomcycle.app.ui.theme.EyebrowStyle
import com.bloomcycle.app.ui.theme.Spacing

/**
 * The teaser where a Premium feature is locked.
 *
 * Deliberately not a wall: it says exactly what is inside, the card around it still shows
 * the free charts, and the button opens the paywall — unlocking happens there, so this
 * card never claims a purchase it has not made.
 */
@Composable
fun PremiumTeaser(
    feature: PremiumFeature,
    onOpenPaywall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        contentPadding = PaddingValues(Spacing.md),
    ) {
        PremiumBadge()
        Gap(Spacing.xs)
        Text(text = feature.title, style = MaterialTheme.typography.titleMedium)
        Gap(Spacing.xxxs)
        Text(
            text = feature.blurb,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Gap(Spacing.sm)
        PrimaryButton(
            text = "Open Premium",
            onClick = onOpenPaywall,
            modifier = Modifier.fillMaxWidth(),
        )
        Gap(Spacing.xs)
        Text(
            text = "Logging, calendar, charts and reminders stay free — Premium only adds " +
                "these extra readings.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * One call site for every gated feature: the real content when it is unlocked, the teaser
 * when it is not. Every premium screen goes through this so a feature can never be
 * *silently* unavailable — it is either usable or visibly one tap from the paywall.
 */
@Composable
fun PremiumGate(
    feature: PremiumFeature,
    unlocked: Boolean,
    onOpenPaywall: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (unlocked) {
        content()
    } else {
        PremiumTeaser(feature = feature, onOpenPaywall = onOpenPaywall, modifier = modifier)
    }
}

/** The small "this is Premium" marker — words, never colour alone. */
@Composable
fun PremiumBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = "PREMIUM",
            style = EyebrowStyle,
            modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xxxs),
        )
    }
}
