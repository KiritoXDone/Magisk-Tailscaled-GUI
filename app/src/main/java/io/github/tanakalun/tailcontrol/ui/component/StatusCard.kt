package io.github.tanakalun.tailcontrol.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.tanakalun.tailcontrol.R
import io.github.tanakalun.tailcontrol.core.model.BackendState
import io.github.tanakalun.tailcontrol.ui.theme.isInDarkTheme
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
fun StatusCard(
    state: BackendState,
    online: Boolean,
    hostName: String,
    ip: String,
    modifier: Modifier = Modifier,
) {
    val connected = state == BackendState.Running && online
    val dark = isInDarkTheme()

    val cardColor = when {
        connected -> if (MiuixTheme.isDynamicColor) MiuixTheme.colorScheme.secondaryContainer
        else if (dark) Color(0xFF1A3825) else Color(0xFFDFFAE4)

        else -> if (MiuixTheme.isDynamicColor) MiuixTheme.colorScheme.errorContainer
        else if (dark) Color(0xFF3A2020) else Color(0xFFFDE8E8)
    }
    val statusTint = when {
        connected -> if (MiuixTheme.isDynamicColor) MiuixTheme.colorScheme.primary
        else if (dark) Color(0xFF81C784) else Color(0xFF4CAF50)

        else -> if (dark) Color(0xFFEF9A9A) else Color(0xFFE53935)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        colors = CardDefaults.defaultColors(color = cardColor),
        onClick = {},
        showIndication = true,
        pressFeedbackType = PressFeedbackType.Tilt,
    ) {
        Box(Modifier.fillMaxWidth().height(120.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(27.dp, 31.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Icon(
                    modifier = Modifier.size(110.dp),
                    imageVector = if (connected) Icons.Rounded.CheckCircleOutline else Icons.Rounded.RemoveCircleOutline,
                    tint = statusTint,
                    contentDescription = null,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp, 14.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                Column {
                    Text(
                        text = stringResource(state.labelRes),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = hostName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "IP: $ip",
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.width(4.dp))
                        val copyTarget = ip.takeIf { it.isNotBlank() && it != "—" }
                        CopyIpButton(ip = copyTarget)
                    }
                }
            }
        }
    }
}