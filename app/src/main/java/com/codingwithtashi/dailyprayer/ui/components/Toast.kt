package com.codingwithtashi.dailyprayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Custom snackbar content: darkCard, r16, min height 50, bodyMedium text and a gold action. Tapping the
 * card, or flicking it left, right or down, dismisses it.
 */
@Composable
internal fun PrayerToast(data: SnackbarData, modifier: Modifier = Modifier) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    val shape = RoundedCornerShape(16.dp)
    val dismissLabel = stringResource(R.string.dismiss)
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    // Keyed on [data] so a dismissed toast's offset never leaks into the next one.
    val x = remember(data) { Animatable(0f) }
    val y = remember(data) { Animatable(0f) }
    var width by remember(data) { mutableIntStateOf(1) }
    val flingVelocity = with(density) { FLING_DP_PER_S.dp.toPx() }
    val downThreshold = with(density) { DOWN_DISMISS_DP.dp.toPx() }
    Row(
        modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp)
            .onSizeChanged { width = it.width.coerceAtLeast(1) }
            .offset { IntOffset(x.value.roundToInt(), y.value.roundToInt()) }
            .graphicsLayer { alpha = 1f - (max(abs(x.value) / width, y.value / (downThreshold * 4))).coerceIn(0f, 1f) * 0.8f }
            .pointerInput(data) {
                val velocity = VelocityTracker()
                // The card follows the finger, so change.position barely moves; track the summed drag instead.
                var travelled = Offset.Zero
                detectDragGestures(
                    onDragStart = { travelled = Offset.Zero },
                    onDrag = { change, drag ->
                        change.consume()
                        travelled += drag
                        velocity.addPosition(change.uptimeMillis, travelled)
                        scope.launch {
                            x.snapTo(x.value + drag.x)
                            y.snapTo((y.value + drag.y).coerceAtLeast(0f))
                        }
                    },
                    onDragEnd = {
                        val v = velocity.calculateVelocity()
                        velocity.resetTracking()
                        val goX = abs(x.value) > width * 0.25f || abs(v.x) > flingVelocity
                        val goDown = y.value > downThreshold || v.y > flingVelocity
                        scope.launch {
                            when {
                                goX -> x.animateTo(if (x.value + v.x * 0.1f >= 0f) width.toFloat() else -width.toFloat(), tween(150))
                                goDown -> y.animateTo(y.value + downThreshold * 4, tween(150))
                                else -> {
                                    launch { x.animateTo(0f) }
                                    y.animateTo(0f)
                                    return@launch
                                }
                            }
                            data.dismiss()
                        }
                    },
                    onDragCancel = { scope.launch { launch { x.animateTo(0f) }; y.animateTo(0f) } },
                )
            }
            .shadow(12.dp, shape, ambientColor = c.ink, spotColor = c.ink)
            .background(c.darkCard, shape)
            .border(1.dp, c.darkCardBorder, shape)
            .clip(shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClickLabel = dismissLabel) { data.dismiss() }
            .heightIn(min = 50.dp)
            .padding(horizontal = 16.dp)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                customActions = listOf(CustomAccessibilityAction(dismissLabel) { data.dismiss(); true })
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppText(data.visuals.message, style = t.bodyMedium, color = c.darkCardText, modifier = Modifier.weight(1f).padding(vertical = 12.dp))
        val action = data.visuals.actionLabel
        if (action != null) {
            Box(
                Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .clickable(role = Role.Button) { data.performAction() }
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                AppText(action, style = t.bodyStrong, color = c.gold)
            }
        }
    }
}

private const val FLING_DP_PER_S = 600
private const val DOWN_DISMISS_DP = 32

/**
 * Tracks the top edge (root coordinates) of bottom-pinned UI such as bars and action areas, so the
 * toast host can sit just above the highest one instead of covering it.
 */
@Stable
class ToastAnchors {
    private val tops = mutableStateMapOf<Any, Float>()

    /** Top edge of the highest registered anchor in px, or null when none is on screen. */
    val top: Float? get() = tops.values.minOrNull()

    internal fun update(key: Any, top: Float) {
        if (tops[key] != top) tops[key] = top
    }

    internal fun remove(key: Any) {
        tops.remove(key)
    }
}

val LocalToastAnchors = staticCompositionLocalOf { ToastAnchors() }

/** Marks a bottom-pinned element so toasts stack above it. */
fun Modifier.toastAnchor(): Modifier = this then ToastAnchorElement

private data object ToastAnchorElement : ModifierNodeElement<ToastAnchorNode>() {
    override fun create() = ToastAnchorNode()
    override fun update(node: ToastAnchorNode) = Unit
}

private class ToastAnchorNode : Modifier.Node(), CompositionLocalConsumerModifierNode, GlobalPositionAwareModifierNode {
    private var anchors: ToastAnchors? = null

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val target = anchors ?: currentValueOf(LocalToastAnchors).also { anchors = it }
        target.update(this, coordinates.boundsInRoot().top)
    }

    override fun onDetach() {
        anchors?.remove(this)
        anchors = null
    }
}

/**
 * Bottom-anchored toast host. Place it in a Box aligned BottomCenter that fills the root; it sits 8dp
 * above the highest [toastAnchor], or 16dp above the navigation bar / keyboard when none is registered,
 * with 16dp side margins.
 */
@Composable
fun AppToastHost(hostState: SnackbarHostState, rootHeightPx: Int, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val anchorTop = LocalToastAnchors.current.top
    val systemBottom = WindowInsets.navigationBars.union(WindowInsets.ime).asPaddingValues().calculateBottomPadding()
    val bottom = if (anchorTop != null) with(density) { (rootHeightPx - anchorTop).coerceAtLeast(0f).toDp() } + 8.dp else systemBottom + 16.dp
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, bottom = bottom),
    ) { data -> PrayerToast(data) }
}

/**
 * Shows toasts (with an optional Undo) on a [SnackbarHostState]. Timeouts are our own, shorter than
 * Material's, and stretched to the platform's recommended value when an accessibility service is on.
 */
class Toaster(private val host: SnackbarHostState, private val accessibility: AccessibilityManager? = null) {
    /**
     * Shows [message] for 2 s (3.5 s with an [undoLabel], 8 s when [long]); when [undoLabel] is set and
     * tapped, [onUndo] runs. Suspends until the toast closes by tap, swipe, action or timeout.
     */
    suspend fun show(
        message: String,
        undoLabel: String? = null,
        long: Boolean = false,
        onUndo: (suspend () -> Unit)? = null,
    ) {
        host.currentSnackbarData?.dismiss()
        val base = when {
            long -> LONG_MS
            undoLabel != null -> WITH_ACTION_MS
            else -> PLAIN_MS
        }
        val timeout = accessibility?.calculateRecommendedTimeoutMillis(
            base, containsIcons = false, containsText = true, containsControls = undoLabel != null,
        ) ?: base
        // Indefinite: the timeout below owns the lifetime, and cancelling showSnackbar clears the toast.
        val result = withTimeoutOrNull(timeout) {
            host.showSnackbar(message = message, actionLabel = undoLabel, withDismissAction = false, duration = SnackbarDuration.Indefinite)
        }
        if (result == SnackbarResult.ActionPerformed) onUndo?.invoke()
    }

    companion object {
        const val PLAIN_MS = 2_000L
        const val WITH_ACTION_MS = 3_500L
        const val LONG_MS = 8_000L
    }
}

/** App-level [Toaster]; defaults to one that shows nothing until the scaffold provides the real one. */
val LocalToaster = staticCompositionLocalOf { Toaster(SnackbarHostState()) }
