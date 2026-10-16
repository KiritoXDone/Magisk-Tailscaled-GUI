package io.github.tanakalun.tailcontrol.ui.screen.log

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.tanakalun.tailcontrol.R
import io.github.tanakalun.tailcontrol.core.log.LogArchive
import io.github.tanakalun.tailcontrol.core.log.LogSource
import io.github.tanakalun.tailcontrol.ui.theme.BlurredBar
import io.github.tanakalun.tailcontrol.ui.theme.LocalEnableBlur
import io.github.tanakalun.tailcontrol.ui.theme.LocalStatusColors
import io.github.tanakalun.tailcontrol.ui.theme.rememberBlurBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

private val logSources: List<LogSource> = listOf(LogSource.App, LogSource.Tailscaled, LogSource.Runs)

/** 解析后的一条日志。 */
private data class LogEntry(
    val raw: String,
    val timestamp: String?,
    val level: String?,
    val message: String,
)

/** 将 app.log / tailscaled.log / runs.log 的原始行解析为结构化条目。 */
private fun parseLogLine(raw: String): LogEntry {
    val time = Regex("^\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{3})?")
        .find(raw)?.value?.replace('T', ' ')
    val rest = time?.let { raw.substring(it.length).trimStart(' ', '|', '-') } ?: raw
    val level = when {
        rest.contains("[Error]", ignoreCase = true) || rest.contains("e/", ignoreCase = true) ||
            rest.contains(" error", ignoreCase = true) || rest.contains("error:", ignoreCase = true) -> "error"
        rest.contains("[Warning]", ignoreCase = true) || rest.contains("w/", ignoreCase = true) ||
            rest.contains(" warn", ignoreCase = true) || rest.contains("warn:", ignoreCase = true) -> "warning"
        rest.contains("[Success]", ignoreCase = true) || rest.contains("[OK]", ignoreCase = true) ||
            rest.contains("netcheck: ok", ignoreCase = true) -> "success"
        else -> "info"
    }
    val message = rest.trim().trimStart('[', ']').trim()
    return LogEntry(raw = raw, timestamp = time, level = level, message = message.ifEmpty { raw })
}

@Composable
fun LogScreen(
    onBack: () -> Unit,
    viewModel: LogViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedEntry by remember { mutableStateOf<LogEntry?>(null) }
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(ui.lines.size, ui.paused, ui.isLive) {
        if (ui.isLive && !ui.paused && ui.lines.isNotEmpty()) {
            listState.animateScrollToItem(ui.lines.lastIndex.coerceAtLeast(0))
        }
    }

    val clearedOk = stringResource(R.string.log_clear_done)
    val noRoot = stringResource(R.string.log_clear_no_root)
    val clearFailed = stringResource(R.string.log_clear_failed)
    val tooLarge = stringResource(R.string.log_archive_too_large)
    val readFailed = stringResource(R.string.log_read_failed)
    LaunchedEffect(ui.transient) {
        val msg = when (ui.transient) {
            TransientKind.ClearedOk -> clearedOk
            TransientKind.NoRoot -> noRoot
            TransientKind.ClearFailed -> clearFailed
            TransientKind.ArchiveTooLarge -> tooLarge
            TransientKind.ReadFailed -> readFailed
            null -> null
        }
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissTransient()
        }
    }

    val entries = remember(ui.lines) { ui.lines.map(::parseLogLine) }
    val layoutDirection = LocalLayoutDirection.current
    val backdrop = rememberBlurBackdrop()
    val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface
    val scrollBehavior = MiuixScrollBehavior()

    val sourceLabels = listOf(
        stringResource(R.string.log_source_app),
        stringResource(R.string.log_source_tailscaled),
        stringResource(R.string.log_source_runs),
    )
    val archiveEntries = ui.archives.filterNot { it.isCurrent }
    val liveLabel = stringResource(R.string.log_archive_live)
    val archiveSelectorItems = listOf(liveLabel) + archiveEntries.map { it.label }
    val selectedArchiveIndex = archiveEntries
        .indexOfFirst { it.id == ui.selectedArchiveId }
        .let { if (it >= 0) it + 1 else 0 }

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop) {
                TopAppBar(
                    title = stringResource(R.string.nav_logs),
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                modifier = Modifier.graphicsLayer {
                                    if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                                },
                                imageVector = MiuixIcons.Back,
                                contentDescription = stringResource(R.string.back),
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    actions = {
                        if (ui.isLive) {
                            IconButton(onClick = viewModel::togglePause) {
                                Icon(
                                    if (ui.paused) MiuixIcons.Play else MiuixIcons.Pause,
                                    contentDescription = stringResource(
                                        if (ui.paused) R.string.log_resume else R.string.log_pause
                                    ),
                                    tint = MiuixTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        IconButton(onClick = viewModel::requestClear) {
                            Icon(
                                MiuixIcons.Delete,
                                contentDescription = stringResource(R.string.log_clear),
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(state = snackbarHostState) },
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        Box(
            Modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
        ) {
            PullToRefresh(
                isRefreshing = false,
                pullToRefreshState = pullToRefreshState,
                onRefresh = viewModel::loadEarlier,
                contentPadding = padding,
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .overScrollVertical()
                        .scrollEndHaptic(),
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding() + 6.dp,
                        start = padding.calculateStartPadding(layoutDirection),
                        end = padding.calculateEndPadding(layoutDirection),
                        bottom = padding.calculateBottomPadding() + 16.dp,
                    ),
                ) {
                    item {
                        Column(Modifier.padding(bottom = 8.dp)) {
                            Card(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                            ) {
                                OverlayDropdownPreference(
                                    title = stringResource(R.string.log_source),
                                    items = sourceLabels,
                                    selectedIndex = logSources.indexOf(ui.source).coerceAtLeast(0),
                                    onSelectedIndexChange = { index -> viewModel.switchSource(logSources[index]) },
                                    showValue = false,
                                )
                            }
                            if (ui.archives.size > 1) {
                                Spacer(Modifier.height(8.dp))
                                Card(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                ) {
                                    OverlayDropdownPreference(
                                        title = stringResource(R.string.log_archive_select),
                                        items = archiveSelectorItems,
                                        selectedIndex = selectedArchiveIndex,
                                        onSelectedIndexChange = { index ->
                                            val id = if (index == 0) LogArchive.LIVE_ID else archiveEntries[index - 1].id
                                            viewModel.selectArchive(id)
                                        },
                                        showValue = false,
                                    )
                                }
                            }
                        }
                    }
                    items(entries) { entry ->
                        LogEntryCard(
                            entry = entry,
                            onClick = { selectedEntry = entry },
                        )
                    }
                }
            }
        }
    }

    selectedEntry?.let { entry ->
        OverlayDialog(
            show = true,
            title = entry.timestamp ?: "",
            summary = entry.level,
            onDismissRequest = { selectedEntry = null },
            content = {
                Column {
                    SelectionContainer(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            text = entry.raw,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth()) {
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.done),
                            onClick = { selectedEntry = null },
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                        )
                    }
                }
            },
        )
    }

    if (ui.confirmClear) {
        val targetingArchive = !ui.isLive
        OverlayDialog(
            show = ui.confirmClear,
            title = stringResource(
                if (targetingArchive) R.string.log_archive_delete_confirm_title
                else R.string.log_clear_confirm_title
            ),
            summary = stringResource(
                if (targetingArchive) R.string.log_archive_delete_confirm_msg
                else R.string.log_clear_confirm_msg
            ),
            onDismissRequest = viewModel::dismissClear,
            content = {
                Row(Modifier.fillMaxWidth()) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.cancel),
                        onClick = viewModel::dismissClear,
                    )
                    TextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.confirm),
                        onClick = viewModel::confirmClear,
                    )
                }
            },
        )
    }
}

@Composable
private fun LogEntryCard(
    entry: LogEntry,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp),
        onClick = onClick,
        showIndication = true,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                entry.timestamp?.let {
                    Text(
                        it,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.weight(1f),
                    )
                }
                LevelTag(entry.level)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = entry.message,
                maxLines = 3,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = levelColor(entry.level),
            )
        }
    }
}

@Composable
private fun LevelTag(level: String?) {
    val (bg, fg) = when (level) {
        "error" -> MiuixTheme.colorScheme.errorContainer to MiuixTheme.colorScheme.onErrorContainer
        "warning" -> MiuixTheme.colorScheme.tertiaryContainer to MiuixTheme.colorScheme.onTertiaryContainer
        "success" -> LocalStatusColors.current.online to MiuixTheme.colorScheme.onPrimary
        else -> MiuixTheme.colorScheme.secondaryContainer to MiuixTheme.colorScheme.onSecondaryContainer
    }
    Text(
        text = level ?: "info",
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = fg,
        modifier = Modifier
            // .padding(start = 12.dp)
            .background(
                color = bg,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun levelColor(level: String?): Color = when (level) {
    "error" -> MiuixTheme.colorScheme.error
    "warning" -> LocalStatusColors.current.warning
    "success" -> LocalStatusColors.current.online
    else -> MiuixTheme.colorScheme.onSurface
}