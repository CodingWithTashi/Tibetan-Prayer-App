package com.codingwithtashi.dailyprayer.ui.notifications

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.ui.components.EmptyState
import com.codingwithtashi.dailyprayer.ui.components.LocalToaster
import com.codingwithtashi.dailyprayer.ui.components.TopBar
import com.codingwithtashi.dailyprayer.ui.components.contentWidth
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme
import kotlinx.coroutines.launch

@Composable
fun NotificationsScreen(onBack: () -> Unit, viewModel: NotificationsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toaster = LocalToaster.current
    val message = stringResource(R.string.notification_deleted)
    val undo = stringResource(R.string.undo)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is NotificationsEvent.Deleted -> launch {
                    toaster.show(message, undo) { viewModel.undoDelete(event.notification) }
                }
            }
        }
    }
    val context = LocalContext.current
    NotificationsContent(state, onBack, viewModel::delete) { openLink(context, it) }
}

private fun openLink(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // No browser installed: nothing to open.
    }
}

@Composable
internal fun NotificationsContent(
    state: NotificationsUiState,
    onBack: () -> Unit,
    onDelete: (NotificationItem) -> Unit,
    onOpenLink: (String) -> Unit,
) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    Column(Modifier.fillMaxSize().background(c.background)) {
        TopBar(onBack = onBack, center = { Text(stringResource(R.string.notifications), style = t.body15Semi, color = c.ink) })
        val items = state.items
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.TopCenter) {
            when {
                items == null -> Unit
                items.isEmpty() -> EmptyState(R.drawable.ic_bell, stringResource(R.string.no_notifications), Modifier.contentWidth())
                else -> LazyColumn(
                    modifier = Modifier.contentWidth(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                ) {
                    items(items, key = { it.id }) { item ->
                        NotificationRow(item, showDivider = item.id != items.last().id, onDelete = { onDelete(item) }, onOpenLink = onOpenLink)
                    }
                    item { Box(Modifier.navigationBarsPadding()) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun NotificationRow(item: NotificationItem, showDivider: Boolean, onDelete: () -> Unit, onOpenLink: (String) -> Unit) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    val currentDelete by rememberUpdatedState(onDelete)
    val dismiss = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.5f },
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                currentDelete()
                true
            } else false
        },
    )
    val deleteLabel = stringResource(R.string.cd_delete)
    SwipeToDismissBox(
        state = dismiss,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(Modifier.fillMaxSize().background(c.danger).padding(end = 20.dp), contentAlignment = Alignment.CenterEnd) {
                Icon(painterResource(R.drawable.ic_trash), null, Modifier.size(22.dp), tint = c.onAccentSolid)
            }
        },
    ) {
        val n = item.notification
        Column(
            Modifier
                .fillMaxWidth()
                .background(c.background)
                .drawBehind {
                    if (showDivider) {
                        val h = 1.dp.toPx()
                        drawRect(c.divider, Offset(0f, size.height - h), Size(size.width, h))
                    }
                }
                .combinedClickable(onClick = {}, onLongClick = onDelete, onLongClickLabel = deleteLabel)
                .semantics { customActions = listOf(CustomAccessibilityAction(deleteLabel) { onDelete(); true }) }
                .heightIn(min = 72.dp)
                .padding(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Text(n.title, style = t.body15Semi, color = c.ink, modifier = Modifier.weight(1f))
                Text(item.timeLabel, style = t.caption12, color = c.textMuted, maxLines = 1)
            }
            Text(n.content, style = t.body14, color = c.textMuted, overflow = TextOverflow.Ellipsis)
            if (!n.link.isNullOrBlank()) {
                Box(
                    Modifier
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Button) { onOpenLink(n.link) }
                        .padding(end = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(stringResource(R.string.more), style = t.body13Semi, color = c.accent)
                }
            }
        }
    }
}
