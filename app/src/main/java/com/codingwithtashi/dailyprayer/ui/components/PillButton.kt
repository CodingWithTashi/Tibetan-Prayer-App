package com.codingwithtashi.dailyprayer.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme

/** Visual variant of a [PillButton]. */
sealed interface PillVariant {
    /** accentSolid fill, onAccentSolid text. */
    data object Filled : PillVariant

    /** surfaceWarm fill, accent text. */
    data object Tonal : PillVariant

    /** Transparent with a border: 1dp `border` colour, or accent when [width] > 1dp. */
    data class Outlined(val width: Dp = 1.dp) : PillVariant

    /** Transparent, accent text, no border. */
    data object Text : PillVariant
}

/**
 * Pill-shaped button. Text style follows [height] unless [textStyle] is given; colour overrides
 * replace the variant defaults. Touch target is at least 48dp. The visible pill fills the width
 * the caller imposes (`Modifier.fillMaxWidth()` / `weight`); otherwise it wraps its content.
 * [interactive] = false renders a plain, non-clickable pill at full opacity.
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PillVariant = PillVariant.Filled,
    height: Dp = 48.dp,
    horizontalPadding: Dp = 20.dp,
    radius: Dp = height / 2,
    @DrawableRes leadingIcon: Int? = null,
    leadingIconSize: Dp = 16.dp,
    enabled: Boolean = true,
    interactive: Boolean = true,
    textStyle: TextStyle? = null,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
) {
    val c = PrayerTheme.colors
    val type = PrayerTheme.type
    val style = textStyle ?: when {
        height >= 54.dp -> type.body16Semi
        height >= 48.dp -> type.body15Semi
        height >= 44.dp -> type.body14Semi
        height >= 40.dp -> type.body14Semi
        else -> type.body13Semi
    }
    val defaultContainer = when (variant) {
        PillVariant.Filled -> c.accentSolid
        PillVariant.Tonal -> c.surfaceWarm
        else -> Color.Transparent
    }
    val defaultContent = when (variant) {
        PillVariant.Filled -> c.onAccentSolid
        else -> c.accent
    }
    val bg = if (containerColor != Color.Unspecified) containerColor else defaultContainer
    val fg = if (contentColor != Color.Unspecified) contentColor else defaultContent
    val shape = RoundedCornerShape(radius)
    val borderMod = if (variant is PillVariant.Outlined) {
        Modifier.border(variant.width, if (variant.width > 1.dp) c.accent else c.border, shape)
    } else Modifier
    Box(
        if (interactive) modifier.minimumInteractiveComponentSize() else modifier,
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true,
    ) {
        Row(
            Modifier
                .height(height)
                .alpha(if (enabled || !interactive) 1f else 0.5f)
                .clip(shape)
                .background(bg)
                .then(borderMod)
                .then(
                    if (interactive) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = fg),
                            enabled = enabled,
                            role = Role.Button,
                            onClick = onClick,
                        )
                    } else Modifier,
                )
                .padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (leadingIcon != null) {
                Icon(painterResource(leadingIcon), contentDescription = null, tint = fg, modifier = Modifier.size(leadingIconSize))
                Box(Modifier.size(8.dp))
            }
            Text(text, style = style, color = fg, maxLines = 1)
        }
    }
}
