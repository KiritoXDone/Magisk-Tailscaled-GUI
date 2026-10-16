package io.github.tanakalun.tailcontrol.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.tanakalun.tailcontrol.R
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 未授予 root 权限时的提示，使用 miuix WindowDialog（无需 Scaffold 祖先）。
 * 提供「重试」与「退出」两个按钮，同行排布。
 */
@Composable
fun RootDeniedDialog(
    onRetry: () -> Unit,
    onExit: () -> Unit,
) {
    WindowDialog(
        show = true,
        title = stringResource(R.string.get_root_failed_title),
        summary = stringResource(R.string.get_root_failed_msg),
        onDismissRequest = null,
        content = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.root_retry),
                    onClick = onRetry,
                )
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.exit),
                    onClick = onExit,
                )
            }
        },
    )
}