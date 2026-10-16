package io.github.tanakalun.tailcontrol.ui.screen.drop

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.tanakalun.tailcontrol.R
import io.github.tanakalun.tailcontrol.core.model.BackendState
import io.github.tanakalun.tailcontrol.core.model.ConflictBehavior
import io.github.tanakalun.tailcontrol.ui.component.MiuixTextInput
import io.github.tanakalun.tailcontrol.ui.component.SectionHeader
import io.github.tanakalun.tailcontrol.ui.theme.BlurredBar
import io.github.tanakalun.tailcontrol.ui.theme.LocalEnableBlur
import io.github.tanakalun.tailcontrol.ui.theme.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun DropScreen(
    viewModel: DropViewModel = hiltViewModel(),
    bottomInset: Dp = 0.dp,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val daemonOutput by viewModel.daemonOutput.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(ui.errorToast) {
        ui.errorToast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeError()
        }
    }

    val pickFolder = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            val path = uri.path?.replace("/tree/primary:", "/sdcard/") ?: return@rememberLauncherForActivityResult
            viewModel.setPath(path)
            Toast.makeText(context, "${context.getString(R.string.path_selected)}: $path", Toast.LENGTH_SHORT).show()
        }
    }

    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val barColor = if (backdrop != null) ComposeColor.Transparent else MiuixTheme.colorScheme.surface

    val conflictLabels = mapOf(
        ConflictBehavior.Rename to stringResource(R.string.rename),
        ConflictBehavior.Skip to stringResource(R.string.skip),
        ConflictBehavior.Overwrite to stringResource(R.string.overwrite),
    )
    val conflicts = ConflictBehavior.entries

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop) {
                TopAppBar(
                    title = "Tailscale Drop",
                    color = barColor,
                    scrollBehavior = scrollBehavior,
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
            Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .overScrollVertical()
                .scrollEndHaptic(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(padding.calculateTopPadding() + 4.dp))

            if (!ui.fileCommandSupported) {
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.errorContainer),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.drop_unavailable_title),
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.drop_unavailable_msg),
                            color = MiuixTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            SectionHeader(stringResource(R.string.drop_group_receive))
            // 1. Receive daemon + 路径选择
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                Column {
                    ArrowPreference(
                        title = stringResource(R.string.select_path),
                        summary = ui.config.path,
                        onClick = { pickFolder.launch(null) },
                    )
                    SwitchPreference(
                        title = stringResource(R.string.file_receiver_daemon),
                        checked = ui.config.enabled,
                        onCheckedChange = viewModel::setEnabled,
                    )
                }
            }

            // 2. Conflict behavior
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                OverlayDropdownPreference(
                    title = stringResource(R.string.conflict_behavior),
                    items = conflicts.map { conflictLabels.getValue(it) },
                    selectedIndex = conflicts.indexOf(ui.config.conflict).coerceAtLeast(0),
                    onSelectedIndexChange = { index -> viewModel.setConflict(conflicts[index]) },
                    showValue = false,
                )
            }

            SectionHeader(stringResource(R.string.drop_group_diagnostics))
            // 3. Ping：输入框与按钮各占一行
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    MiuixTextInput(
                        value = ui.pingAddress,
                        onValueChange = viewModel::setPingAddress,
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(R.string.ping_placeholder),
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = viewModel::startPing,
                        enabled = ui.pingAddress.isNotBlank() && ui.tailscaleState is BackendState.Running,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColorsPrimary(),
                    ) {
                        Text(stringResource(R.string.test))
                    }
                }
            }

            // 4. Fix duplicate processes：直接 primary 按钮，不封装卡片
            Button(
                onClick = viewModel::openKillDialog,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(stringResource(R.string.fix_duplicate_file))
            }

            // 5. Daemon output
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                Column(Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("${stringResource(R.string.daemon_output)}:", fontSize = 14.sp)
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .border(1.dp, MiuixTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(daemonOutput.ifBlank { "—" }, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(padding.calculateBottomPadding() + bottomInset))
        }
        }
    }

    if (ui.pingDialogOpen) {
        OverlayDialog(
            show = ui.pingDialogOpen,
            title = "Ping ${stringResource(R.string.test)}",
            onDismissRequest = viewModel::closePingDialog,
            content = {
                Column {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(ui.pingOutput)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth()) {
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(if (ui.isPinging) R.string.cancel else R.string.done),
                            onClick = if (ui.isPinging) viewModel::stopPing else viewModel::closePingDialog,
                        )
                    }
                }
            },
        )
    }

    if (ui.killDialogOpen) {
        val (titleRes, textRes) = when {
            ui.processCount == 0 -> R.string.oops to R.string.no_tail_process
            ui.processCount in 1..2 -> R.string.kill_process to R.string.only_one_process
            else -> R.string.kill_process to R.string.multi_process
        }
        OverlayDialog(
            show = ui.killDialogOpen,
            title = stringResource(titleRes),
            summary = stringResource(textRes),
            onDismissRequest = viewModel::dismissKillDialog,
            content = {
                Row(Modifier.fillMaxWidth()) {
                    if (ui.processCount > 0) {
                        TextButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.cancel),
                            onClick = viewModel::dismissKillDialog,
                        )
                    }
                    TextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(if (ui.processCount == 0) R.string.done else R.string.confirm),
                        onClick = if (ui.processCount == 0) viewModel::dismissKillDialog else viewModel::confirmKillProcesses,
                    )
                }
            },
        )
    }
}