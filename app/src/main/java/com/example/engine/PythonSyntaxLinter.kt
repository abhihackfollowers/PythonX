package com.example.engine

import com.example.model.LintIssue
import com.example.model.LintSeverity
import java.util.Stack

object PythonSyntaxLinter {

    private val BLOCK_KEYWORDS = listOf(
        "def", "class", "if", "elif", "else", "for", "while",
        "try", "except", "finally", "with", "async def", "async with", "async for"
    )

    fun lint(code: String): List<LintIssue> {
        val issues = mutableListOf<LintIssue>()
        if (code.isBlank()) return issues

        val lines = code.split("\n")
        var inTripleSingleQuote = false
        var inTripleDoubleQuote = false
        var insideFunction = false
        var currentFunctionIndent = 0

        // Bracket stack tracking: stores Pair(char, lineIndex)
        val bracketStack = Stack<Pair<Char, Int>>()

        var previousLineExpectedIndent = false
        var expectedIndentLevel = 0

        for ((index, rawLine) in lines.withIndex()) {
            val lineNumber = index + 1
            val trimmedLine = rawLine.trim()

            // 1. Triple quote block comment handling
            if (!inTripleSingleQuote && !inTripleDoubleQuote) {
                if (trimmedLine.startsWith("\"\"\"") && !trimmedLine.endsWith("\"\"\"") || trimmedLine.count { it == '"' } % 6 == 3) {
                    inTripleDoubleQuote = true
                } else if (trimmedLine.startsWith("'''") && !trimmedLine.endsWith("'''") || trimmedLine.count { it == '\'' } % 6 == 3) {
                    inTripleSingleQuote = true
                }
            } else {
                if (inTripleDoubleQuote && trimmedLine.contains("\"\"\"")) {
                    inTripleDoubleQuote = false
                    continue
                } else if (inTripleSingleQuote && trimmedLine.contains("'''")) {
                    inTripleSingleQuote = false
                    continue
                }
                // While inside multi-line docstring, skip single line checks
                continue
            }

            // Skip empty lines and full-line comments
            if (trimmedLine.isEmpty()) continue
            if (trimmedLine.startsWith("#")) continue

            // Remove trailing comment for syntax parsing
            val codePart = if (trimmedLine.contains("#")) {
                trimmedLine.substringBefore("#").trim()
            } else {
                trimmedLine
            }

            val currentIndent = rawLine.takeWhile { it == ' ' }.length

            // 2. Indentation check
            if (previousLineExpectedIndent) {
                if (currentIndent <= expectedIndentLevel) {
                    issues.add(
                        LintIssue(
                            line = lineNumber,
                            column = currentIndent + 1,
                            message = "IndentationError: Expected an indented block after block statement.",
                            severity = LintSeverity.ERROR,
                            quickFix = "Indent line by 4 spaces",
                            replacement = "    $rawLine"
                        )
                    )
                }
                previousLineExpectedIndent = false
            }

            // Function tracking for return statement check
            if (codePart.startsWith("def ")) {
                insideFunction = true
                currentFunctionIndent = currentIndent
            } else if (insideFunction && currentIndent <= currentFunctionIndent && !codePart.startsWith("def ") && !codePart.startsWith("class ")) {
                insideFunction = false
            }

            // 3. Return outside function check
            if (codePart.startsWith("return ") || codePart == "return") {
                if (!insideFunction) {
                    issues.add(
                        LintIssue(
                            line = lineNumber,
                            column = rawLine.indexOf("return") + 1,
                            message = "SyntaxError: 'return' outside function.",
                            severity = LintSeverity.ERROR
                        )
                    )
                }
            }

            // 4. JS/C++ operator syntax warnings
            if (codePart.contains("++") || codePart.contains("--")) {
                issues.add(
                    LintIssue(
                        line = lineNumber,
                        column = 1,
                        message = "SyntaxError: '++' and '--' are not supported in Python. Use '+=' or '-='.",
                        severity = LintSeverity.ERROR
                    )
                )
            }
            if (codePart.contains("===")) {
                issues.add(
                    LintIssue(
                        line = lineNumber,
                        column = rawLine.indexOf("===") + 1,
                        message = "SyntaxError: '===' is invalid. In Python, use '==' for value equality.",
                        severity = LintSeverity.ERROR,
                        quickFix = "Replace with '=='"
                    )
                )
            }
            if (codePart.contains("&&") || codePart.contains("||")) {
                issues.add(
                    LintIssue(
                        line = lineNumber,
                        column = 1,
                        message = "SyntaxError: In Python, use 'and' and 'or' instead of '&&' or '||'.",
                        severity = LintSeverity.ERROR,
                        quickFix = "Use 'and' / 'or'"
                    )
                )
            }
            if (codePart.startsWith("function ")) {
                issues.add(
                    LintIssue(
                        line = lineNumber,
                        column = 1,
                        message = "SyntaxError: In Python, functions are defined with 'def', not 'function'.",
                        severity = LintSeverity.ERROR,
                        quickFix = "Replace 'function' with 'def'"
                    )
                )
            }
            if (codePart.startsWith("var ") || codePart.startsWith("let ") || codePart.startsWith("const ")) {
                issues.add(
                    LintIssue(
                        line = lineNumber,
                        column = 1,
                        message = "SyntaxError: Python is dynamically typed; do not use 'var/let/const'.",
                        severity = LintSeverity.ERROR
                    )
                )
            }
            if (codePart.contains(" null") || codePart.endsWith("=null") || codePart.endsWith("= null")) {
                issues.add(
                    LintIssue(
                        line = lineNumber,
                        column = 1,
                        message = "NameError: 'null' is not defined. Did you mean 'None'?",
                        severity = LintSeverity.WARNING,
                        quickFix = "Replace with 'None'"
                    )
                )
            }
            if (codePart.contains(" true") || codePart.contains(" false")) {
                issues.add(
                    LintIssue(
                        line = lineNumber,
                        column = 1,
                        message = "NameError: In Python, booleans are capitalized: 'True' and 'False'.",
                        severity = LintSeverity.WARNING,
                        quickFix = "Capitalize boolean"
                    )
                )
            }

            // 5. Block colon check
            val isBlockStatement = BLOCK_KEYWORDS.any { kw ->
                codePart.startsWith("$kw ") || codePart == kw || (kw == "else" && codePart.startsWith("else")) || (kw == "try" && codePart.startsWith("try"))
            }

            if (isBlockStatement) {
                if (!codePart.endsWith(":")) {
                    issues.add(
                        LintIssue(
                            line = lineNumber,
                            column = rawLine.length + 1,
                            message = "SyntaxError: Expected ':' at the end of '${codePart.split(" ").first()}' statement.",
                            severity = LintSeverity.ERROR,
                            quickFix = "Add ':'",
                            replacement = "$rawLine:"
                        )
                    )
                } else {
                    previousLineExpectedIndent = true
                    expectedIndentLevel = currentIndent
                }
            }

            // 6. Bracket pairing check on this line
            var inStrSingle = false
            var inStrDouble = false
            var escaped = false

            for (ch in codePart) {
                if (escaped) {
                    escaped = false
                    continue
                }
                if (ch == '\\') {
                    escaped = true
                    continue
                }

                if (ch == '\'' && !inStrDouble) {
                    inStrSingle = !inStrSingle
                    continue
                }
                if (ch == '"' && !inStrSingle) {
                    inStrDouble = !inStrDouble
                    continue
                }

                if (!inStrSingle && !inStrDouble) {
                    when (ch) {
                        '(', '[', '{' -> bracketStack.push(Pair(ch, lineNumber))
                        ')' -> {
                            if (bracketStack.isEmpty() || bracketStack.peek().first != '(') {
                                issues.add(
                                    LintIssue(
                                        line = lineNumber,
                                        message = "SyntaxError: Unmatched closing parenthesis ')'.",
                                        severity = LintSeverity.ERROR
                                    )
                                )
                            } else {
                                bracketStack.pop()
                            }
                        }
                        ']' -> {
                            if (bracketStack.isEmpty() || bracketStack.peek().first != '[') {
                                issues.add(
                                    LintIssue(
                                        line = lineNumber,
                                        message = "SyntaxError: Unmatched closing bracket ']'.",
                                        severity = LintSeverity.ERROR
                                    )
                                )
                            } else {
                                bracketStack.pop()
                            }
                        }
                        '}' -> {
                            if (bracketStack.isEmpty() || bracketStack.peek().first != '{') {
                                issues.add(
                                    LintIssue(
                                        line = lineNumber,
                                        message = "SyntaxError: Unmatched closing brace '}'.",
                                        severity = LintSeverity.ERROR
                                    )
                                )
                            } else {
                                bracketStack.pop()
                            }
                        }
                    }
                }
            }

            if (inStrSingle || inStrDouble) {
                issues.add(
                    LintIssue(
                        line = lineNumber,
                        message = "SyntaxError: Unterminated string literal.",
                        severity = LintSeverity.ERROR,
                        quickFix = "Close quote"
                    )
                )
            }
        }

        // Remaining unclosed brackets
        while (!bracketStack.isEmpty()) {
            val (openChar, line) = bracketStack.pop()
            val closeChar = when (openChar) {
                '(' -> ')'
                '[' -> ']'
                '{' -> '}'
                else -> '?'
            }
            issues.add(
                LintIssue(
                    line = line,
                    message = "SyntaxError: Closing parenthesis '$closeChar' does not match or was never closed.",
                    severity = LintSeverity.ERROR,
                    quickFix = "Add '$closeChar'"
                )
            )
        }

        return issues
    }

    /**
     * Real-time 1-Click Auto-Fix Engine
     * Automatically fixes missing colons, invalid operators, indentation, and bracket mismatches.
     */
    fun autoFixAll(code: String): Pair<String, Int> {
        if (code.isBlank()) return Pair(code, 0)
        var fixesCount = 0
        val lines = code.split("\n").toMutableList()

        var previousLineExpectedIndent = false
        var expectedIndent = 0

        for (i in lines.indices) {
            var line = lines[i]
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            val currentIndent = line.takeWhile { it == ' ' }.length

            // 1. Indentation auto-fix
            if (previousLineExpectedIndent) {
                if (currentIndent <= expectedIndent) {
                    val fixedIndent = " ".repeat(expectedIndent + 4)
                    line = fixedIndent + trimmed
                    lines[i] = line
                    fixesCount++
                }
                previousLineExpectedIndent = false
            }

            // 2. Missing colon auto-fix
            val isBlock = BLOCK_KEYWORDS.any { kw ->
                trimmed.startsWith("$kw ") || trimmed == kw || (kw == "else" && trimmed.startsWith("else")) || (kw == "try" && trimmed.startsWith("try"))
            }
            if (isBlock && !trimmed.endsWith(":")) {
                line = "$line:"
                lines[i] = line
                fixesCount++
                previousLineExpectedIndent = true
                expectedIndent = currentIndent
            } else if (trimmed.endsWith(":")) {
                previousLineExpectedIndent = true
                expectedIndent = currentIndent
            }

            // 3. Operators auto-fix
            if (line.contains("===")) {
                line = line.replace("===", "==")
                fixesCount++
            }
            if (line.contains("!==")) {
                line = line.replace("!==", "!=")
                fixesCount++
            }
            if (line.contains(" && ")) {
                line = line.replace(" && ", " and ")
                fixesCount++
            }
            if (line.contains(" || ")) {
                line = line.replace(" || ", " or ")
                fixesCount++
            }
            if (line.trimStart().startsWith("function ")) {
                val indentStr = line.takeWhile { it == ' ' }
                line = indentStr + "def " + line.trimStart().removePrefix("function ")
                fixesCount++
            }
            if (line.trimStart().startsWith("var ") || line.trimStart().startsWith("let ") || line.trimStart().startsWith("const ")) {
                val indentStr = line.takeWhile { it == ' ' }
                line = indentStr + line.trimStart().removePrefix("var ").removePrefix("let ").removePrefix("const ")
                fixesCount++
            }
            if (line.contains("++")) {
                line = Regex("""([a-zA-Z_][a-zA-Z0-9_]*)\+\+""").replace(line) { m -> "${m.groupValues[1]} += 1" }
                fixesCount++
            }
            if (line.contains("--")) {
                line = Regex("""([a-zA-Z_][a-zA-Z0-9_]*)--""").replace(line) { m -> "${m.groupValues[1]} -= 1" }
                fixesCount++
            }
            if (line.contains(" null")) {
                line = line.replace(" null", " None")
                fixesCount++
            }
            if (line.contains("=null") || line.contains("= null")) {
                line = line.replace("=null", "= None").replace("= null", "= None")
                fixesCount++
            }
            if (line.contains(" true") || line.contains("(true") || line.contains("[true")) {
                line = line.replace(" true", " True").replace("(true", "(True").replace("[true", "[True")
                fixesCount++
            }
            if (line.contains(" false") || line.contains("(false") || line.contains("[false")) {
                line = line.replace(" false", " False").replace("(false", "(False").replace("[false", "[False")
                fixesCount++
            }

            // 4. Bracket balancing for single line
            val openParen = line.count { it == '(' }
            val closeParen = line.count { it == ')' }
            if (openParen > closeParen) {
                line = line + ")".repeat(openParen - closeParen)
                fixesCount++
            }

            val openBracket = line.count { it == '[' }
            val closeBracket = line.count { it == ']' }
            if (openBracket > closeBracket) {
                line = line + "]".repeat(openBracket - closeBracket)
                fixesCount++
            }

            val openBrace = line.count { it == '{' }
            val closeBrace = line.count { it == '}' }
            if (openBrace > closeBrace) {
                line = line + "}".repeat(openBrace - closeBrace)
                fixesCount++
            }

            lines[i] = line
        }

        return Pair(lines.joinToString("\n"), fixesCount)
    }
}
