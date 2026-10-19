package io.github.tanakalun.tailcontrol.ui.screen.peer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.tanakalun.tailcontrol.R
import io.github.tanakalun.tailcontrol.ui.component.SpeedChart
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun PeerPingSection(
    pingActive: Boolean,
    rttSamples: List<Double>,
    pingLines: List<String>,
    avgRtt: Double,
    p95Rtt: Double,
    onTogglePing: () -> Unit,
) {
    Button(
        onClick = onTogglePing,
        modifier = Modifier.fillMaxWidth(),
        minHeight = 50.dp,
        colors = if (pingActive) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColorsPrimary(),
    ) {
        Text(
            text = stringResource(if (pingActive) R.string.peer_stop_ping else R.string.peer_start_ping),
            style = MiuixTheme.textStyles.button,
        )
    }

    if (pingActive || rttSamples.isNotEmpty() || pingLines.isNotEmpty()) {
        PeerDetailSection(title = stringResource(R.string.peer_ping_results)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (rttSamples.isNotEmpty()) {
                    SpeedChart(
                        rxValues = rttSamples,
                        txValues = emptyList(),
                        rxLabel = "RTT (ms)",
                        txLabel = "",
                        height = 160.dp,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PeerDetailValue(
                            label = stringResource(R.string.peer_ping_avg),
                            value = "${"%.1f".format(avgRtt)} ms",
                        )
                        PeerDetailValue(
                            label = stringResource(R.string.peer_ping_p95),
                            value = "${"%.1f".format(p95Rtt)} ms",
                        )
                        PeerDetailValue(
                            label = stringResource(R.string.peer_ping_count),
                            value = rttSamples.size.toString(),
                        )
                    }
                }
                if (pingLines.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp).verticalScroll(rememberScrollState())) {
                        SelectionContainer {
                            Text(
                                text = pingLines.joinToString("\n"),
                                fontFamily = FontFamily.Monospace,
                                style = MiuixTheme.textStyles.body2,
                            )
                        }
                    }
                } else if (pingActive) {
                    Text(
                        text = stringResource(R.string.peer_ping_waiting),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
    }
}
