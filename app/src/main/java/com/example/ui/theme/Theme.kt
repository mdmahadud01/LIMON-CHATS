package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.model.AccentColorOption
import com.example.model.AppearanceMode
import com.example.model.GlassStyleOption

private object NoDarkIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        object : Modifier.Node(), DrawModifierNode {
            override fun ContentDrawScope.draw() {
                drawContent()
            }
        }

    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = 0
}

@OptIn(ExperimentalMaterial3Api::class)
private val ZeroRippleConfiguration = RippleConfiguration(
    color = Color.Transparent,
    rippleAlpha = RippleAlpha(
        draggedAlpha = 0f,
        focusedAlpha = 0f,
        hoveredAlpha = 0f,
        pressedAlpha = 0f
    )
)

data class LiquidGlassColors(
    val isDark: Boolean,
    val glassStyle: GlassStyleOption = GlassStyleOption.IOS_27_DEFAULT,
    val glassSurface: Color,
    val glassSurfaceElevated: Color,
    val glassBorder: Color,
    val glassHighlight: Color,
    val smokyOverlay: Color = SmokyGlassOverlay,
    val smokyScrim: Color = SmokyGlassScrim,
    val sentBubble: Color,
    val sentBubbleStart: Color = sentBubble,
    val sentBubbleEnd: Color = sentBubble,
    val sentBubbleText: Color,
    val receivedBubble: Color,
    val receivedBubbleText: Color,
    val accent: Color,
    val accentSecondary: Color,
    val backgroundTop: Color,
    val backgroundMiddle: Color,
    val backgroundBottom: Color
)

val LocalLiquidGlassColors = staticCompositionLocalOf {
    LiquidGlassColors(
        isDark = false,
        glassStyle = GlassStyleOption.IOS_27_DEFAULT,
        glassSurface = SmokyGlassSurface,
        glassSurfaceElevated = SmokyGlassSurfaceElevated,
        glassBorder = SmokyGlassBorder,
        glassHighlight = SmokyGlassHighlight,
        smokyOverlay = SmokyGlassOverlay,
        smokyScrim = SmokyGlassScrim,
        sentBubble = SentBubbleLight,
        sentBubbleStart = Color(0xFF22C55E),
        sentBubbleEnd = SentBubbleLightEnd,
        sentBubbleText = Color.White,
        receivedBubble = Color(0xEBFFFFFF),
        receivedBubbleText = Color(0xFF0F172A),
        accent = CitrusEmerald,
        accentSecondary = OceanCyan,
        backgroundTop = Color(0xFFE5F4EE),
        backgroundMiddle = PearlBase,
        backgroundBottom = Color(0xFFE2ECF8)
    )
}

val LimonShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LimonChatTheme(
    appearanceMode: AppearanceMode = AppearanceMode.LIQUID_GLASS,
    accentOption: AccentColorOption = AccentColorOption.CITRUS_EMERALD,
    glassStyle: GlassStyleOption = GlassStyleOption.IOS_27_DEFAULT,
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (appearanceMode) {
        AppearanceMode.LIQUID_GLASS -> false
        AppearanceMode.DARK -> true
        AppearanceMode.LIGHT -> false
        AppearanceMode.SYSTEM -> systemDark
    }

    val (primaryAccent, secondaryAccent) = when (accentOption) {
        AccentColorOption.CITRUS_EMERALD -> CitrusEmerald to CitrusLime
        AccentColorOption.OCEAN_CYAN -> OceanCyan to CitrusGlow
        AccentColorOption.INDIGO_FROST -> IndigoFrost to OceanCyan
        AccentColorOption.CORAL_SUNSET -> CoralSunset to WarningAmber
    }

    val colorScheme = if (useDark) {
        darkColorScheme(
            primary = primaryAccent,
            onPrimary = Color.White,
            primaryContainer = primaryAccent.copy(alpha = 0.25f),
            onPrimaryContainer = Color.White,
            secondary = secondaryAccent,
            onSecondary = Color(0xFF062018),
            background = ObsidianBase,
            onBackground = Color.White,
            surface = ObsidianElevated,
            onSurface = Color.White,
            surfaceVariant = Color(0xFF192434),
            onSurfaceVariant = Color(0xFFE2E8F0),
            outline = Color(0x4DFFFFFF),
            error = DestructiveRed,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = primaryAccent,
            onPrimary = Color.White,
            primaryContainer = primaryAccent.copy(alpha = 0.14f),
            onPrimaryContainer = Color(0xFF064E3B),
            secondary = secondaryAccent,
            onSecondary = Color.White,
            background = PearlBase,
            onBackground = Color(0xFF0F172A),
            surface = PearlElevated,
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFE2E8F0),
            onSurfaceVariant = Color(0xFF475569),
            outline = Color(0x260F172A),
            error = DestructiveRed,
            onError = Color.White
        )
    }

    val isWaterGlass = glassStyle == GlassStyleOption.WATER_GLASS
    val glassColors = if (useDark) {
        LiquidGlassColors(
            isDark = true,
            glassStyle = glassStyle,
            glassSurface = if (isWaterGlass) Color(0x2E1A283A) else Color(0xCC121B28),
            glassSurfaceElevated = if (isWaterGlass) Color(0x4524364E) else Color(0xDE1B293D),
            glassBorder = if (isWaterGlass) Color(0x70FFFFFF) else Color(0x40FFFFFF),
            glassHighlight = if (isWaterGlass) Color(0x90FFFFFF) else Color(0x30FFFFFF),
            smokyOverlay = if (isWaterGlass) Color(0x66121C2B) else Color(0xF0151F2E),
            smokyScrim = Color(0x99020617),
            sentBubble = SentBubbleDark,
            sentBubbleStart = Color(0xFF16A34A),
            sentBubbleEnd = Color(0xFF065F46),
            sentBubbleText = Color.White,
            receivedBubble = if (isWaterGlass) Color(0x451A2638) else ReceivedBubbleDark,
            receivedBubbleText = Color.White,
            accent = primaryAccent,
            accentSecondary = secondaryAccent,
            backgroundTop = ObsidianBase,
            backgroundMiddle = ObsidianBase,
            backgroundBottom = Color(0xFF0A101A)
        )
    } else {
        val isSmokyLiquid = appearanceMode == AppearanceMode.LIQUID_GLASS
        LiquidGlassColors(
            isDark = false,
            glassStyle = glassStyle,
            glassSurface = when {
                isWaterGlass -> Color(0x34FFFFFF)
                isSmokyLiquid -> SmokyGlassSurface
                else -> Color(0xCCFFFFFF)
            },
            glassSurfaceElevated = when {
                isWaterGlass -> Color(0x4EFFFFFF)
                isSmokyLiquid -> SmokyGlassSurfaceElevated
                else -> Color(0xEEFFFFFF)
            },
            glassBorder = if (isWaterGlass) Color(0xDEFFFFFF) else SmokyGlassBorder,
            glassHighlight = if (isWaterGlass) Color(0xFAFFFFFF) else SmokyGlassHighlight,
            smokyOverlay = if (isWaterGlass) Color(0x6DF8FAFC) else SmokyGlassOverlay,
            smokyScrim = SmokyGlassScrim,
            sentBubble = SentBubbleLight,
            sentBubbleStart = Color(0xFF22C55E),
            sentBubbleEnd = SentBubbleLightEnd,
            sentBubbleText = Color.White,
            receivedBubble = if (isWaterGlass) Color(0x4EFFFFFF) else Color(0xEDFFFFFF),
            receivedBubbleText = Color(0xFF0F172A),
            accent = primaryAccent,
            accentSecondary = secondaryAccent,
            backgroundTop = when {
                isWaterGlass -> Color(0xFFD5F3E8)
                isSmokyLiquid -> Color(0xFFE4F4ED)
                else -> Color(0xFFDFF7EE)
            },
            backgroundMiddle = PearlBase,
            backgroundBottom = when {
                isWaterGlass -> Color(0xFFD4E6FD)
                isSmokyLiquid -> Color(0xFFE1EAF7)
                else -> Color(0xFFE0ECFF)
            }
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !useDark
                isAppearanceLightNavigationBars = !useDark
            }
        }
    }

    CompositionLocalProvider(
        LocalLiquidGlassColors provides glassColors,
        LocalIndication provides NoDarkIndication,
        LocalRippleConfiguration provides ZeroRippleConfiguration
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = limonTypography(fontScale),
            shapes = LimonShapes,
            content = content
        )
    }
}
