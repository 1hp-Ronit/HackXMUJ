package com.pulsenet.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulsenet.app.domain.model.Message
import com.pulsenet.app.ui.theme.SurfaceDark
import com.pulsenet.app.ui.theme.TextPrimary
import com.pulsenet.app.ui.theme.toColor
import java.text.DateFormat
import java.util.Date

@Composable
fun MessageCard(message: Message, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 12.dp)) {
        Spacer(
            modifier = Modifier
                .width(6.dp)
                .height(84.dp)
                .background(message.priority.toColor(), RoundedCornerShape(3.dp))
        )
        Card(
            modifier = Modifier.fillMaxWidth().padding(start = 10.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = message.senderAlias,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(message.createdAtEpochMs)),
                        color = TextPrimary.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = message.content,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "Hop ${message.hopCount} of ${message.maxHops}",
                    color = TextPrimary.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}
