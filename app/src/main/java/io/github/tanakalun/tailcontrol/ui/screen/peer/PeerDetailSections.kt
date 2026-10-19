package io.github.tanakalun.tailcontrol.ui.screen.peer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.tanakalun.tailcontrol.R
import io.github.tanakalun.tailcontrol.core.model.TailscaleDevice
import io.github.tanakalun.tailcontrol.core.model.WhoisInfo
import io.github.tanakalun.tailcontrol.ui.component.CopyTextButton
import io.github.tanakalun.tailcontrol.ui.theme.LocalStatusColors
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun PeerIdentitySummary(peer: TailscaleDevice) {
    val statusColors = LocalStatusColors.current
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = peer.os,
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier.size(8.dp).background(
                    color = if (peer.online) statusColors.online else statusColors.offline,
                    shape = CircleShape,
                ),
            )
            Text(
                text = stringResource(if (peer.online) R.string.status_online else R.string.status_offline),
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

@Composable
internal fun PeerConnectionSection(peer: TailscaleDevice) {
    PeerDetailSection(title = stringResource(R.string.peer_connection)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            peer.relay?.takeIf { it.isNotBlank() }?.let { relay ->
                PeerDetailValue(
                    label = stringResource(R.string.peer_relay),
                    value = relay,
                    modifier = Modifier.weight(1f),
                )
            }
            PeerDetailValue(
                label = stringResource(R.string.peer_activity),
                value = stringResource(if (peer.active) R.string.peer_active else R.string.peer_inactive),
                modifier = Modifier.weight(1f),
            )
        }
        if (!peer.online && !peer.lastSeen.isNullOrBlank()) {
            PeerDetailValue(
                label = stringResource(R.string.last_seen),
                value = peer.lastSeen.toLocalTime(),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }
        if (peer.exitNodeOption || peer.isExitNode) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (peer.exitNodeOption) Text(
                    text = stringResource(R.string.peer_exit_node_option),
                    style = MiuixTheme.textStyles.body2,
                )
                if (peer.isExitNode) Text(
                    text = stringResource(R.string.peer_is_exit_node),
                    style = MiuixTheme.textStyles.body2,
                )
            }
        }
    }
}

@Composable
internal fun PeerNetworkSection(peer: TailscaleDevice) {
    PeerDetailSection(title = stringResource(R.string.peer_addresses)) {
        if (peer.ips.isEmpty()) {
            Text(
                text = stringResource(R.string.unknown),
                modifier = Modifier.padding(16.dp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        peer.ips.forEach { ip ->
            PeerDetailValueRow(label = if (ip.contains(':')) "IPv6" else "IPv4", value = ip)
        }
        if (peer.allowedIps.isNotEmpty()) {
            PeerDetailDisclosure(
                title = stringResource(R.string.peer_allowed_ips),
                stateKey = peer.name,
                summary = pluralStringResource(R.plurals.peer_item_count, peer.allowedIps.size, peer.allowedIps.size),
            ) {
                peer.allowedIps.forEach { ip ->
                    PeerDetailValueRow(label = if (ip.contains(':')) "IPv6" else "IPv4", value = ip)
                }
            }
        }
        if (peer.primaryRoutes.isNotEmpty()) {
            PeerDetailValue(
                label = stringResource(R.string.peer_routes),
                value = peer.primaryRoutes.joinToString("\n"),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
internal fun PeerOwnershipSection(peer: TailscaleDevice, whois: WhoisInfo?) {
    val userName = whois?.userName?.takeIf { it.isNotBlank() }
    val machineName = whois?.machineName?.takeIf { it.isNotBlank() }
        ?: peer.dnsName.trimEnd('.').takeIf { it.isNotBlank() }
    val machineId = whois?.machineId?.takeIf { it.isNotBlank() }
    if (userName == null && machineName == null && machineId == null) return

    PeerDetailSection(title = stringResource(R.string.peer_device_info)) {
        userName?.let {
            PeerDetailValue(
                label = stringResource(R.string.peer_user),
                value = it,
                modifier = Modifier.padding(16.dp),
            )
        }
        machineName?.let {
            PeerDetailValueRow(label = stringResource(R.string.peer_machine), value = it)
        }
        machineId?.let {
            PeerDetailDisclosure(
                title = stringResource(R.string.peer_device_id),
                stateKey = it,
            ) {
                PeerDetailValueRow(label = stringResource(R.string.peer_device_id), value = it)
            }
        }
    }
}

@Composable
internal fun PeerSshSection(command: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.peer_ssh_command),
                    style = MiuixTheme.textStyles.headline1,
                    modifier = Modifier.weight(1f),
                )
                CopyTextButton(text = command, modifier = Modifier.size(48.dp))
            }
            SelectionContainer {
                Text(
                    text = command,
                    fontFamily = FontFamily.Monospace,
                    style = MiuixTheme.textStyles.body2,
                )
            }
            Text(
                text = stringResource(R.string.peer_ssh_hint),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}
