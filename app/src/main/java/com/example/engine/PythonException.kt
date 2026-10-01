package com.example.engine

data class StackFrame(
    val fileName: String,
    val lineNumber: Int,
    val functionName: String,
    val codeLine: String?
)

open class PythonException(
    val typeName: String,
    override val message: String,
    val line: Int = 1,
    val fileName: String = "main.py"
) : RuntimeException(message) {

    val frames = mutableListOf<StackFrame>()

    fun addFrame(fileName: String, lineNumber: Int, functionName: String, codeLine: String?) {
        frames.add(StackFrame(fileName, lineNumber, functionName, codeLine))
    }

    fun formatTraceback(scriptLines: List<String>): String {
        val sb = StringBuilder()
        if (typeName == "SyntaxError" || typeName == "IndentationError") {
            sb.append("  File \"$fileName\", line $line\n")
            val rawLine = scriptLines.getOrNull(line - 1)?.trimEnd() ?: ""
            sb.append("    $rawLine\n")
            val indent = " ".repeat(4 + (rawLine.takeWhile { it == ' ' }.length).coerceAtLeast(0))
            sb.append("$indent^\n")
            sb.append("$typeName: $message\n")
            return sb.toString()
        }

        sb.append("Traceback (most recent call last):\n")
        if (frames.isNotEmpty()) {
            for (frame in frames) {
                sb.append("  File \"${frame.fileName}\", line ${frame.lineNumber}, in ${frame.functionName}\n")
                val lineContent = frame.codeLine ?: scriptLines.getOrNull(frame.lineNumber - 1)?.trim()
                if (!lineContent.isNullOrBlank()) {
                    sb.append("    $lineContent\n")
                }
            }
        } else {
            sb.append("  File \"$fileName\", line $line, in <module>\n")
            val lineContent = scriptLines.getOrNull(line - 1)?.trim()
            if (!lineContent.isNullOrBlank()) {
                sb.append("    $lineContent\n")
            }
        }
        sb.append("$typeName: $message\n")
        return sb.toString()
    }
}

class PythonSyntaxError(msg: String, line: Int = 1, fileName: String = "main.py") :
    PythonException("SyntaxError", msg, line, fileName)

class PythonIndentationError(msg: String, line: Int = 1, fileName: String = "main.py") :
    PythonException("IndentationError", msg, line, fileName)

class PythonNameError(name: String, line: Int = 1, fileName: String = "main.py") :
    PythonException("NameError", "name '$name' is not defined", line, fileName)

class PythonTypeError(msg: String, line: Int = 1, fileName: String = "main.py") :
    PythonException("TypeError", msg, line, fileName)

class PythonValueError(msg: String, line: Int = 1, fileName: String = "main.py") :
    PythonException("ValueError", msg, line, fileName)

class PythonZeroDivisionError(line: Int = 1, fileName: String = "main.py") :
    PythonException("ZeroDivisionError", "division by zero", line, fileName)

class PythonIndexError(msg: String = "list index out of range", line: Int = 1, fileName: String = "main.py") :
    PythonException("IndexError", msg, line, fileName)

class PythonKeyError(key: String, line: Int = 1, fileName: String = "main.py") :
    PythonException("KeyError", "'$key'", line, fileName)

class PythonAttributeError(objName: String, attrName: String, line: Int = 1, fileName: String = "main.py") :
    PythonException("AttributeError", "'$objName' object has no attribute '$attrName'", line, fileName)

class PythonImportError(moduleName: String, line: Int = 1, fileName: String = "main.py") :
    PythonException("ImportError", "No module named '$moduleName'", line, fileName)
