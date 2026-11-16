package com.example.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.ChatWallpaperPreset
import com.example.model.DeliveryStatus
import com.example.model.GlassStyleOption
import kotlin.math.roundToInt

fun Modifier.iosTap(
    enabled: Boolean = true,
    pressedScale: Float = 0.94f,
    pressedAlpha: Float = 0.78f,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ios_tap_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (enabled && isPressed) pressedAlpha else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "ios_tap_alpha"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick
        )
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.iosCombinedTap(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    pressedAlpha: Float = 0.85f,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.74f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ios_combined_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (enabled && isPressed) pressedAlpha else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "ios_combined_alpha"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
            onLongClick = onLongClick
        )
}

@Composable
fun IosGlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .iosTap(
                enabled = enabled,
                pressedScale = 0.88f,
                pressedAlpha = 0.68f,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    animateOrbs: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    val infinite = rememberInfiniteTransition(label = "glass_bg")
    val drift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_drift"
    )
    val t = if (animateOrbs) drift else 0.35f

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(glass.backgroundTop, glass.backgroundMiddle, glass.backgroundBottom)
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glass.accent.copy(alpha = if (glass.isDark) 0.24f else 0.18f), Color.Transparent),
                    center = Offset(size.width * (0.18f + 0.12f * t), size.height * (0.12f + 0.05f * t)),
                    radius = size.width * 0.72f
                ),
                radius = size.width * 0.72f,
                center = Offset(size.width * (0.18f + 0.12f * t), size.height * (0.12f + 0.05f * t))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glass.accentSecondary.copy(alpha = if (glass.isDark) 0.18f else 0.14f), Color.Transparent),
                    center = Offset(size.width * (0.85f - 0.12f * t), size.height * (0.82f - 0.06f * t)),
                    radius = size.width * 0.65f
                ),
                radius = size.width * 0.65f,
                center = Offset(size.width * (0.85f - 0.12f * t), size.height * (0.82f - 0.06f * t))
            )
        }
        content()
    }
}

fun chatWallpaperStops(preset: ChatWallpaperPreset, isDark: Boolean): List<Color> = when (preset) {
    ChatWallpaperPreset.PASTEL_MINT -> if (isDark) {
        listOf(Color(0xFF061E19), Color(0xFF0A1B24), Color(0xFF091426))
    } else {
        listOf(Color(0xFFE3FAEF), Color(0xFFEEFBF8), Color(0xFFE2F1FF))
    }
    ChatWallpaperPreset.LAVENDER_SKY -> if (isDark) {
        listOf(Color(0xFF15112B), Color(0xFF0C182E), Color(0xFF081C2C))
    } else {
        listOf(Color(0xFFF0EBFF), Color(0xFFECF3FF), Color(0xFFE4F8FF))
    }
    ChatWallpaperPreset.WARM_PEACH -> if (isDark) {
        listOf(Color(0xFF241216), Color(0xFF1B1424), Color(0xFF0E1524))
    } else {
        listOf(Color(0xFFFFEFE6), Color(0xFFFFF5ED), Color(0xFFFCEBFF))
    }
    ChatWallpaperPreset.MIDNIGHT_AURORA -> if (isDark) {
        listOf(Color(0xFF041D1B), Color(0xFF08162C), Color(0xFF130F2A))
    } else {
        listOf(Color(0xFFD9FCE7), Color(0xFFE0F2FE), Color(0xFFEDE9FE))
    }
    ChatWallpaperPreset.CUSTOM -> if (isDark) {
        listOf(Color(0xFF061E19), Color(0xFF0A1B24), Color(0xFF091426))
    } else {
        listOf(Color(0xFFE3FAEF), Color(0xFFEEFBF8), Color(0xFFE2F1FF))
    }
}

@Composable
fun ChatWallpaperBackground(
    preset: ChatWallpaperPreset,
    customWallpaperUri: String?,
    blurEnabled: Boolean,
    animateOrbs: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    val infinite = rememberInfiniteTransition(label = "chat_wallpaper_anim")
    val drift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wallpaper_drift"
    )
    val t = if (animateOrbs) drift else 0.35f
    val gradientStops = remember(preset, glass.isDark) { chatWallpaperStops(preset, glass.isDark) }
    val blurModifier = if (blurEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(radius = 20.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
    } else {
        Modifier
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().then(blurModifier)) {
            if (preset == ChatWallpaperPreset.CUSTOM && !customWallpaperUri.isNullOrBlank()) {
                AsyncImage(
                    model = customWallpaperUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(brush = Brush.verticalGradient(colors = gradientStops))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glass.accent.copy(alpha = if (glass.isDark) 0.22f else 0.16f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * (0.16f + 0.14f * t), size.height * (0.14f + 0.06f * t)),
                            radius = size.width * 0.78f
                        ),
                        radius = size.width * 0.78f,
                        center = Offset(size.width * (0.16f + 0.14f * t), size.height * (0.14f + 0.06f * t))
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glass.accentSecondary.copy(alpha = if (glass.isDark) 0.18f else 0.14f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * (0.86f - 0.14f * t), size.height * (0.84f - 0.06f * t)),
                            radius = size.width * 0.68f
                        ),
                        radius = size.width * 0.68f,
                        center = Offset(size.width * (0.86f - 0.14f * t), size.height * (0.84f - 0.06f * t))
                    )
                }
            }
        }
        if (preset == ChatWallpaperPreset.CUSTOM && !customWallpaperUri.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (glass.isDark) {
                            Color.Black.copy(alpha = if (blurEnabled) 0.42f else 0.30f)
                        } else {
                            Color.White.copy(alpha = if (blurEnabled) 0.36f else 0.20f)
                        }
                    )
            )
        } else if (blurEnabled && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (glass.isDark) Color.Black.copy(alpha = 0.18f)
                        else Color.White.copy(alpha = 0.18f)
                    )
            )
        }
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(13.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    val isWaterGlass = glass.glassStyle == GlassStyleOption.WATER_GLASS
    val fill = if (elevated) glass.glassSurfaceElevated else glass.glassSurface
    val fillBrush = if (isWaterGlass) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = if (glass.isDark) 0.14f else 0.42f),
                fill,
                Color.White.copy(alpha = if (glass.isDark) 0.05f else 0.16f)
            )
        )
    } else {
        Brush.verticalGradient(colors = listOf(fill, fill))
    }
    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            glass.glassHighlight.copy(alpha = if (glass.isDark) 0.52f else 0.98f),
            glass.glassBorder.copy(alpha = if (isWaterGlass) 0.72f else if (glass.isDark) 0.16f else 0.55f)
        )
    )
    val interactive = onLongClick != null || onClick != null
    val clickMod = if (interactive) {
        Modifier.iosCombinedTap(
            pressedScale = 0.975f,
            pressedAlpha = 0.88f,
            onClick = { onClick?.invoke() },
            onLongClick = onLongClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isWaterGlass) 4.dp else if (elevated) 14.dp else 6.dp,
                shape = shape,
                ambientColor = glass.accent.copy(alpha = if (isWaterGlass) 0.06f else 0.12f),
                spotColor = Color(if (isWaterGlass) 0x160F172A else 0x2E0F172A)
            )
            .clip(shape)
            .background(fillBrush)
            .border(BorderStroke(if (isWaterGlass) 1.2.dp else 1.dp, borderBrush), shape)
            .then(clickMod)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun GlassConfirmDialog(
    onDismissRequest: () -> Unit,
    title: String,
    body: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null
) {
    val glass = LocalLiquidGlassColors.current
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(glass.smokyScrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                )
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(26.dp),
                color = glass.smokyOverlay,
                shadowElevation = 20.dp,
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            glass.glassHighlight.copy(alpha = if (glass.isDark) 0.38f else 0.95f),
                            glass.glassBorder.copy(alpha = if (glass.isDark) 0.18f else 0.55f)
                        )
                    )
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    body()
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (dismissButton != null) {
                            dismissButton()
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        confirmButton()
                    }
                }
            }
        }
    }
}

data class NavTabItem(
    val id: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun FloatingGlassNavBar(
    items: List<NavTabItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlassColors.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val selectedIndex = items.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    val isWaterGlass = glass.glassStyle == GlassStyleOption.WATER_GLASS

    val barSurfaceColor = when {
        isWaterGlass && glass.isDark -> Color(0x36141E2B)
        isWaterGlass -> Color(0x3BFFFFFF)
        glass.isDark -> Color(0xDC141E2B)
        else -> Color(0xE8F8FAFD)
    }
    val activePillColor = when {
        isWaterGlass && glass.isDark -> Color(0x520A84FF)
        isWaterGlass -> Color(0x47FFFFFF)
        glass.isDark -> Color(0x360A84FF)
        else -> Color(0xFFE0EDFF)
    }
    val activeContentColor = if (glass.isDark) Color(0xFF4BA3FF) else Color(0xFF007AFF)
    val inactiveContentColor = if (glass.isDark) Color.White else Color(0xFF1F242D)
    val chatsBlueBadge = Color(0xFF0A84FF)
    val badgeRed = Color(0xFFFF3B30)

    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var dragHoveredIndex by remember { mutableIntStateOf(selectedIndex) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isWaterGlass) 8.dp else 20.dp,
                    shape = RoundedCornerShape(30.dp),
                    ambientColor = Color(0x1E007AFF),
                    spotColor = Color(if (isWaterGlass) 0x180F172A else 0x2E0F172A)
                ),
            shape = RoundedCornerShape(30.dp),
            color = barSurfaceColor,
            border = BorderStroke(
                width = if (isWaterGlass) 1.25.dp else 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (isWaterGlass) 0.96f else if (glass.isDark) 0.24f else 0.96f),
                        Color.White.copy(alpha = if (isWaterGlass) 0.52f else if (glass.isDark) 0.08f else 0.55f)
                    )
                )
            )
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 5.dp, vertical = 4.dp)
            ) {
                val count = items.size.coerceAtLeast(1)
                val tabWidth = maxWidth / count
                val tabWidthPx = with(density) { tabWidth.toPx() }.coerceAtLeast(1f)
                val maxTrackPx = ((count - 1) * tabWidthPx).coerceAtLeast(0f)

                val targetOffsetPx = if (isDragging) dragOffsetPx else selectedIndex * tabWidthPx
                val animatedOffsetPx by animateFloatAsState(
                    targetValue = targetOffsetPx,
                    animationSpec = if (isDragging) {
                        spring(dampingRatio = 0.92f, stiffness = Spring.StiffnessHigh)
                    } else {
                        spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow)
                    },
                    label = "nav_sliding_pill_px"
                )
                val pillScaleX by animateFloatAsState(
                    targetValue = if (isDragging) 1.15f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.64f, stiffness = Spring.StiffnessMedium),
                    label = "nav_pill_zoom_x"
                )
                val pillScaleY by animateFloatAsState(
                    targetValue = if (isDragging) 1.13f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.64f, stiffness = Spring.StiffnessMedium),
                    label = "nav_pill_zoom_y"
                )

                val dragGestureModifier = Modifier.pointerInput(items, selectedIndex, tabWidthPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val initialX = (offset.x - tabWidthPx / 2f).coerceIn(0f, maxTrackPx)
                            dragOffsetPx = initialX
                            val hovered = ((initialX + tabWidthPx / 2f) / tabWidthPx).toInt().coerceIn(0, count - 1)
                            dragHoveredIndex = hovered
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val nextX = (dragOffsetPx + dragAmount).coerceIn(0f, maxTrackPx)
                            dragOffsetPx = nextX
                            val hovered = ((nextX + tabWidthPx / 2f) / tabWidthPx).toInt().coerceIn(0, count - 1)
                            if (hovered != dragHoveredIndex) {
                                dragHoveredIndex = hovered
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onDragEnd = {
                            val targetIdx = ((dragOffsetPx + tabWidthPx / 2f) / tabWidthPx).toInt().coerceIn(0, count - 1)
                            isDragging = false
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (targetIdx in items.indices && items[targetIdx].id != selectedId) {
                                onSelect(items[targetIdx].id)
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(dragGestureModifier)
                ) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(animatedOffsetPx.roundToInt(), 0) }
                            .width(tabWidth)
                            .height(48.dp)
                            .padding(horizontal = 2.dp)
                            .graphicsLayer {
                                scaleX = pillScaleX
                                scaleY = pillScaleY
                            }
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = if (isDragging || isWaterGlass) {
                                        listOf(
                                            Color.White.copy(alpha = if (glass.isDark) 0.24f else 0.72f),
                                            activePillColor
                                        )
                                    } else {
                                        listOf(activePillColor, activePillColor)
                                    }
                                )
                            )
                            .border(
                                width = if (isDragging) 1.25.dp else 0.75.dp,
                                color = Color.White.copy(alpha = if (isDragging) 0.92f else if (glass.isDark) 0.16f else 0.76f),
                                shape = RoundedCornerShape(24.dp)
                            )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items.forEachIndexed { index, item ->
                            val selected = item.id == selectedId
                            val dragFocused = isDragging && dragHoveredIndex == index
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val contentColor by animateColorAsState(
                                targetValue = if (selected || dragFocused) activeContentColor else inactiveContentColor,
                                animationSpec = tween(200),
                                label = "nav_icon"
                            )
                            val iconScale by animateFloatAsState(
                                targetValue = when {
                                    dragFocused -> 1.15f
                                    isPressed -> 0.88f
                                    selected -> 1.07f
                                    else -> 1.0f
                                },
                                animationSpec = spring(
                                    dampingRatio = 0.66f,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "nav_scale"
                            )
                            val tabAlpha by animateFloatAsState(
                                targetValue = if (isPressed) 0.76f else 1f,
                                animationSpec = tween(100),
                                label = "nav_alpha"
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .minimumInteractiveComponentSize()
                                    .graphicsLayer { alpha = tabAlpha }
                                    .clip(RoundedCornerShape(24.dp))
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClick = {
                                            if (!selected) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onSelect(item.id)
                                            }
                                        }
                                    )
                                    .testTag("nav_tab_${item.id}")
                                    .padding(vertical = 4.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(width = 48.dp, height = 25.dp)
                                ) {
                                    if (item.id == "chats") {
                                        DualChatBubblesIcon(
                                            tint = contentColor,
                                            modifier = Modifier
                                                .size(23.dp)
                                                .graphicsLayer {
                                                    scaleX = iconScale
                                                    scaleY = iconScale
                                                }
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (selected || dragFocused) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label,
                                            tint = contentColor,
                                            modifier = Modifier
                                                .size(21.dp)
                                                .graphicsLayer {
                                                    scaleX = iconScale
                                                    scaleY = iconScale
                                                }
                                        )
                                    }
                                    if (item.badgeCount > 0) {
                                        val isChats = item.id == "chats"
                                        val pillBg = if (isChats) chatsBlueBadge else badgeRed
                                        val labelText = if (isChats) {
                                            if (item.badgeCount > 999) "999+" else item.badgeCount.toString()
                                        } else {
                                            if (item.badgeCount > 99) "99+" else item.badgeCount.toString()
                                        }
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 2.dp, y = (-4).dp)
                                                .defaultMinSize(minWidth = 18.dp, minHeight = 16.dp)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(pillBg)
                                                .border(
                                                    width = 1.4.dp,
                                                    color = if (selected || dragFocused) activePillColor else barSurfaceColor,
                                                    shape = RoundedCornerShape(9.dp)
                                                )
                                                .padding(horizontal = 4.dp, vertical = 1.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = labelText,
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                lineHeight = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = item.label,
                                    fontSize = 10.sp,
                                    lineHeight = 11.sp,
                                    fontWeight = if (selected || dragFocused) FontWeight.SemiBold else FontWeight.Medium,
                                    color = contentColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DualChatBubblesIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.095f

        // Primary front-left outlined speech bubble with bottom-left tail (matching Screenshot 2)
        val mainPath = Path().apply {
            moveTo(w * 0.14f, h * 0.44f)
            cubicTo(w * 0.14f, h * 0.20f, w * 0.30f, h * 0.12f, w * 0.48f, h * 0.12f)
            cubicTo(w * 0.66f, h * 0.12f, w * 0.78f, h * 0.24f, w * 0.78f, h * 0.43f)
            cubicTo(w * 0.78f, h * 0.62f, w * 0.64f, h * 0.74f, w * 0.44f, h * 0.74f)
            lineTo(w * 0.31f, h * 0.74f)
            lineTo(w * 0.16f, h * 0.84f)
            lineTo(w * 0.19f, h * 0.66f)
            cubicTo(w * 0.15f, h * 0.60f, w * 0.14f, h * 0.52f, w * 0.14f, h * 0.44f)
            close()
        }
        drawPath(
            path = mainPath,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Secondary overlapping bubble on bottom-right with small right tail
        val backPath = Path().apply {
            moveTo(w * 0.45f, h * 0.80f)
            cubicTo(w * 0.52f, h * 0.85f, w * 0.62f, h * 0.86f, w * 0.73f, h * 0.83f)
            lineTo(w * 0.86f, h * 0.89f)
            lineTo(w * 0.82f, h * 0.74f)
            cubicTo(w * 0.86f, h * 0.68f, w * 0.87f, h * 0.60f, w * 0.85f, h * 0.52f)
        }
        drawPath(
            path = backPath,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun UserAvatar(
    displayName: String,
    photoUrl: String?,
    isOnline: Boolean = false,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlassColors.current
    val initials = remember(displayName) {
        displayName.trim().split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifEmpty { "L" }
    }
    Box(modifier = modifier.size(size)) {
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "$displayName avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(1.5.dp, glass.glassHighlight, CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(glass.accent, glass.accentSecondary)
                        )
                    )
                    .border(1.5.dp, glass.glassHighlight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = if (size >= 64.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium
                )
            }
        }
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-1).dp, y = (-1).dp)
                    .clip(CircleShape)
                    .background(OnlineGreen)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
            )
        }
    }
}

@Composable
fun MessageStatusTicks(
    status: DeliveryStatus,
    tintOverride: Color? = null,
    modifier: Modifier = Modifier
) {
    val (icon, tint, desc) = when (status) {
        DeliveryStatus.SENT -> Triple(Icons.Default.Done, tintOverride ?: TickSentGrey, "Sent")
        DeliveryStatus.DELIVERED -> Triple(Icons.Default.DoneAll, tintOverride ?: TickDeliveredGrey, "Delivered")
        DeliveryStatus.READ -> Triple(Icons.Default.DoneAll, TickReadGreen, "Read")
    }
    Icon(
        imageVector = icon,
        contentDescription = desc,
        tint = tint,
        modifier = modifier.size(15.dp)
    )
}

@Composable
fun TypingDotsIndicator(modifier: Modifier = Modifier) {
    val glass = LocalLiquidGlassColors.current
    val transition = rememberInfiniteTransition(label = "typing_dots")
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 3) {
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 550, delayMillis = i * 160),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$i"
            )
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(glass.accent.copy(alpha = alpha))
            )
        }
    }
}

@Composable
fun VoiceWaveformView(
    amplitudes: List<Float>,
    progress: Float,
    barColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    onSeek: ((Float) -> Unit)? = null
) {
    val bars = remember(amplitudes) {
        if (amplitudes.isEmpty()) listOf(0.25f, 0.55f, 0.8f, 0.45f, 0.9f, 0.6f, 0.35f, 0.75f, 0.5f, 0.3f, 0.7f, 0.85f, 0.4f, 0.6f, 0.3f)
        else amplitudes
    }
    val seekMod = if (onSeek != null) {
        Modifier
            .pointerInput(onSeek) {
                detectTapGestures { offset ->
                    val frac = (offset.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                    onSeek(frac)
                }
            }
            .pointerInput(onSeek) {
                detectHorizontalDragGestures { change, _ ->
                    val frac = (change.position.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                    onSeek(frac)
                }
            }
    } else Modifier

    Canvas(
        modifier = modifier
            .height(28.dp)
            .then(seekMod)
    ) {
        val count = bars.size.coerceAtLeast(1)
        val gap = 3.dp.toPx()
        val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(2.dp.toPx())
        bars.forEachIndexed { idx, amp ->
            val ratio = idx.toFloat() / count.toFloat()
            val h = (size.height * amp.coerceIn(0.18f, 1.0f))
            val x = idx * (barWidth + gap)
            val top = (size.height - h) / 2f
            drawRoundRect(
                color = if (ratio <= progress) barColor else trackColor,
                topLeft = Offset(x, top),
                size = androidx.compose.ui.geometry.Size(barWidth, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun SkeletonChatRow(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.25f))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(13.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.25f))
                )
                Spacer(modifier = Modifier.height(7.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.18f))
                )
            }
        }
    }
}
