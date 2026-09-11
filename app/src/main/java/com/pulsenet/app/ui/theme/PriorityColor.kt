package com.pulsenet.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.pulsenet.app.domain.model.Priority

fun Priority.toColor(): Color = when (this) {
    Priority.SOS -> SOSRed
    Priority.MEDICAL -> MedicalOrange
    Priority.RESOURCE -> ResourceYellow
    Priority.GENERAL -> GeneralGray
}
