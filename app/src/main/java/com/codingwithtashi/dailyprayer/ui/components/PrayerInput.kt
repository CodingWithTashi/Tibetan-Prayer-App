package com.codingwithtashi.dailyprayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.codingwithtashi.dailyprayer.core.text.hasTibetan
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme

/**
 * Text field with a 1.5dp border (borderStrong, accent when focused), surface background and
 * accent cursor. [height] is the minimum height. [value] is truncated to [maxLength].
 */
@Composable
fun PrayerInput(
    value: String,
    onValueChange: (String) -> Unit,
    height: Dp,
    radius: Dp,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    maxLength: Int = Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    autofocus: Boolean = false,
    enabled: Boolean = true,
) {
    val c = PrayerTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val requester = remember { FocusRequester() }
    if (autofocus) LaunchedEffect(Unit) { runCatching { requester.requestFocus() } }
    val shape = RoundedCornerShape(radius)
    val fieldStyle = textStyle.forContent(if (value.isEmpty()) placeholder else value)
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(if (it.length > maxLength) it.take(maxLength) else it) },
        modifier = modifier.fillMaxWidth().focusRequester(requester),
        enabled = enabled,
        textStyle = fieldStyle.copy(color = c.ink),
        cursorBrush = SolidColor(c.accent),
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interaction,
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = height)
                    .background(c.surface, shape)
                    .border(1.5.dp, if (focused) c.accent else c.borderStrong, shape)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    AppText(placeholder, style = fieldStyle, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                inner()
            }
        },
    )
}
