package com.garbage.management.domain.model

/**
 * Domain entity representing an assistive AI analysis of garbage media.
 * Contains waste category classification and accumulation severity estimation.
 *
 * NOTE: The AI analysis is an assistive prediction. Official municipal actions
 * require officer verification.
 */
data class AiAnalysis(
    val classification: AiClassification? = null,
    val severity: AiSeverity? = null,
    val modelStatus: String = "DEVELOPMENT_BASELINE",
    val analyzedAt: Long? = null
)

data class AiClassification(
    val category: String,
    val confidence: Double? = null
)

data class AiSeverity(
    val level: String,
    val confidence: Double? = null
)
