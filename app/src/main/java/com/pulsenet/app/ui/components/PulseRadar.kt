package com.pulsenet.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pulsenet.app.ui.theme.PulseBlue
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Animated concentric-ring radar showing nearby peers as dots. Each peer's
 * angle/distance is derived from a hash of its endpointId so a given peer's
 * dot stays put across recompositions instead of jumping around.
 */
@Composable
fun PulseRadar(peerIds: List<String>, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseFraction"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            val radius = min(size.width, size.height) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            for (i in 1..3) {
                drawCircle(
                    color = PulseBlue.copy(alpha = 0.25f),
                    radius = radius * i / 3f,
                    center = center,
                    style = Stroke(width = 2f)
                )
            }

            drawCircle(
                color = PulseBlue.copy(alpha = (1f - pulseFraction) * 0.6f),
                radius = radius * pulseFraction,
                center = center,
                style = Stroke(width = 3f)
            )

            peerIds.forEach { peerId ->
                val seed = Random(peerId.hashCode())
                val angle = seed.nextFloat() * 2f * Math.PI.toFloat()
                val distanceFraction = 0.35f + seed.nextFloat() * 0.55f
                val dotRadius = radius * distanceFraction
                val dotCenter = Offset(
                    x = center.x + dotRadius * cos(angle),
                    y = center.y + dotRadius * sin(angle)
                )
                drawCircle(color = PulseBlue, radius = 10f, center = dotCenter)
            }
        }
    }
}
