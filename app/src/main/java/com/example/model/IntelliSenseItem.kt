package com.example.model

enum class IntelliSenseType {
    KEYWORD,
    BUILTIN,
    FUNCTION,
    VARIABLE,
    CLASS,
    SNIPPET,
    MODULE
}

data class IntelliSenseItem(
    val label: String,
    val insertText: String = label,
    val type: IntelliSenseType = IntelliSenseType.KEYWORD,
    val detail: String = "",
    val docstring: String = "",
    val cursorOffset: Int = 0 // Offset from end of insertText to position cursor
)
