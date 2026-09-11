package com.pulsenet.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulsenet.app.domain.model.Message
import com.pulsenet.app.ui.theme.BorderSubtle
import com.pulsenet.app.ui.theme.SurfaceDark
import com.pulsenet.app.ui.theme.TextDim
import com.pulsenet.app.ui.theme.TextFaint
import com.pulsenet.app.ui.theme.TextPrimary
import com.pulsenet.app.ui.theme.toColor
import java.text.DateFormat
import java.util.Date

@Composable
fun MessageCard(message: Message, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 5.dp, horizontal = 12.dp)
    ) {
        Spacer(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(message.priority.toColor(), RoundedCornerShape(2.dp))
        )
        Card(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = message.senderAlias,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(message.createdAtEpochMs)),
                        color = TextFaint,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = message.content,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "Hop ${message.hopCount} of ${message.maxHops}",
                    color = TextDim,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
