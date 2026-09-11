package com.pulsenet.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PulseTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 18.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp)
)
