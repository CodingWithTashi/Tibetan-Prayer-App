package com.codingwithtashi.dailyprayer.ui.library

import androidx.compose.ui.platform.testTag
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.core.text.hasTibetan
import com.codingwithtashi.dailyprayer.data.model.PrayerCategory
import com.codingwithtashi.dailyprayer.ui.components.AppText
import com.codingwithtashi.dailyprayer.ui.components.forContent
import com.codingwithtashi.dailyprayer.ui.components.CategoryChip
import com.codingwithtashi.dailyprayer.ui.components.IconActionButton
import com.codingwithtashi.dailyprayer.ui.components.LibraryRow
import com.codingwithtashi.dailyprayer.ui.components.ScreenTitle
import com.codingwithtashi.dailyprayer.ui.components.contentWidth
import com.codingwithtashi.dailyprayer.ui.theme.CategoryKey
import com.codingwithtashi.dailyprayer.ui.theme.PrayerDimens
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme
import com.codingwithtashi.dailyprayer.ui.theme.category

@Composable
fun LibraryTab(
    listState: LazyListState,
    onOpenReader: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LibraryContent(state, listState, { viewModel.onEvent(LibraryEvent.SetQuery(it)) }, { viewModel.onEvent(LibraryEvent.SetCategory(it)) }, onOpenReader, modifier)
}

@Composable
internal fun LibraryContent(
    state: LibraryUiState,
    listState: LazyListState,
    onQueryChange: (String) -> Unit,
    onCategory: (PrayerCategory?) -> Unit,
    onOpenReader: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = PrayerTheme.colors
    val focus = LocalFocusManager.current
    Box(modifier.fillMaxSize().background(c.background), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.contentWidth().statusBarsPadding().imePadding(),
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ScreenTitle(stringResource(R.string.tab_library))
                    SearchField(state.query, onQueryChange, focus)
                }
            }
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).selectableGroup(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(state.categories, key = { it?.key ?: "all" }) { cat ->
                        CategoryChip(
                            label = stringResource(cat.labelRes()),
                            selected = state.category == cat,
                            onClick = { onCategory(cat) },
                            dot = cat?.let { c.category(CategoryKey.valueOf(it.name)).dot },
                        )
                    }
                }
            }
            if (state.items.isEmpty()) {
                item {
                    AppText(
                        stringResource(R.string.no_results),
                        style = PrayerTheme.type.body,
                        color = c.textMuted,
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            } else {
                items(state.items, key = { it.id }) { p ->
                    LibraryRow(
                        syllable = p.syllable,
                        category = p.category,
                        tib = p.title,
                        en = p.titleEn,
                        hasAudio = p.hasAudio,
                        onClick = { onOpenReader(p.id) },
                        modifier = Modifier.padding(horizontal = 20.dp),
                        showDivider = p.id != state.items.last().id,
                    )
                }
            }
        }
    }
}

@StringRes
private fun PrayerCategory?.labelRes(): Int = when (this) {
    null -> R.string.all
    PrayerCategory.DAILY -> R.string.cat_daily
    PrayerCategory.SUTRA -> R.string.cat_sutra
    PrayerCategory.LIFE -> R.string.cat_life
    PrayerCategory.PRAISE -> R.string.cat_praise
    PrayerCategory.ASPIRE -> R.string.cat_aspire
    PrayerCategory.OTHER -> R.string.cat_other
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit, focus: FocusManager) {
    val c = PrayerTheme.colors
    val shape = RoundedCornerShape(26.dp)
    val hint = stringResource(R.string.search_hint)
    val fieldStyle = PrayerTheme.type.input.forContent(if (query.isEmpty()) hint else query)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = PrayerDimens.searchFieldMin)
            .background(c.surface, shape)
            .border(1.dp, c.border, shape)
            .padding(start = 16.dp, end = if (query.isNotEmpty()) 0.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(painterResource(R.drawable.ic_search), null, Modifier.size(18.dp), tint = c.textMuted)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                AppText(hint, style = fieldStyle, color = c.textSubtle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BasicTextField(
                value = query,
                onValueChange = onChange,
                modifier = Modifier.fillMaxWidth().testTag("library_search"),
                textStyle = fieldStyle.copy(color = c.ink),
                cursorBrush = SolidColor(c.accent),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            )
        }
        if (query.isNotEmpty()) {
            IconActionButton(R.drawable.ic_close, stringResource(R.string.cd_clear_search), { onChange("") }, tint = c.textMuted, iconSize = 16.dp)
        }
    }
}
