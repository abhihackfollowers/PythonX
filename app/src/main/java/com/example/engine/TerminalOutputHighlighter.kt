package com.example.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.example.model.ConsoleEvent
import com.example.model.ConsoleStreamType
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCoral
import com.example.ui.theme.CyberError
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberSecondary
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

data class DetectedTracebackInfo(
    val fileName: String,
    val lineNumber: Int,
    val functionName: String?,
    val errorType: String,
    val errorMessage: String,
    val culpritLine: String?
)

object TerminalOutputHighlighter {

    private val PYTHON_KEYWORDS = setOf(
        "def", "class", "return", "if", "else", "elif", "for", "while", "import",
        "from", "try", "except", "finally", "raise", "with", "as", "in", "and",
        "or", "not", "is", "lambda", "pass", "break", "continue", "yield", "global"
    )

    private val PYTHON_EXCEPTIONS = setOf(
        "ZeroDivisionError", "SyntaxError", "IndentationError", "NameError",
        "TypeError", "ValueError", "IndexError", "KeyError", "AttributeError",
        "ImportError", "RuntimeError", "KeyboardInterrupt", "FileNotFoundError",
        "ModuleNotFoundError", "StopIteration", "OverflowError", "RecursionError",
        "AssertionError", "NotImplementedError", "Exception"
    )

    // Regex matchers
    private val FILE_LINE_REGEX = Regex("""^\s*File\s+"([^"]+)",\s+line\s+(\d+)(?:,\s+in\s+(.+))?""")
    private val CARET_REGEX = Regex("""^\s*\^+\s*$""")
    private val EXCEPTION_REGEX = Regex("""^([a-zA-Z_][a-zA-Z0-9_]*(?:Error|Exception|Interrupt|Warning)):\s*(.*)""")
    private val WARNING_REGEX = Regex("""^(?:WARNING|Warning|[a-zA-Z0-9_]+Warning):\s*(.*)""")

    /**
     * Extracts structured traceback information to provide interactive Jump-to-Line and AI-Fix actions.
     */
    fun parseTracebackInfo(text: String): DetectedTracebackInfo? {
        val lines = text.split("\n")
        var lastFile: String? = null
        var lastLineNumber: Int? = null
        var lastFunction: String? = null
        var culpritCode: String? = null
        var errorType: String? = null
        var errorMessage: String? = null

        for (i in lines.indices) {
            val line = lines[i]
            val fileMatch = FILE_LINE_REGEX.find(line)
            if (fileMatch != null) {
                lastFile = fileMatch.groupValues[1]
                lastLineNumber = fileMatch.groupValues[2].toIntOrNull()
                lastFunction = fileMatch.groupValues.getOrNull(3)?.trim()
                val nextLine = lines.getOrNull(i + 1)?.trim()
                if (!nextLine.isNullOrBlank() && !nextLine.startsWith("File ") && !EXCEPTION_REGEX.matches(nextLine)) {
                    culpritCode = nextLine
                }
            }

            val exMatch = EXCEPTION_REGEX.find(line.trim())
            if (exMatch != null) {
                errorType = exMatch.groupValues[1]
                errorMessage = exMatch.groupValues[2]
            }
        }

        if (errorType != null) {
            return DetectedTracebackInfo(
                fileName = lastFile ?: "main.py",
                lineNumber = lastLineNumber ?: 1,
                functionName = lastFunction ?: "<module>",
                errorType = errorType,
                errorMessage = errorMessage ?: "",
                culpritLine = culpritCode
            )
        }
        return null
    }

    /**
     * Builds richly syntax-highlighted terminal text with color-coded stack trace frames,
     * highlight badges for files/line numbers, and distinct exception styling.
     */
    fun highlightTerminalOutput(event: ConsoleEvent): AnnotatedString {
        return when (event.type) {
            ConsoleStreamType.STDERR -> buildStderrTraceback(event.text)
            ConsoleStreamType.STDOUT -> buildStdoutText(event.text)
            ConsoleStreamType.SYSTEM -> buildSystemText(event.text)
            ConsoleStreamType.PROMPT -> buildPromptText(event.text)
            ConsoleStreamType.INPUT_ECHO -> buildInputEchoText(event.text)
        }
    }

    private fun buildStderrTraceback(rawText: String): AnnotatedString {
        return buildAnnotatedString {
            val lines = rawText.split("\n")
            for ((idx, line) in lines.withIndex()) {
                val trimmed = line.trim()

                when {
                    // 1. Traceback Header
                    trimmed.startsWith("Traceback (most recent call last):") -> {
                        append("⚠️ ")
                        pushStyle(
                            SpanStyle(
                                color = Color(0xFFF87171),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        append("Traceback (most recent call last):")
                        pop()
                    }

                    // 2. File and Line location frame
                    FILE_LINE_REGEX.find(line) != null -> {
                        val match = FILE_LINE_REGEX.find(line)!!
                        val fullMatch = match.value
                        val fileName = match.groupValues[1]
                        val lineNum = match.groupValues[2]
                        val funcName = match.groupValues.getOrNull(3)

                        val leadingSpaces = line.takeWhile { it == ' ' }
                        append(leadingSpaces)

                        // "File "
                        pushStyle(SpanStyle(color = Color(0xFF94A3B8)))
                        append("File ")
                        pop()

                        // "\"fileName\""
                        pushStyle(
                            SpanStyle(
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                        append("\"$fileName\"")
                        pop()

                        // ", line "
                        pushStyle(SpanStyle(color = Color(0xFF94A3B8)))
                        append(", line ")
                        pop()

                        // "lineNumber" (highlighted in prominent amber badge style)
                        pushStyle(
                            SpanStyle(
                                color = Color(0xFFFDE047),
                                fontWeight = FontWeight.ExtraBold,
                                background = Color(0x30FBBF24)
                            )
                        )
                        append(lineNum)
                        pop()

                        // ", in function"
                        if (!funcName.isNullOrBlank()) {
                            pushStyle(SpanStyle(color = Color(0xFF94A3B8)))
                            append(", in ")
                            pop()
                            pushStyle(
                                SpanStyle(
                                    color = Color(0xFFC084FC),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            append(funcName)
                            pop()
                        }
                    }

                    // 3. Caret Error Pointer (e.g. "   ^" or "   ^^^^^^^^")
                    CARET_REGEX.matches(line) -> {
                        pushStyle(
                            SpanStyle(
                                color = Color(0xFFFDE047),
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        append(line)
                        pop()
                    }

                    // 4. Exception Name and Message (e.g. "ZeroDivisionError: division by zero")
                    EXCEPTION_REGEX.find(trimmed) != null -> {
                        val match = EXCEPTION_REGEX.find(trimmed)!!
                        val exName = match.groupValues[1]
                        val exMsg = match.groupValues[2]

                        // Distinctive Exception Pill
                        pushStyle(
                            SpanStyle(
                                color = Color(0xFFFFFFFF),
                                background = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        append(" ✖ $exName ")
                        pop()

                        pushStyle(SpanStyle(color = Color(0xFFCBD5E1)))
                        append(": ")
                        pop()

                        // Error message in readable warm peach
                        pushStyle(
                            SpanStyle(
                                color = Color(0xFFFECACA),
                                fontWeight = FontWeight.Medium
                            )
                        )
                        append(exMsg)
                        pop()
                    }

                    // 5. Warning output (e.g. "UserWarning: ...")
                    WARNING_REGEX.find(trimmed) != null -> {
                        val match = WARNING_REGEX.find(trimmed)!!
                        pushStyle(
                            SpanStyle(
                                color = Color(0xFF000000),
                                background = Color(0xFFF59E0B),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        append(" ⚠ WARNING ")
                        pop()
                        pushStyle(SpanStyle(color = Color(0xFFFDE68A)))
                        append(" ${match.groupValues[1]}")
                        pop()
                    }

                    // 6. Indented code line inside traceback frame
                    line.startsWith("    ") || line.startsWith("  ") -> {
                        // Subtle dark container for code line
                        val leading = line.takeWhile { it == ' ' }
                        val code = line.trimStart()
                        append(leading)

                        pushStyle(SpanStyle(background = Color(0x221E293B)))
                        appendCodeWithSyntaxHighlight(code)
                        pop()
                    }

                    // Default fallback for other lines in stderr
                    else -> {
                        pushStyle(SpanStyle(color = CyberCoral))
                        append(line)
                        pop()
                    }
                }

                if (idx < lines.size - 1) {
                    append("\n")
                }
            }
        }
    }

    private fun AnnotatedString.Builder.appendCodeWithSyntaxHighlight(code: String) {
        val tokens = tokenizePythonLine(code)
        for (token in tokens) {
            when {
                PYTHON_KEYWORDS.contains(token) -> {
                    pushStyle(SpanStyle(color = Color(0xFFF43F5E), fontWeight = FontWeight.Bold))
                    append(token)
                    pop()
                }
                token.startsWith("\"") || token.startsWith("'") -> {
                    pushStyle(SpanStyle(color = Color(0xFF34D399)))
                    append(token)
                    pop()
                }
                token.all { it.isDigit() || it == '.' } && token.isNotEmpty() -> {
                    pushStyle(SpanStyle(color = Color(0xFFFBBF24)))
                    append(token)
                    pop()
                }
                token in listOf("(", ")", "[", "]", "{", "}", ",", ":", ".") -> {
                    pushStyle(SpanStyle(color = Color(0xFF94A3B8)))
                    append(token)
                    pop()
                }
                token in listOf("+", "-", "*", "/", "%", "**", "==", "!=", "<=", ">=", "<", ">", "=") -> {
                    pushStyle(SpanStyle(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold))
                    append(token)
                    pop()
                }
                else -> {
                    pushStyle(SpanStyle(color = Color(0xFFE2E8F0)))
                    append(token)
                    pop()
                }
            }
        }
    }

    private fun tokenizePythonLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c.isWhitespace()) {
                val start = i
                while (i < line.length && line[i].isWhitespace()) i++
                tokens.add(line.substring(start, i))
            } else if (c == '"' || c == '\'') {
                val start = i
                val quote = c
                i++
                while (i < line.length && line[i] != quote) {
                    if (line[i] == '\\' && i + 1 < line.length) i++
                    i++
                }
                if (i < line.length) i++
                tokens.add(line.substring(start, i))
            } else if (c.isLetter() || c == '_') {
                val start = i
                while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '_')) i++
                tokens.add(line.substring(start, i))
            } else if (c.isDigit()) {
                val start = i
                while (i < line.length && (line[i].isDigit() || line[i] == '.')) i++
                tokens.add(line.substring(start, i))
            } else if (i + 1 < line.length && line.substring(i, i + 2) in listOf("**", "==", "!=", "<=", ">=")) {
                tokens.add(line.substring(i, i + 2))
                i += 2
            } else {
                tokens.add(c.toString())
                i++
            }
        }
        return tokens
    }

    private fun buildStdoutText(text: String): AnnotatedString {
        return buildAnnotatedString {
            pushStyle(SpanStyle(color = CyberSecondary))
            append(text)
            pop()
        }
    }

    private fun buildSystemText(text: String): AnnotatedString {
        return buildAnnotatedString {
            pushStyle(SpanStyle(color = CyberPrimary, fontWeight = FontWeight.SemiBold))
            append(text)
            pop()
        }
    }

    private fun buildPromptText(text: String): AnnotatedString {
        return buildAnnotatedString {
            pushStyle(SpanStyle(color = CyberAmber, fontWeight = FontWeight.Bold))
            append(text)
            pop()
        }
    }

    private fun buildInputEchoText(text: String): AnnotatedString {
        return buildAnnotatedString {
            pushStyle(SpanStyle(color = CyberTextPrimary, fontWeight = FontWeight.Medium))
            append(text)
            pop()
        }
    }
}
