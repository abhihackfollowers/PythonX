package com.example.engine

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.example.model.LintIssue
import com.example.ui.theme.CyberCoral
import com.example.ui.theme.CyberError
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.SyntaxBuiltin
import com.example.ui.theme.SyntaxComment
import com.example.ui.theme.SyntaxFunction
import com.example.ui.theme.SyntaxKeyword
import com.example.ui.theme.SyntaxNumber
import com.example.ui.theme.SyntaxOperator
import com.example.ui.theme.SyntaxString

object PythonHighlighter {

    private val KEYWORDS = setOf(
        "def", "class", "import", "from", "as", "return", "yield",
        "if", "elif", "else", "for", "while", "in", "not", "is", "and", "or",
        "try", "except", "finally", "raise", "with", "lambda", "pass", "break",
        "continue", "global", "nonlocal", "assert", "async", "await",
        "True", "False", "None"
    )

    private val BUILTINS = setOf(
        "print", "input", "len", "range", "enumerate", "zip", "map", "filter",
        "sum", "min", "max", "abs", "round", "sorted", "reversed", "open",
        "type", "isinstance", "int", "float", "str", "bool", "list", "dict",
        "set", "tuple", "dir", "help", "any", "all", "id", "hash"
    )

    fun highlight(code: String, issues: List<LintIssue> = emptyList()): AnnotatedString {
        if (code.isEmpty()) return AnnotatedString("")

        val errorLines = issues.map { it.line }.toSet()

        return buildAnnotatedString {
            append(code)

            // Default text color
            addStyle(
                SpanStyle(color = CyberTextPrimary),
                0,
                code.length
            )

            // Tokenizer loop
            var i = 0
            val n = code.length

            while (i < n) {
                val c = code[i]

                // Comments: # until end of line
                if (c == '#') {
                    val start = i
                    while (i < n && code[i] != '\n') {
                        i++
                    }
                    addStyle(
                        SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic),
                        start,
                        i
                    )
                    continue
                }

                // Multi-line or single strings
                if (c == '\'' || c == '"') {
                    val quoteChar = c
                    val isTriple = (i + 2 < n && code[i + 1] == quoteChar && code[i + 2] == quoteChar)
                    val start = i

                    if (isTriple) {
                        i += 3
                        while (i + 2 < n) {
                            if (code[i] == quoteChar && code[i + 1] == quoteChar && code[i + 2] == quoteChar) {
                                i += 3
                                break
                            }
                            if (code[i] == '\\') i++ // Skip escaped
                            i++
                        }
                    } else {
                        i++
                        while (i < n && code[i] != '\n') {
                            if (code[i] == quoteChar) {
                                i++
                                break
                            }
                            if (code[i] == '\\' && i + 1 < n) {
                                i += 2
                                continue
                            }
                            i++
                        }
                    }

                    addStyle(
                        SpanStyle(color = SyntaxString),
                        start,
                        i.coerceAtMost(n)
                    )
                    continue
                }

                // Numbers: integers, floats, hex
                if (c.isDigit()) {
                    val start = i
                    while (i < n && (code[i].isLetterOrDigit() || code[i] == '.' || code[i] == '_')) {
                        i++
                    }
                    addStyle(
                        SpanStyle(color = SyntaxNumber),
                        start,
                        i
                    )
                    continue
                }

                // Identifiers & Keywords
                if (c.isLetter() || c == '_') {
                    val start = i
                    while (i < n && (code[i].isLetterOrDigit() || code[i] == '_')) {
                        i++
                    }
                    val word = code.substring(start, i)

                    // Check preceding word for def/class to highlight function/class name
                    val prevText = code.substring(0, start).trimEnd()
                    val isDefTarget = prevText.endsWith("def")
                    val isClassTarget = prevText.endsWith("class")

                    if (word in KEYWORDS) {
                        addStyle(
                            SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.SemiBold),
                            start,
                            i
                        )
                    } else if (isDefTarget || isClassTarget) {
                        addStyle(
                            SpanStyle(color = SyntaxFunction, fontWeight = FontWeight.Bold),
                            start,
                            i
                        )
                    } else if (word in BUILTINS) {
                        addStyle(
                            SpanStyle(color = SyntaxBuiltin),
                            start,
                            i
                        )
                    }
                    continue
                }

                // Operators & punctuation
                if (c in "+-*/%=<>!&|^~@:;,.") {
                    addStyle(
                        SpanStyle(color = SyntaxOperator),
                        i,
                        i + 1
                    )
                }

                i++
            }

            // Highlight error lines with red underline
            if (errorLines.isNotEmpty()) {
                val lines = code.split("\n")
                var charOffset = 0
                for ((lineIdx, lineText) in lines.withIndex()) {
                    val lineNum = lineIdx + 1
                    val lineLength = lineText.length
                    if (lineNum in errorLines && lineLength > 0) {
                        addStyle(
                            SpanStyle(
                                textDecoration = TextDecoration.Underline,
                                color = CyberCoral
                            ),
                            charOffset,
                            charOffset + lineLength
                        )
                    }
                    charOffset += lineLength + 1 // +1 for \n
                }
            }
        }
    }
}
