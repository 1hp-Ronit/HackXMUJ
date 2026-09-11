package com.pulsenet.app.ui.map

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.compose.ui.graphics.toArgb
import com.pulsenet.app.domain.model.Priority
import com.pulsenet.app.ui.theme.toColor

/** SOS pins are drawn larger than the rest so they stand out at a glance. */
fun priorityMarkerIcon(priority: Priority): Drawable {
    val sizePx = if (priority == Priority.SOS) 56 else 36
    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(priority.toColor().toArgb())
        setSize(sizePx, sizePx)
    }
}
