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
 * the free charts, and unlocking is one tap against the local entitlement store. Phase 9
 * replaces the button with a real paywall — the shape of this card stays.
 */
@Composable
fun PremiumTeaser(
    feature: PremiumFeature,
    onUnlock: () -> Unit,
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
            text = "Unlock",
            onClick = onUnlock,
            modifier = Modifier.fillMaxWidth(),
        )
        Gap(Spacing.xs)
        Text(
            text = "Unlocked on this device. Play billing arrives with the store release.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
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
