package com.bloomcycle.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bloomcycle.app.domain.model.EntitlementState
import com.bloomcycle.app.domain.model.PremiumFeature
import com.bloomcycle.app.ui.LocalNavController
import com.bloomcycle.app.ui.components.Gap
import com.bloomcycle.app.ui.components.PrimaryButton
import com.bloomcycle.app.ui.components.PremiumBadge
import com.bloomcycle.app.ui.components.SectionHeader
import com.bloomcycle.app.ui.components.SoftCard
import com.bloomcycle.app.ui.navigation.BloomDestination
import com.bloomcycle.app.ui.rememberAppContainer
import com.bloomcycle.app.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * Phase 9 — the one place Premium is explained and unlocked.
 *
 * The layout *is* the policy (plan.md Phase 9 acceptance, risk R2):
 *  - the free tier is stated first, so nothing reads as a bait-and-switch;
 *  - every feature is listed with its real description, unlocked or not — nothing is
 *    silently unavailable;
 *  - no countdown, no fake scarcity, no pre-ticked bundles, no subscription framing;
 *  - v1 takes no payment, so the copy says exactly that instead of showing a price.
 */
@Composable
fun PaywallScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val container = rememberAppContainer()
    val scope = rememberCoroutineScope()
    val navController = LocalNavController.current
    val entitlements by container.entitlements.state
        .collectAsStateWithLifecycle(initialValue = EntitlementState())
    val unlocked = entitlements.isDebugUnlocked || entitlements.unlocked.isNotEmpty()

    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.Start)
                    .defaultMinSize(minHeight = 48.dp)
                    .semantics { contentDescription = "Back" },
            ) {
                Text("Back")
            }

            SectionHeader(eyebrow = "Premium", title = "A few extra readings")

            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Logging, calendar, charts and reminders are free and stay free — " +
                        "nothing in Bloom's core loop is behind this screen. Premium adds five " +
                        "optional readings on top. There is no subscription and nothing to " +
                        "cancel: unlocking a reading is permanent, and you can come back and " +
                        "change your mind at any point.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            PremiumFeature.entries.forEach { feature ->
                FeatureCard(
                    feature = feature,
                    unlocked = entitlements.isUnlocked(feature),
                    onUnlock = {
                        scope.launch { container.entitlements.unlock(feature) }
                    },
                )
            }

            // The readings are one tap away whether or not anything is unlocked yet — each
            // section gates itself, so this is never a dead end.
            PrimaryButton(
                text = "Open the readings",
                onClick = { navController?.navigate(BloomDestination.READINGS) },
                modifier = Modifier.fillMaxWidth(),
            )

            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No payment is taken today — Play billing arrives with the store " +
                        "release, and until then unlocks are saved on this device only. Bloom " +
                        "is not medical advice, and none of these readings replaces talking " +
                        "to a clinician.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (unlocked) {
                Gap(Spacing.xxs)
                Text(
                    text = "Everything is unlocked on this device.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(
    feature: PremiumFeature,
    unlocked: Boolean,
    onUnlock: () -> Unit,
) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
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
        if (unlocked) {
            Text(
                text = "Unlocked",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { contentDescription = "Unlocked" },
            )
        } else {
            PrimaryButton(
                text = "Unlock",
                onClick = onUnlock,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
