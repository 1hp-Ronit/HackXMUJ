package com.pulsenet.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulsenet.app.ui.theme.SOSRed

/** Giant panic button: single tap for confirm-then-send flows, long press for immediate SOS. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SOSButton(
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    label: String = "SOS"
) {
    Box(
        modifier = modifier
            .size(size)
            .background(SOSRed, CircleShape)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
                onLongClick = onLongPress
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 48.sp)
    }
}
