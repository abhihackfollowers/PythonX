package com.example.model

enum class ConsoleStreamType {
    STDOUT,
    STDERR,
    PROMPT,
    SYSTEM,
    INPUT_ECHO
}

data class ConsoleEvent(
    val type: ConsoleStreamType,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
