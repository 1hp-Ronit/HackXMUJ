package com.pulsenet.app.data.remote

/** Keyword-based triage — deliberately simple, no ML needed for a hackathon-scale demo. */
object SeverityClassifier {

    private val CRITICAL_KEYWORDS = listOf(
        "trapped", "bleeding", "drowning", "fire", "dying",
        "crushed", "buried", "unconscious", "heart attack"
    )
    private val HIGH_KEYWORDS = listOf(
        "injured", "broken", "medical", "pain", "help",
        "hurt", "wound", "fracture", "ambulance"
    )
    private val MEDIUM_KEYWORDS = listOf(
        "food", "water", "shelter", "medicine",
        "blanket", "clothes", "baby", "child", "elderly"
    )

    fun classify(text: String): String {
        val lower = text.lowercase()
        return when {
            CRITICAL_KEYWORDS.any { lower.contains(it) } -> "CRITICAL"
            HIGH_KEYWORDS.any { lower.contains(it) } -> "HIGH"
            MEDIUM_KEYWORDS.any { lower.contains(it) } -> "MEDIUM"
            else -> "LOW"
        }
    }
}
