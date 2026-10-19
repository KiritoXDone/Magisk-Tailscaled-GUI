package io.github.tanakalun.tailcontrol.ui.screen.peer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.tanakalun.tailcontrol.R
import io.github.tanakalun.tailcontrol.ui.theme.BlurredBar
import io.github.tanakalun.tailcontrol.ui.theme.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter


fun String.toLocalTime(): String {
    return try {
        LocalDateTime.parse(trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            .atZone(ZoneId.of("UTC"))
            .withZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    } catch (e: Exception) {
        "$this (UTC)"
    }
}

@Composable
fun PeerDetailScreen(
    peerName: String,
    onBack: () -> Unit,
    viewModel: PeerDetailViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val layoutDirection = LocalLayoutDirection.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val barColor = if (backdrop != null) ComposeColor.Transparent else MiuixTheme.colorScheme.surface

    LaunchedEffect(peerName) {
        viewModel.load(peerName)
    }

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop) {
                TopAppBar(
                    title = peerName,
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
                    .padding(start = 16.dp, end = 16.dp)
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(rememberScrollState())
                    .overScrollVertical()
                    .scrollEndHaptic(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Spacer(Modifier.height(padding.calculateTopPadding() + 4.dp))

                val peer = ui.peer
                if (ui.notFound || peer == null) {
                    Text(stringResource(if (ui.notFound) R.string.peer_not_found else R.string.loading))
                    return@Column
                }

                PeerIdentitySummary(peer)
                PeerConnectionSection(peer)
                PeerNetworkSection(peer)
                PeerOwnershipSection(peer, ui.whois)
                viewModel.sshCommand?.let { PeerSshSection(command = it) }

                PeerPingSection(
                    pingActive = ui.pingActive,
                    rttSamples = ui.rttSamples,
                    pingLines = ui.pingLines,
                    avgRtt = viewModel.avgRtt,
                    p95Rtt = viewModel.p95Rtt,
                    onTogglePing = if (ui.pingActive) viewModel::stopPing else viewModel::startPing,
                )

                Spacer(Modifier.height(16.dp))
                Spacer(Modifier.height(padding.calculateBottomPadding()))
            }
        }
    }
}
