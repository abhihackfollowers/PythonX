package com.example.model

enum class LintSeverity {
    ERROR,
    WARNING,
    INFO
}

data class LintIssue(
    val line: Int,           // 1-indexed line number
    val column: Int = 1,     // 1-indexed column
    val message: String,
    val severity: LintSeverity = LintSeverity.ERROR,
    val quickFix: String? = null,
    val replacement: String? = null
)
