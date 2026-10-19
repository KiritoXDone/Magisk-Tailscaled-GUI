package io.github.tanakalun.tailcontrol.ui.screen.peer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.tanakalun.tailcontrol.R
import io.github.tanakalun.tailcontrol.ui.component.CopyTextButton
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun PeerDetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        SmallTitle(
            text = title,
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        )
        Card(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
internal fun PeerDetailValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    SelectionContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Text(text = value, style = MiuixTheme.textStyles.main)
        }
    }
}

@Composable
internal fun PeerDetailValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PeerDetailValue(label = label, value = value, modifier = Modifier.weight(1f))
        CopyTextButton(text = value, label = label, modifier = Modifier.size(48.dp))
    }
}

@Composable
internal fun PeerDetailDisclosure(
    title: String,
    stateKey: String,
    summary: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable(stateKey) { mutableStateOf(false) }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val expandedDescription = stringResource(
        if (expanded) R.string.peer_expanded else R.string.peer_collapsed,
    )
    BasicComponent(
        title = title,
        summary = summary,
        modifier = Modifier.semantics { stateDescription = expandedDescription },
        role = Role.Button,
        onClickLabel = stringResource(if (expanded) R.string.peer_collapse else R.string.peer_expand),
        onClick = { expanded = !expanded },
        endActions = {
            Icon(
                imageVector = MiuixIcons.ChevronForward,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                modifier = Modifier.size(20.dp).graphicsLayer {
                    rotationZ = if (expanded) 90f else if (isRtl) 180f else 0f
                },
            )
        },
    )
    AnimatedVisibility(visible = expanded) {
        Column(modifier = Modifier.padding(bottom = 8.dp), content = content)
    }
}
