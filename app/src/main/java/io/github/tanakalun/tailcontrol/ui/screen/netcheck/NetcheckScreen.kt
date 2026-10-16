package io.github.tanakalun.tailcontrol.ui.screen.netcheck

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.tanakalun.tailcontrol.R
import io.github.tanakalun.tailcontrol.ui.component.SectionHeader
import io.github.tanakalun.tailcontrol.ui.theme.BlurredBar
import io.github.tanakalun.tailcontrol.ui.theme.LocalEnableBlur
import io.github.tanakalun.tailcontrol.ui.theme.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NetcheckScreen(
    viewModel: NetcheckViewModel = hiltViewModel(),
    bottomInset: Dp = 0.dp,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val expanded = remember { mutableStateMapOf<Long, Boolean>() }
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val barColor = if (backdrop != null) ComposeColor.Transparent else MiuixTheme.colorScheme.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop) {
                TopAppBar(
                    title = stringResource(R.string.netcheck_report_title),
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                    actions = {
                        if (history.isNotEmpty()) {
                            IconButton(onClick = viewModel::clearHistory) {
                                Icon(
                                    MiuixIcons.Delete,
                                    contentDescription = stringResource(R.string.netcheck_clear_history),
                                )
                            }
                        }
                    },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            Spacer(Modifier.height(padding.calculateTopPadding() + 8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                Button(
                    onClick = viewModel::run,
                    enabled = !ui.running,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Icon(MiuixIcons.Play, contentDescription = null)
                    Spacer(Modifier.padding(end = 8.dp))
                    Text(stringResource(if (ui.running) R.string.loading else R.string.netcheck_run))
                }
            }

            Spacer(Modifier.height(12.dp))

            when {
                // 当前结果占满剩余空间，内部滚动
                ui.current.isNotBlank() -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(
                                start = 12.dp,
                                end = 12.dp,
                                bottom = padding.calculateBottomPadding() + bottomInset + 12.dp,
                            ),
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp),
                        ) {
                            SelectionContainer {
                                Text(ui.current, fontSize = 13.sp)
                            }
                        }
                    }
                }
                // 没当前结果时，历史列表占满剩余空间
                history.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .overScrollVertical()
                            .scrollEndHaptic(),
                        contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 16.dp + bottomInset),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            SectionHeader(stringResource(R.string.netcheck_history))
                        }
                        items(history, key = { it.timestampMillis }) { report ->
                            val isOpen = expanded[report.timestampMillis] == true
                            val time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                .format(Date(report.timestampMillis))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .animateContentSize(),
                                onClick = { expanded[report.timestampMillis] = !isOpen },
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(time, fontWeight = FontWeight.SemiBold)
                                    if (isOpen) {
                                        Spacer(Modifier.height(6.dp))
                                        SelectionContainer {
                                            Text(report.raw, fontSize = 13.sp)
                                        }
                                    } else {
                                        Text(
                                            report.raw.lineSequence().firstOrNull().orEmpty(),
                                            fontSize = 13.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.netcheck_history_empty))
                    }
                }
            }
        }
        }
    }
}