package com.bloomcycle.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * "Pressed petals on warm paper."
 *
 * The direction is botanical-editorial rather than wellness-app pastel: parchment grounds,
 * warm ink instead of black, and a dried-rose primary that reads as feminine without
 * reaching for pink-on-white. Green carries the brand, rose carries menstruation — so the
 * two most load-bearing meanings in the product never collide.
 */

// --- Neutrals: paper, not white --------------------------------------------------------
val Paper = Color(0xFFF6F0E7)
val PaperRaised = Color(0xFFFCF8F2)
val PaperSunken = Color(0xFFEFE7DA)
val Ink = Color(0xFF231B18)
val InkSoft = Color(0xFF6D6058)
val InkFaint = Color(0xFF9A8C81)
val Line = Color(0xFFE4DACC)
val LineStrong = Color(0xFFC8BCAA)

// --- Accents ---------------------------------------------------------------------------
val DriedRose = Color(0xFF8C5A60)
val DriedRoseLight = Color(0xFFD8A3A7)
val DriedRoseContainer = Color(0xFFF2DFDD)
val DriedRoseOnContainer = Color(0xFF3C1F23)

val Sage = Color(0xFF6E7A5E)
val SageContainer = Color(0xFFE2E7D6)
val SageOnContainer = Color(0xFF28301F)

val Terracotta = Color(0xFFB4744A)
val TerracottaContainer = Color(0xFFF5E3D4)
val TerracottaOnContainer = Color(0xFF3C2313)

val Oxblood = Color(0xFF9E4450)
val Moss = Color(0xFF6E8F6A)
val Amber = Color(0xFFC08A2E)
val Mauve = Color(0xFF7E6E86)

// --- Dark ground: warm charcoal, never blue-black --------------------------------------
val NightGround = Color(0xFF1A1513)
val NightRaised = Color(0xFF231D1A)
val NightSunken = Color(0xFF141010)
val NightInk = Color(0xFFEFE6DC)
val NightInkSoft = Color(0xFFB0A399)
val NightLine = Color(0xFF3B322D)

val OxbloodNight = Color(0xFFE1909A)
val MossNight = Color(0xFF9CBE96)
val AmberNight = Color(0xFFE0B968)
val MauveNight = Color(0xFFAFA0BB)

// --- Semantic phase colours -------------------------------------------------------------
// Colour alone never carries the phase — every surface pairs these with a word and a
// shape, so the meaning survives greyscale and colour-vision deficiency.
val PhaseColors = mapOf(
    com.bloomcycle.app.domain.model.PhaseType.MENSTRUAL to Oxblood,
    com.bloomcycle.app.domain.model.PhaseType.FOLLICULAR to Moss,
    com.bloomcycle.app.domain.model.PhaseType.OVULATORY to Amber,
    com.bloomcycle.app.domain.model.PhaseType.LUTEAL to Mauve,
)

val PhaseColorsDark = mapOf(
    com.bloomcycle.app.domain.model.PhaseType.MENSTRUAL to OxbloodNight,
    com.bloomcycle.app.domain.model.PhaseType.FOLLICULAR to MossNight,
    com.bloomcycle.app.domain.model.PhaseType.OVULATORY to AmberNight,
    com.bloomcycle.app.domain.model.PhaseType.LUTEAL to MauveNight,
)

// --- Schemes ------------------------------------------------------------------------------

internal val BloomLightScheme = lightColorScheme(
    primary = DriedRose,
    onPrimary = Color(0xFFFFFBF8),
    primaryContainer = DriedRoseContainer,
    onPrimaryContainer = DriedRoseOnContainer,
    secondary = Sage,
    onSecondary = Color(0xFFFFFBF8),
    secondaryContainer = SageContainer,
    onSecondaryContainer = SageOnContainer,
    tertiary = Terracotta,
    onTertiary = Color(0xFFFFFBF8),
    tertiaryContainer = TerracottaContainer,
    onTertiaryContainer = TerracottaOnContainer,
    error = Color(0xFF9C3B36),
    onError = Color(0xFFFFFBF8),
    background = Paper,
    onBackground = Ink,
    surface = PaperRaised,
    onSurface = Ink,
    surfaceVariant = PaperSunken,
    onSurfaceVariant = InkSoft,
    surfaceContainerLowest = PaperRaised,
    surfaceContainerLow = Paper,
    surfaceContainer = PaperSunken,
    surfaceContainerHigh = PaperSunken,
    surfaceContainerHighest = Line,
    outline = LineStrong,
    outlineVariant = Line,
    inverseSurface = Ink,
    inverseOnSurface = Paper,
    inversePrimary = DriedRoseLight,
    scrim = Color(0x66231B18),
)

internal val BloomDarkScheme = darkColorScheme(
    primary = DriedRoseLight,
    onPrimary = Color(0xFF402126),
    primaryContainer = Color(0xFF5A3238),
    onPrimaryContainer = Color(0xFFF6E0E1),
    secondary = Color(0xFFA9B495),
    onSecondary = Color(0xFF20271A),
    secondaryContainer = Color(0xFF39412F),
    onSecondaryContainer = Color(0xFFE1E7D6),
    tertiary = Color(0xFFD89C73),
    onTertiary = Color(0xFF361C0B),
    tertiaryContainer = Color(0xFF4E311F),
    onTertiaryContainer = Color(0xFFF5DFCE),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF5F1512),
    background = NightGround,
    onBackground = NightInk,
    surface = NightRaised,
    onSurface = NightInk,
    surfaceVariant = NightSunken,
    onSurfaceVariant = NightInkSoft,
    surfaceContainerLowest = NightSunken,
    surfaceContainerLow = NightGround,
    surfaceContainer = NightRaised,
    surfaceContainerHigh = NightRaised,
    surfaceContainerHighest = Color(0xFF2C2522),
    outline = Color(0xFF5A504A),
    outlineVariant = NightLine,
    inverseSurface = NightInk,
    inverseOnSurface = NightGround,
    inversePrimary = DriedRose,
    scrim = Color(0x99000000),
)
