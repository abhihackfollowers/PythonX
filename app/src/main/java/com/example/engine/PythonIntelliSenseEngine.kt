package com.example.engine

import com.example.model.IntelliSenseItem
import com.example.model.IntelliSenseType

object PythonIntelliSenseEngine {

    private val KEYWORDS = listOf(
        IntelliSenseItem("def", "def name():\n    ", IntelliSenseType.KEYWORD, "Define function", "Defines a new user function with arguments and code block."),
        IntelliSenseItem("class", "class Name:\n    def __init__(self):\n        ", IntelliSenseType.KEYWORD, "Define class", "Defines a new class with constructor."),
        IntelliSenseItem("import", "import ", IntelliSenseType.KEYWORD, "Import module", "Imports an external package or module."),
        IntelliSenseItem("from", "from  import ", IntelliSenseType.KEYWORD, "From import", "Imports specific objects or functions from a module."),
        IntelliSenseItem("return", "return ", IntelliSenseType.KEYWORD, "Return statement", "Exits a function and optionally passes back an expression."),
        IntelliSenseItem("yield", "yield ", IntelliSenseType.KEYWORD, "Yield generator", "Yields a value from a generator function."),
        IntelliSenseItem("if", "if :", IntelliSenseType.KEYWORD, "Conditional if", "Branch execution based on truth value of an expression."),
        IntelliSenseItem("elif", "elif :", IntelliSenseType.KEYWORD, "Else-if branch", "Alternative conditional branch."),
        IntelliSenseItem("else", "else:\n    ", IntelliSenseType.KEYWORD, "Else branch", "Fallback branch if no conditions were met."),
        IntelliSenseItem("for", "for item in :\n    ", IntelliSenseType.KEYWORD, "For-in loop", "Iterates over items of any sequence."),
        IntelliSenseItem("while", "while :", IntelliSenseType.KEYWORD, "While loop", "Repeats a block as long as expression is true."),
        IntelliSenseItem("try", "try:\n    \nexcept Exception as e:\n    print(e)", IntelliSenseType.KEYWORD, "Try-except block", "Handles runtime exceptions safely."),
        IntelliSenseItem("except", "except Exception as e:\n    ", IntelliSenseType.KEYWORD, "Catch exception", "Catches matching exceptions."),
        IntelliSenseItem("finally", "finally:\n    ", IntelliSenseType.KEYWORD, "Finally block", "Always executes before leaving try block."),
        IntelliSenseItem("with", "with open() as f:\n    ", IntelliSenseType.KEYWORD, "Context manager", "Guarantees resource release with context manager."),
        IntelliSenseItem("lambda", "lambda x: ", IntelliSenseType.KEYWORD, "Anonymous function", "Creates an anonymous inline lambda function."),
        IntelliSenseItem("async", "async def ", IntelliSenseType.KEYWORD, "Async coroutine", "Declares an asynchronous coroutine function."),
        IntelliSenseItem("await", "await ", IntelliSenseType.KEYWORD, "Await coroutine", "Awaits an async coroutine result."),
        IntelliSenseItem("raise", "raise ValueError()", IntelliSenseType.KEYWORD, "Raise exception", "Raises an exception explicitly."),
        IntelliSenseItem("pass", "pass", IntelliSenseType.KEYWORD, "Pass statement", "Null operation statement; does nothing."),
        IntelliSenseItem("break", "break", IntelliSenseType.KEYWORD, "Break loop", "Terminates the nearest enclosing loop."),
        IntelliSenseItem("continue", "continue", IntelliSenseType.KEYWORD, "Continue loop", "Continues with the next iteration of the loop."),
        IntelliSenseItem("True", "True", IntelliSenseType.KEYWORD, "Boolean True", "Boolean true literal."),
        IntelliSenseItem("False", "False", IntelliSenseType.KEYWORD, "Boolean False", "Boolean false literal."),
        IntelliSenseItem("None", "None", IntelliSenseType.KEYWORD, "NoneType", "The sole value of the NoneType, representing absence of a value.")
    )

    private val BUILTINS = listOf(
        IntelliSenseItem("print", "print()", IntelliSenseType.BUILTIN, "(value, ..., sep=' ', end='\\n')", "Prints values to standard output stream."),
        IntelliSenseItem("input", "input()", IntelliSenseType.BUILTIN, "([prompt]) -> str", "Reads a string from standard input stream."),
        IntelliSenseItem("len", "len()", IntelliSenseType.BUILTIN, "(s) -> int", "Returns the number of items in a container."),
        IntelliSenseItem("range", "range()", IntelliSenseType.BUILTIN, "([start], stop, [step])", "Returns an immutable sequence of numbers."),
        IntelliSenseItem("enumerate", "enumerate()", IntelliSenseType.BUILTIN, "(iterable, start=0)", "Returns an enumerate object yielding (index, item)."),
        IntelliSenseItem("zip", "zip()", IntelliSenseType.BUILTIN, "(*iterables)", "Iterates over several iterables in parallel."),
        IntelliSenseItem("map", "map()", IntelliSenseType.BUILTIN, "(func, *iterables)", "Applies func to every item of iterable and returns iterator."),
        IntelliSenseItem("filter", "filter()", IntelliSenseType.BUILTIN, "(function, iterable)", "Yields items for which function(item) is true."),
        IntelliSenseItem("sum", "sum()", IntelliSenseType.BUILTIN, "(iterable, [start])", "Sums all items in iterable."),
        IntelliSenseItem("min", "min()", IntelliSenseType.BUILTIN, "(arg1, arg2, *args)", "Returns the smallest item."),
        IntelliSenseItem("max", "max()", IntelliSenseType.BUILTIN, "(arg1, arg2, *args)", "Returns the largest item."),
        IntelliSenseItem("abs", "abs()", IntelliSenseType.BUILTIN, "(x) -> number", "Returns the absolute value of a number."),
        IntelliSenseItem("round", "round()", IntelliSenseType.BUILTIN, "(number, [ndigits])", "Rounds number to ndigits decimal precision."),
        IntelliSenseItem("sorted", "sorted()", IntelliSenseType.BUILTIN, "(iterable, key=None, reverse=False)", "Returns a new sorted list."),
        IntelliSenseItem("reversed", "reversed()", IntelliSenseType.BUILTIN, "(sequence)", "Returns a reverse iterator over the sequence."),
        IntelliSenseItem("type", "type()", IntelliSenseType.BUILTIN, "(object) -> type", "Returns the type of an object."),
        IntelliSenseItem("isinstance", "isinstance()", IntelliSenseType.BUILTIN, "(object, classinfo) -> bool", "Checks if object is an instance of classinfo."),
        IntelliSenseItem("str", "str()", IntelliSenseType.BUILTIN, "(object='') -> str", "Creates a string representation."),
        IntelliSenseItem("int", "int()", IntelliSenseType.BUILTIN, "(x=0, base=10) -> int", "Converts number or string to integer."),
        IntelliSenseItem("float", "float()", IntelliSenseType.BUILTIN, "(x=0.0) -> float", "Converts number or string to floating point."),
        IntelliSenseItem("bool", "bool()", IntelliSenseType.BUILTIN, "(x=False) -> bool", "Returns boolean value of x."),
        IntelliSenseItem("list", "list()", IntelliSenseType.BUILTIN, "([iterable]) -> list", "Built-in mutable sequence constructor."),
        IntelliSenseItem("dict", "dict()", IntelliSenseType.BUILTIN, "([mapping]) -> dict", "Built-in key-value mapping constructor."),
        IntelliSenseItem("set", "set()", IntelliSenseType.BUILTIN, "([iterable]) -> set", "Built-in unordered unique elements collection."),
        IntelliSenseItem("tuple", "tuple()", IntelliSenseType.BUILTIN, "([iterable]) -> tuple", "Built-in immutable sequence constructor."),
        IntelliSenseItem("open", "open()", IntelliSenseType.BUILTIN, "(file, mode='r') -> file", "Opens file stream for I/O operations.")
    )

    private val SNIPPETS = listOf(
        IntelliSenseItem("main", "if __name__ == '__main__':\n    ", IntelliSenseType.SNIPPET, "Main entry point boilerplate", "Standard Python script entry block."),
        IntelliSenseItem("comprehension", "[x for x in items if condition]", IntelliSenseType.SNIPPET, "List comprehension", "Compact syntax for creating new lists."),
        IntelliSenseItem("dictcomp", "{k: v for k, v in items.items()}", IntelliSenseType.SNIPPET, "Dict comprehension", "Compact syntax for creating new dictionaries."),
        IntelliSenseItem("timer", "import time\nt0 = time.time()\n# code\nprint(f'Done in {time.time() - t0:.3f}s')", IntelliSenseType.SNIPPET, "Time execution block", "Calculates elapsed execution time.")
    )

    private val MODULES = listOf(
        IntelliSenseItem("math", "import math", IntelliSenseType.MODULE, "Module", "Standard math library: sqrt, sin, cos, pi, log, etc."),
        IntelliSenseItem("random", "import random", IntelliSenseType.MODULE, "Module", "Pseudo-random numbers, choice, shuffle, randint."),
        IntelliSenseItem("json", "import json", IntelliSenseType.MODULE, "Module", "JSON encoding and decoding utilities."),
        IntelliSenseItem("sys", "import sys", IntelliSenseType.MODULE, "Module", "System-specific parameters and functions."),
        IntelliSenseItem("time", "import time", IntelliSenseType.MODULE, "Module", "Time access and conversions."),
        IntelliSenseItem("datetime", "import datetime", IntelliSenseType.MODULE, "Module", "Basic date and time types.")
    )

    fun getSuggestions(code: String, cursorPosition: Int): List<IntelliSenseItem> {
        if (code.isEmpty() || cursorPosition <= 0) return emptyList()

        val safeCursor = cursorPosition.coerceIn(0, code.length)
        val textBeforeCursor = code.substring(0, safeCursor)

        // 1. Check dot member access: object.method or math.sqrt
        val dotMatch = Regex("""([a-zA-Z0-9_]+)\.([a-zA-Z0-9_]*)$""").find(textBeforeCursor)
        if (dotMatch != null) {
            val objectOrModule = dotMatch.groupValues[1]
            val methodPrefix = dotMatch.groupValues[2]
            val members = getMemberSuggestions(objectOrModule)
            return members
                .filter { it.label.startsWith(methodPrefix, ignoreCase = true) && !it.label.equals(methodPrefix, ignoreCase = true) }
                .take(12)
        }

        // 2. Standard word prefix before cursor
        val wordRegex = Regex("""[a-zA-Z0-9_]+$""")
        val match = wordRegex.find(textBeforeCursor) ?: return emptyList()
        val prefix = match.value

        if (prefix.isBlank()) return emptyList()

        // Extract user symbols (functions, classes, variables) defined in current code
        val userSymbols = extractUserSymbols(code)

        val allItems = KEYWORDS + BUILTINS + userSymbols + MODULES + SNIPPETS

        return allItems
            .filter { it.label.startsWith(prefix, ignoreCase = true) && !it.label.equals(prefix, ignoreCase = true) }
            .sortedBy {
                if (it.label.startsWith(prefix, ignoreCase = false)) 0 else 1
            }
            .take(12)
    }

    private fun getMemberSuggestions(target: String): List<IntelliSenseItem> {
        return when (target.lowercase()) {
            "math" -> listOf(
                IntelliSenseItem("sqrt", "sqrt()", IntelliSenseType.FUNCTION, "(x) -> float", "Return the square root of x.", -1),
                IntelliSenseItem("isqrt", "isqrt()", IntelliSenseType.FUNCTION, "(n) -> int", "Return the integer square root of n.", -1),
                IntelliSenseItem("sin", "sin()", IntelliSenseType.FUNCTION, "(x) -> float", "Return the sine of x (measured in radians).", -1),
                IntelliSenseItem("cos", "cos()", IntelliSenseType.FUNCTION, "(x) -> float", "Return the cosine of x (measured in radians).", -1),
                IntelliSenseItem("pi", "pi", IntelliSenseType.VARIABLE, "float", "The mathematical constant π = 3.141592..."),
                IntelliSenseItem("e", "e", IntelliSenseType.VARIABLE, "float", "The mathematical constant e = 2.718281..."),
                IntelliSenseItem("floor", "floor()", IntelliSenseType.FUNCTION, "(x) -> int", "Return the floor of x as an Integral.", -1),
                IntelliSenseItem("ceil", "ceil()", IntelliSenseType.FUNCTION, "(x) -> int", "Return the ceiling of x as an Integral.", -1),
                IntelliSenseItem("pow", "pow()", IntelliSenseType.FUNCTION, "(x, y) -> float", "Return x raised to the power y.", -1),
                IntelliSenseItem("log", "log()", IntelliSenseType.FUNCTION, "(x, [base]) -> float", "Return the logarithm of x to the given base.", -1)
            )
            "random" -> listOf(
                IntelliSenseItem("randint", "randint()", IntelliSenseType.FUNCTION, "(a, b) -> int", "Return random integer in range [a, b].", -1),
                IntelliSenseItem("choice", "choice()", IntelliSenseType.FUNCTION, "(seq)", "Choose a random element from a non-empty sequence.", -1),
                IntelliSenseItem("shuffle", "shuffle()", IntelliSenseType.FUNCTION, "(x)", "Shuffle list x in place.", -1),
                IntelliSenseItem("random", "random()", IntelliSenseType.FUNCTION, "() -> float", "Return the next random floating point number in [0.0, 1.0).", 0),
                IntelliSenseItem("sample", "sample()", IntelliSenseType.FUNCTION, "(population, k)", "Chooses k unique elements from population.", -1)
            )
            "sys" -> listOf(
                IntelliSenseItem("argv", "argv", IntelliSenseType.VARIABLE, "list[str]", "List of command line arguments passed to Python script."),
                IntelliSenseItem("version", "version", IntelliSenseType.VARIABLE, "str", "A string containing the version number of the Python interpreter."),
                IntelliSenseItem("exit", "exit()", IntelliSenseType.FUNCTION, "([status])", "Exit from Python with status code.", -1),
                IntelliSenseItem("path", "path", IntelliSenseType.VARIABLE, "list[str]", "List of directories specifying search path for modules.")
            )
            else -> listOf(
                IntelliSenseItem("append", "append()", IntelliSenseType.FUNCTION, "(object)", "Append object to the end of the list.", -1),
                IntelliSenseItem("extend", "extend()", IntelliSenseType.FUNCTION, "(iterable)", "Extend list by appending elements from the iterable.", -1),
                IntelliSenseItem("pop", "pop()", IntelliSenseType.FUNCTION, "([index])", "Remove and return item at index (default last).", 0),
                IntelliSenseItem("split", "split()", IntelliSenseType.FUNCTION, "([sep]) -> list", "Return a list of the words in the string.", 0),
                IntelliSenseItem("join", "join()", IntelliSenseType.FUNCTION, "(iterable) -> str", "Concatenate any number of strings.", -1),
                IntelliSenseItem("strip", "strip()", IntelliSenseType.FUNCTION, "() -> str", "Return a copy of the string with leading/trailing whitespace removed.", 0),
                IntelliSenseItem("lower", "lower()", IntelliSenseType.FUNCTION, "() -> str", "Return a copy of the string converted to lowercase.", 0),
                IntelliSenseItem("upper", "upper()", IntelliSenseType.FUNCTION, "() -> str", "Return a copy of the string converted to uppercase.", 0),
                IntelliSenseItem("replace", "replace()", IntelliSenseType.FUNCTION, "(old, new) -> str", "Return a copy with occurrences of substring replaced.", -1),
                IntelliSenseItem("keys", "keys()", IntelliSenseType.FUNCTION, "() -> dict_keys", "Return a set-like object providing a view on D's keys.", 0),
                IntelliSenseItem("values", "values()", IntelliSenseType.FUNCTION, "() -> dict_values", "Return an object providing a view on D's values.", 0),
                IntelliSenseItem("items", "items()", IntelliSenseType.FUNCTION, "() -> dict_items", "Return a set-like object providing a view on D's items.", 0),
                IntelliSenseItem("get", "get()", IntelliSenseType.FUNCTION, "(key, [default])", "Return the value for key if key is in the dictionary.", -1)
            )
        }
    }

    private fun extractUserSymbols(code: String): List<IntelliSenseItem> {
        val symbols = mutableListOf<IntelliSenseItem>()
        val seen = mutableSetOf<String>()

        // Find function definitions
        val fnRegex = Regex("""def\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\((.*?)\)""")
        fnRegex.findAll(code).forEach { match ->
            val name = match.groupValues[1]
            val params = match.groupValues[2]
            if (seen.add(name)) {
                symbols.add(
                    IntelliSenseItem(
                        label = name,
                        insertText = "$name()",
                        type = IntelliSenseType.FUNCTION,
                        detail = "def $name($params)",
                        docstring = "User defined function."
                    )
                )
            }
        }

        // Find class definitions
        val classRegex = Regex("""class\s+([a-zA-Z_][a-zA-Z0-9_]*)""")
        classRegex.findAll(code).forEach { match ->
            val name = match.groupValues[1]
            if (seen.add(name)) {
                symbols.add(
                    IntelliSenseItem(
                        label = name,
                        insertText = name,
                        type = IntelliSenseType.CLASS,
                        detail = "class $name",
                        docstring = "User defined class."
                    )
                )
            }
        }

        // Find variable declarations
        val varRegex = Regex("""([a-zA-Z_][a-zA-Z0-9_]*)\s*=""")
        varRegex.findAll(code).forEach { match ->
            val name = match.groupValues[1]
            if (name !in setOf("if", "elif", "while", "for") && seen.add(name)) {
                symbols.add(
                    IntelliSenseItem(
                        label = name,
                        insertText = name,
                        type = IntelliSenseType.VARIABLE,
                        detail = "variable",
                        docstring = "Local or global variable."
                    )
                )
            }
        }

        return symbols
    }

    /**
     * Smart Auto-Close Bracket / Quote logic
     * Returns the pair: (newText, newCursorPosition)
     */
    fun handleAutoClose(code: String, cursor: Int, insertedChar: Char): Pair<String, Int>? {
        val safeCursor = cursor.coerceIn(0, code.length)
        val closingChar = when (insertedChar) {
            '(' -> ')'
            '[' -> ']'
            '{' -> '}'
            '"' -> '"'
            '\'' -> '\''
            else -> return null
        }

        val newText = StringBuilder(code)
            .insert(safeCursor, insertedChar)
            .insert(safeCursor + 1, closingChar)
            .toString()

        return Pair(newText, safeCursor + 1)
    }

    /**
     * Smart Auto-Indent upon newline
     */
    fun handleAutoIndent(code: String, cursor: Int): Pair<String, Int> {
        val safeCursor = cursor.coerceIn(0, code.length)
        val lineStart = code.lastIndexOf('\n', (safeCursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val currentLineBeforeCursor = code.substring(lineStart, safeCursor)
        val leadingSpaces = currentLineBeforeCursor.takeWhile { it == ' ' }.length

        val shouldIncreaseIndent = currentLineBeforeCursor.trimEnd().endsWith(":")
        val indentAmount = if (shouldIncreaseIndent) leadingSpaces + 4 else leadingSpaces
        val indentStr = "\n" + " ".repeat(indentAmount)

        val newText = StringBuilder(code).insert(safeCursor, indentStr).toString()
        return Pair(newText, safeCursor + indentStr.length)
    }
}
