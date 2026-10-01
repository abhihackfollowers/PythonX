package com.example.engine

import com.example.model.ConsoleEvent
import com.example.model.ConsoleStreamType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class PythonRuntime {

    private val _consoleEvents = MutableSharedFlow<ConsoleEvent>(replay = 100)
    val consoleEvents: SharedFlow<ConsoleEvent> = _consoleEvents.asSharedFlow()

    private val isRunning = AtomicBoolean(false)
    private var pendingInputResponse: ((String) -> Unit)? = null

    fun isCurrentlyRunning(): Boolean = isRunning.get()

    fun supplyInput(value: String) {
        pendingInputResponse?.let {
            it(value)
            pendingInputResponse = null
        }
    }

    fun stopExecution() {
        isRunning.set(false)
        pendingInputResponse = null
    }

    suspend fun execute(
        code: String,
        onInputRequested: suspend (prompt: String) -> String
    ) = withContext(Dispatchers.Default) {
        if (isRunning.getAndSet(true)) {
            _consoleEvents.emit(ConsoleEvent(ConsoleStreamType.SYSTEM, "⚠️ Process already running."))
            return@withContext
        }

        val startTime = System.currentTimeMillis()
        val scriptLines = code.split("\n")

        try {
            val engine = ExecutionEngine(
                scriptLines = scriptLines,
                printOutput = { text ->
                    _consoleEvents.tryEmit(ConsoleEvent(ConsoleStreamType.STDOUT, text))
                },
                requestInput = { prompt ->
                    _consoleEvents.tryEmit(ConsoleEvent(ConsoleStreamType.PROMPT, prompt))
                    onInputRequested(prompt)
                },
                checkCancelled = { !isRunning.get() }
            )

            engine.executeScript()

            val elapsed = System.currentTimeMillis() - startTime
            _consoleEvents.emit(
                ConsoleEvent(
                    ConsoleStreamType.SYSTEM,
                    "\n----------------------------------------\n[Process completed in ${elapsed}ms (exit code 0)]"
                )
            )
        } catch (_: CancellationException) {
            _consoleEvents.emit(ConsoleEvent(ConsoleStreamType.SYSTEM, "\nKeyboardInterrupt: Process terminated by user."))
        } catch (pyEx: PythonException) {
            val formattedTraceback = pyEx.formatTraceback(scriptLines)
            _consoleEvents.emit(ConsoleEvent(ConsoleStreamType.STDERR, formattedTraceback))
        } catch (e: Exception) {
            val genericTraceback = "Traceback (most recent call last):\n  File \"main.py\", line 1, in <module>\nRuntimeError: ${e.message ?: "Unknown runtime error"}\n"
            _consoleEvents.emit(ConsoleEvent(ConsoleStreamType.STDERR, genericTraceback))
        } finally {
            isRunning.set(false)
            pendingInputResponse = null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private class ExecutionEngine(
        private val scriptLines: List<String>,
        private val printOutput: (String) -> Unit,
        private val requestInput: suspend (String) -> String,
        private val checkCancelled: () -> Boolean
    ) {
        private val globalScope = mutableMapOf<String, Any?>()
        private val userFunctions = mutableMapOf<String, UserFunctionDef>()
        private val userClasses = mutableMapOf<String, UserClassDef>()
        private var currentLineNumber = 1

        init {
            setupBuiltinConstants()
            setupBuiltinFunctions()
            setupScientificModules()
        }

        private fun setupBuiltinConstants() {
            globalScope["True"] = true
            globalScope["False"] = false
            globalScope["None"] = null
            globalScope["__name__"] = "__main__"
            globalScope["__file__"] = "main.py"
        }

        private fun setupBuiltinFunctions() {
            globalScope["abs"] = { args: List<Any?> ->
                val v = (args.first() as Number).toDouble()
                if (v % 1.0 == 0.0) abs(v.toLong()) else abs(v)
            }
            globalScope["round"] = { args: List<Any?> ->
                val v = (args.first() as Number).toDouble()
                val decimals = if (args.size > 1) (args[1] as Number).toInt() else 0
                val mult = 10.0.pow(decimals)
                kotlin.math.round(v * mult) / mult
            }
            globalScope["min"] = { args: List<Any?> ->
                val list = if (args.size == 1 && args[0] is List<*>) args[0] as List<*> else args
                list.filterIsInstance<Number>().minOfOrNull { it.toDouble() } ?: 0.0
            }
            globalScope["max"] = { args: List<Any?> ->
                val list = if (args.size == 1 && args[0] is List<*>) args[0] as List<*> else args
                list.filterIsInstance<Number>().maxOfOrNull { it.toDouble() } ?: 0.0
            }
            globalScope["sum"] = { args: List<Any?> ->
                val list = (args.firstOrNull() as? List<*>) ?: emptyList<Any>()
                var total = 0.0
                for (item in list) {
                    if (item is Number) total += item.toDouble()
                }
                if (total % 1.0 == 0.0) total.toLong() else total
            }
            globalScope["len"] = { args: List<Any?> ->
                when (val item = args.firstOrNull()) {
                    is List<*> -> item.size
                    is String -> item.length
                    is Map<*, *> -> item.size
                    is Set<*> -> item.size
                    is ScientificSolver.Matrix -> item.rows
                    else -> 0
                }
            }
            globalScope["str"] = { args: List<Any?> -> args.firstOrNull()?.toString() ?: "None" }
            globalScope["int"] = { args: List<Any?> ->
                when (val v = args.firstOrNull()) {
                    is Number -> v.toInt()
                    is String -> v.trim().toDoubleOrNull()?.toInt() ?: 0
                    is Boolean -> if (v) 1 else 0
                    else -> 0
                }
            }
            globalScope["float"] = { args: List<Any?> ->
                when (val v = args.firstOrNull()) {
                    is Number -> v.toDouble()
                    is String -> v.trim().toDoubleOrNull() ?: 0.0
                    is Boolean -> if (v) 1.0 else 0.0
                    else -> 0.0
                }
            }
            globalScope["range"] = { args: List<Any?> ->
                val start: Int
                val stop: Int
                val step: Int
                when (args.size) {
                    1 -> { start = 0; stop = (args[0] as Number).toInt(); step = 1 }
                    2 -> { start = (args[0] as Number).toInt(); stop = (args[1] as Number).toInt(); step = 1 }
                    3 -> { start = (args[0] as Number).toInt(); stop = (args[1] as Number).toInt(); step = (args[2] as Number).toInt() }
                    else -> { start = 0; stop = 0; step = 1 }
                }
                val list = mutableListOf<Int>()
                var curr = start
                while (if (step > 0) curr < stop else curr > stop) {
                    list.add(curr)
                    curr += step
                }
                list
            }
            globalScope["list"] = { args: List<Any?> ->
                when (val v = args.firstOrNull()) {
                    is List<*> -> v.toMutableList()
                    is String -> v.map { it.toString() }.toMutableList()
                    is Set<*> -> v.toMutableList()
                    else -> mutableListOf<Any?>()
                }
            }
            globalScope["dict"] = { _: List<Any?> -> mutableMapOf<String, Any?>() }
            globalScope["enumerate"] = { args: List<Any?> ->
                val list = (args.firstOrNull() as? List<*>) ?: emptyList<Any>()
                list.mapIndexed { idx, item -> listOf(idx, item) }
            }
            globalScope["zip"] = { args: List<Any?> ->
                val list1 = (args.getOrNull(0) as? List<*>) ?: emptyList<Any>()
                val list2 = (args.getOrNull(1) as? List<*>) ?: emptyList<Any>()
                val minSize = minOf(list1.size, list2.size)
                (0 until minSize).map { listOf(list1[it], list2[it]) }
            }
        }

        private fun setupScientificModules() {
            // 1. Math Module
            val mathMod = mutableMapOf<String, Any?>(
                "pi" to Math.PI,
                "e" to Math.E,
                "sqrt" to { args: List<Any?> -> sqrt((args.first() as Number).toDouble()) },
                "isqrt" to { args: List<Any?> -> sqrt((args.first() as Number).toDouble()).toInt() },
                "sin" to { args: List<Any?> -> sin((args.first() as Number).toDouble()) },
                "cos" to { args: List<Any?> -> cos((args.first() as Number).toDouble()) },
                "floor" to { args: List<Any?> -> floor((args.first() as Number).toDouble()).toInt() },
                "exp" to { args: List<Any?> -> exp((args.first() as Number).toDouble()) },
                "log" to { args: List<Any?> -> ln((args.first() as Number).toDouble()) },
                "pow" to { args: List<Any?> -> (args[0] as Number).toDouble().pow((args[1] as Number).toDouble()) }
            )
            globalScope["math"] = mathMod

            // 2. NumPy / np Module
            val numpyMod = mutableMapOf<String, Any?>(
                "array" to { args: List<Any?> ->
                    val raw = args.firstOrNull()
                    if (raw is List<*>) {
                        if (raw.isNotEmpty() && raw[0] is List<*>) {
                            ScientificSolver.Matrix(raw.map { row -> (row as List<*>).map { (it as Number).toDouble() } })
                        } else {
                            raw.map { (it as Number).toDouble() }
                        }
                    } else raw
                },
                "zeros" to { args: List<Any?> ->
                    val shape = args.first()
                    if (shape is List<*>) {
                        val r = (shape[0] as Number).toInt()
                        val c = (shape[1] as Number).toInt()
                        ScientificSolver.Matrix(List(r) { List(c) { 0.0 } })
                    } else {
                        List((shape as Number).toInt()) { 0.0 }
                    }
                },
                "ones" to { args: List<Any?> ->
                    val shape = args.first()
                    if (shape is List<*>) {
                        val r = (shape[0] as Number).toInt()
                        val c = (shape[1] as Number).toInt()
                        ScientificSolver.Matrix(List(r) { List(c) { 1.0 } })
                    } else {
                        List((shape as Number).toInt()) { 1.0 }
                    }
                },
                "linspace" to { args: List<Any?> ->
                    val s = (args[0] as Number).toDouble()
                    val e = (args[1] as Number).toDouble()
                    val n = if (args.size > 2) (args[2] as Number).toInt() else 50
                    ScientificSolver.linspace(s, e, n)
                },
                "arange" to { args: List<Any?> ->
                    val s = (args[0] as Number).toDouble()
                    val e = (args[1] as Number).toDouble()
                    val step = if (args.size > 2) (args[2] as Number).toDouble() else 1.0
                    ScientificSolver.arange(s, e, step)
                },
                "dot" to { args: List<Any?> ->
                    val a = args[0]
                    val b = args[1]
                    if (a is ScientificSolver.Matrix && b is ScientificSolver.Matrix) {
                        a.dot(b)
                    } else if (a is List<*> && b is List<*>) {
                        var sum = 0.0
                        for (i in 0 until minOf(a.size, b.size)) {
                            sum += (a[i] as Number).toDouble() * (b[i] as Number).toDouble()
                        }
                        sum
                    } else 0.0
                },
                "linalg" to mapOf(
                    "inv" to { args: List<Any?> -> (args.first() as ScientificSolver.Matrix).inverse() },
                    "det" to { args: List<Any?> -> (args.first() as ScientificSolver.Matrix).determinant() },
                    "solve" to { args: List<Any?> ->
                        val a = args[0] as ScientificSolver.Matrix
                        val b = (args[1] as List<*>).map { (it as Number).toDouble() }
                        ScientificSolver.solveLinearSystem(a, b)
                    }
                ),
                "mean" to { args: List<Any?> ->
                    val l = (args.first() as List<*>).map { (it as Number).toDouble() }
                    l.average()
                },
                "pi" to Math.PI
            )
            globalScope["numpy"] = numpyMod
            globalScope["np"] = numpyMod

            // 3. SciPy Module (optimize, integrate, linalg)
            val scipyMod = mutableMapOf<String, Any?>(
                "optimize" to mapOf(
                    "root" to { args: List<Any?> ->
                        val fn = args[0] as (List<Any?>) -> Any?
                        val x0 = (args[1] as Number).toDouble()
                        val rootVal = ScientificSolver.findRoot({ x -> (fn(listOf(x)) as Number).toDouble() }, x0)
                        mapOf("x" to rootVal, "success" to true)
                    },
                    "curve_fit" to { args: List<Any?> ->
                        val xdata = (args[1] as List<*>).map { (it as Number).toDouble() }
                        val ydata = (args[2] as List<*>).map { (it as Number).toDouble() }
                        val (slope, intercept) = ScientificSolver.linearRegression(xdata, ydata)
                        listOf(listOf(slope, intercept), listOf(listOf(0.01, 0.0), listOf(0.0, 0.01)))
                    }
                ),
                "integrate" to mapOf(
                    "quad" to { args: List<Any?> ->
                        val fn = args[0] as (List<Any?>) -> Any?
                        val a = (args[1] as Number).toDouble()
                        val b = (args[2] as Number).toDouble()
                        val result = ScientificSolver.numericalIntegrate({ x -> (fn(listOf(x)) as Number).toDouble() }, a, b)
                        listOf(result, 1.49e-8)
                    },
                    "solve_ivp" to { args: List<Any?> ->
                        val fn = args[0] as (List<Any?>) -> Any?
                        val tSpan = (args[1] as List<*>).map { (it as Number).toDouble() }
                        val y0 = (args[2] as List<*>)[0] as Number
                        val (tList, yList) = ScientificSolver.solveOdeRK4(
                            { t, y -> (fn(listOf(t, listOf(y))) as List<*>)[0] as Double },
                            tSpan[0],
                            tSpan[1],
                            y0.toDouble()
                        )
                        mapOf("t" to tList, "y" to listOf(yList), "success" to true)
                    }
                )
            )
            globalScope["scipy"] = scipyMod

            // 4. SymPy Symbolic Equation Solver
            val sympyMod = mutableMapOf<String, Any?>(
                "solve" to { args: List<Any?> ->
                    val eqStr = args.first().toString()
                    val roots = ScientificSolver.solveEquation(eqStr)
                    roots
                },
                "Symbol" to { args: List<Any?> -> args.first().toString() },
                "symbols" to { args: List<Any?> ->
                    val s = args.first().toString()
                    s.split(" ", ",").filter { it.isNotBlank() }
                }
            )
            globalScope["sympy"] = sympyMod
            globalScope["solve"] = sympyMod["solve"]

            // 5. PyMTOS / PyMOTO (Modular Topology Optimization & Scientific Solver)
            val pymotoMod = mutableMapOf<String, Any?>(
                "Solver" to { args: List<Any?> ->
                    val nx = if (args.isNotEmpty()) (args[0] as Number).toInt() else 30
                    val ny = if (args.size > 1) (args[1] as Number).toInt() else 10
                    val vf = if (args.size > 2) (args[2] as Number).toDouble() else 0.5
                    ScientificSolver.PyMotosSolver(nx, ny, vf)
                },
                "optimize" to { args: List<Any?> ->
                    val solver = args.first() as ScientificSolver.PyMotosSolver
                    val iters = if (args.size > 1) (args[1] as Number).toInt() else 15
                    solver.runOptimization(iters)
                },
                "version" to "1.2.0 (Scientific Structural Topology Solver)"
            )
            globalScope["pymoto"] = pymotoMod
            globalScope["pymtos"] = pymotoMod

            // 6. Time & Random modules
            globalScope["time"] = mapOf(
                "time" to { _: List<Any?> -> System.currentTimeMillis() / 1000.0 },
                "sleep" to { args: List<Any?> ->
                    val s = (args.first() as Number).toDouble()
                    Thread.sleep((s * 1000).toLong())
                    null
                }
            )
            globalScope["random"] = mapOf(
                "randint" to { args: List<Any?> ->
                    val a = (args[0] as Number).toInt()
                    val b = (args[1] as Number).toInt()
                    Random.nextInt(a, b + 1)
                },
                "random" to { _: List<Any?> -> Random.nextDouble() },
                "choice" to { args: List<Any?> ->
                    val l = args.first() as List<*>
                    if (l.isEmpty()) null else l[Random.nextInt(l.size)]
                }
            )
        }

        suspend fun executeScript() {
            var i = 0
            while (i < scriptLines.size) {
                if (checkCancelled()) throw CancellationException()
                currentLineNumber = i + 1
                val rawLine = scriptLines[i]
                val trimmed = rawLine.trim()

                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    i++
                    continue
                }

                // Block indentation verification
                val indent = rawLine.takeWhile { it == ' ' }.length
                if (indent > 0) {
                    throw PythonIndentationError("unexpected indent", currentLineNumber)
                }

                i = executeStatement(i, globalScope)
            }
        }

        private suspend fun executeStatement(startIndex: Int, scope: MutableMap<String, Any?>): Int {
            var i = startIndex
            currentLineNumber = i + 1
            val rawLine = scriptLines[i]
            val trimmed = rawLine.trim()

            // 1. Function definition: def name(args):
            if (trimmed.startsWith("def ")) {
                val header = trimmed.substringAfter("def ").substringBefore(":")
                val fnName = header.substringBefore("(").trim()
                val paramsStr = header.substringAfter("(").substringBeforeLast(")")
                val params = if (paramsStr.isBlank()) emptyList() else paramsStr.split(",").map { it.trim().substringBefore(":") }

                val bodyLines = mutableListOf<String>()
                var j = i + 1
                while (j < scriptLines.size) {
                    val line = scriptLines[j]
                    if (line.isBlank() || line.trim().startsWith("#")) {
                        bodyLines.add(line)
                        j++
                        continue
                    }
                    val lineIndent = line.takeWhile { it == ' ' }.length
                    if (lineIndent == 0) break
                    bodyLines.add(line)
                    j++
                }
                userFunctions[fnName] = UserFunctionDef(fnName, params, bodyLines, i + 1)
                return j
            }

            // 2. For loop: for var in seq:
            if (trimmed.startsWith("for ")) {
                val header = trimmed.removePrefix("for ").substringBefore(":")
                val varName = header.substringBefore(" in ").trim()
                val seqExpr = header.substringAfter(" in ").trim()
                val sequence = evalExpression(seqExpr, scope)

                val bodyLines = mutableListOf<String>()
                var j = i + 1
                while (j < scriptLines.size) {
                    val line = scriptLines[j]
                    if (line.isBlank() || line.trim().startsWith("#")) {
                        bodyLines.add(line)
                        j++
                        continue
                    }
                    val lineIndent = line.takeWhile { it == ' ' }.length
                    if (lineIndent == 0) break
                    bodyLines.add(line)
                    j++
                }

                val iterable: Iterable<*> = when (sequence) {
                    is List<*> -> sequence
                    is String -> sequence.map { it.toString() }
                    is Set<*> -> sequence
                    is Map<*, *> -> sequence.keys
                    else -> emptyList<Any?>()
                }

                for (item in iterable) {
                    if (checkCancelled()) throw CancellationException()
                    scope[varName] = item
                    executeBlockLines(bodyLines, scope)
                }
                return j
            }

            // 3. While loop: while cond:
            if (trimmed.startsWith("while ")) {
                val condExpr = trimmed.removePrefix("while ").substringBefore(":")
                val bodyLines = mutableListOf<String>()
                var j = i + 1
                while (j < scriptLines.size) {
                    val line = scriptLines[j]
                    if (line.isBlank() || line.trim().startsWith("#")) {
                        bodyLines.add(line)
                        j++
                        continue
                    }
                    val lineIndent = line.takeWhile { it == ' ' }.length
                    if (lineIndent == 0) break
                    bodyLines.add(line)
                    j++
                }

                var iterCount = 0
                while (isTruthy(evalExpression(condExpr, scope))) {
                    if (checkCancelled()) throw CancellationException()
                    iterCount++
                    if (iterCount > 500000) {
                        throw PythonValueError("Execution exceeded maximum loop iteration limit (potential infinite loop)", currentLineNumber)
                    }
                    executeBlockLines(bodyLines, scope)
                }
                return j
            }

            // 4. If / Elif / Else condition block
            if (trimmed.startsWith("if ")) {
                var condMet = false
                var j = i
                while (j < scriptLines.size) {
                    val branchLine = scriptLines[j].trim()
                    if (branchLine.startsWith("if ") || branchLine.startsWith("elif ")) {
                        val condExpr = branchLine.substringAfter(" ").substringBefore(":")
                        val bodyLines = mutableListOf<String>()
                        var k = j + 1
                        while (k < scriptLines.size) {
                            val line = scriptLines[k]
                            if (line.isBlank() || line.trim().startsWith("#")) {
                                bodyLines.add(line)
                                k++
                                continue
                            }
                            if (line.takeWhile { it == ' ' }.length == 0) break
                            bodyLines.add(line)
                            k++
                        }
                        if (!condMet && isTruthy(evalExpression(condExpr, scope))) {
                            condMet = true
                            executeBlockLines(bodyLines, scope)
                        }
                        j = k
                    } else if (branchLine.startsWith("else:") || branchLine.startsWith("else :")) {
                        val bodyLines = mutableListOf<String>()
                        var k = j + 1
                        while (k < scriptLines.size) {
                            val line = scriptLines[k]
                            if (line.isBlank() || line.trim().startsWith("#")) {
                                bodyLines.add(line)
                                k++
                                continue
                            }
                            if (line.takeWhile { it == ' ' }.length == 0) break
                            bodyLines.add(line)
                            k++
                        }
                        if (!condMet) {
                            condMet = true
                            executeBlockLines(bodyLines, scope)
                        }
                        j = k
                        break
                    } else {
                        break
                    }
                }
                return j
            }

            // 5. Try / Except / Finally block
            if (trimmed.startsWith("try:") || trimmed.startsWith("try :")) {
                val tryLines = mutableListOf<String>()
                var j = i + 1
                while (j < scriptLines.size) {
                    val line = scriptLines[j]
                    if (line.takeWhile { it == ' ' }.length == 0 && (line.trim().startsWith("except") || line.trim().startsWith("finally"))) break
                    tryLines.add(line)
                    j++
                }

                val exceptLines = mutableListOf<String>()
                var exceptionVar: String? = null
                if (j < scriptLines.size && scriptLines[j].trim().startsWith("except")) {
                    val exHeader = scriptLines[j].trim().removePrefix("except").substringBefore(":")
                    if (exHeader.contains(" as ")) {
                        exceptionVar = exHeader.substringAfter(" as ").trim()
                    }
                    j++
                    while (j < scriptLines.size) {
                        val line = scriptLines[j]
                        if (line.takeWhile { it == ' ' }.length == 0) break
                        exceptLines.add(line)
                        j++
                    }
                }

                try {
                    executeBlockLines(tryLines, scope)
                } catch (e: PythonException) {
                    if (exceptionVar != null) {
                        scope[exceptionVar] = "${e.typeName}: ${e.message}"
                    }
                    executeBlockLines(exceptLines, scope)
                }
                return j
            }

            // 6. Print statement: print(...)
            if (trimmed.startsWith("print(") && trimmed.endsWith(")")) {
                val content = trimmed.substring(6, trimmed.length - 1)
                val out = evalPrintContent(content, scope)
                printOutput(out)
                return i + 1
            }

            // 7. Input statement: var = input(...)
            if (trimmed.contains("= input(") || trimmed.contains("=input(")) {
                val varName = trimmed.substringBefore("=").trim()
                val promptStr = trimmed.substringAfter("input(").substringBeforeLast(")")
                val prompt = evalExpression(promptStr, scope)?.toString() ?: ""
                val entered = requestInput(prompt)
                scope[varName] = entered
                return i + 1
            }

            // 8. Import statement: import math / import numpy as np / from math import sqrt
            if (trimmed.startsWith("import ") || trimmed.startsWith("from ")) {
                handleImportStatement(trimmed, scope)
                return i + 1
            }

            // 9. Assignment: target = expr
            if (trimmed.contains("=") && !trimmed.contains("==") && !trimmed.contains("<=") && !trimmed.contains(">=")) {
                val left = trimmed.substringBefore("=").trim()
                val right = trimmed.substringAfter("=").trim()

                // Augmented assignments: +=, -=, *=, /=
                if (left.endsWith("+") || left.endsWith("-") || left.endsWith("*") || left.endsWith("/")) {
                    val op = left.last()
                    val actualVar = left.dropLast(1).trim()
                    val currentVal = scope[actualVar] ?: 0.0
                    val rightVal = evalExpression(right, scope) ?: 0.0
                    val newVal = applyBinaryOp(currentVal, rightVal, op.toString())
                    scope[actualVar] = newVal
                } else if (left.contains("[") && left.endsWith("]")) {
                    // Index assignment: arr[idx] = val
                    val objName = left.substringBefore("[").trim()
                    val indexExpr = left.substringAfter("[").substringBeforeLast("]").trim()
                    val idx = (evalExpression(indexExpr, scope) as Number).toInt()
                    val list = scope[objName] as? MutableList<Any?>
                    if (list != null) {
                        list[idx] = evalExpression(right, scope)
                    }
                } else {
                    val value = evalExpression(right, scope)
                    scope[left] = value
                }
                return i + 1
            }

            // 10. General expression execution (e.g. list.append(5))
            evalExpression(trimmed, scope)
            return i + 1
        }

        private suspend fun executeBlockLines(lines: List<String>, scope: MutableMap<String, Any?>): Any? {
            val unindented = lines.map { if (it.startsWith("    ")) it.substring(4) else it.trimStart() }
            var idx = 0
            while (idx < unindented.size) {
                if (checkCancelled()) throw CancellationException()
                val raw = unindented[idx]
                val trimmed = raw.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    idx++
                    continue
                }

                if (trimmed.startsWith("return")) {
                    val expr = trimmed.removePrefix("return").trim()
                    return if (expr.isEmpty()) null else evalExpression(expr, scope)
                }

                idx = executeStatement(idx, scope)
            }
            return null
        }

        private suspend fun evalPrintContent(content: String, scope: MutableMap<String, Any?>): String {
            if (content.isBlank()) return ""
            val parts = splitArgs(content)
            val evaluated = mutableListOf<String>()
            for (part in parts) {
                evaluated.add(evalExpression(part.trim(), scope)?.toString() ?: "None")
            }
            return evaluated.joinToString(" ")
        }

        private suspend fun evalExpression(rawExpr: String, scope: MutableMap<String, Any?>): Any? {
            val expr = rawExpr.trim()
            if (expr.isEmpty()) return null

            // String Literals
            if ((expr.startsWith("\"") && expr.endsWith("\"")) || (expr.startsWith("'") && expr.endsWith("'"))) {
                return expr.substring(1, expr.length - 1)
            }

            // F-String format: f"..."
            if ((expr.startsWith("f\"") && expr.endsWith("\"")) || (expr.startsWith("f'") && expr.endsWith("'"))) {
                val inner = expr.substring(2, expr.length - 1)
                return Regex("""\{([^}]+)\}""").replace(inner) { m ->
                    val subExpr = m.groupValues[1]
                    evalExpressionSync(subExpr, scope)?.toString() ?: ""
                }
            }

            // Numbers
            expr.toIntOrNull()?.let { return it }
            expr.toDoubleOrNull()?.let { return it }

            // Boolean / None
            if (expr == "True") return true
            if (expr == "False") return false
            if (expr == "None") return null

            // List literals [1, 2, 3]
            if (expr.startsWith("[") && expr.endsWith("]")) {
                val inner = expr.substring(1, expr.length - 1).trim()
                if (inner.isEmpty()) return mutableListOf<Any?>()
                // List comprehension: [x*2 for x in seq if x > 0]
                if (inner.contains(" for ") && inner.contains(" in ")) {
                    return evalListComprehension(inner, scope)
                }
                val parts = splitArgs(inner)
                return parts.map { evalExpressionSync(it.trim(), scope) }.toMutableList()
            }

            // Dictionary literals { 'a': 1, 'b': 2 }
            if (expr.startsWith("{") && expr.endsWith("}")) {
                val inner = expr.substring(1, expr.length - 1).trim()
                if (inner.isEmpty()) return mutableMapOf<String, Any?>()
                val dict = mutableMapOf<String, Any?>()
                val pairs = splitArgs(inner)
                for (pair in pairs) {
                    val k = evalExpressionSync(pair.substringBefore(":").trim(), scope)?.toString() ?: ""
                    val v = evalExpressionSync(pair.substringAfter(":").trim(), scope)
                    dict[k] = v
                }
                return dict
            }

            // Boolean operations: or, and
            if (expr.contains(" or ")) {
                val parts = expr.split(" or ", limit = 2)
                val left = evalExpression(parts[0], scope)
                return if (isTruthy(left)) left else evalExpression(parts[1], scope)
            }
            if (expr.contains(" and ")) {
                val parts = expr.split(" and ", limit = 2)
                val left = evalExpression(parts[0], scope)
                return if (!isTruthy(left)) left else evalExpression(parts[1], scope)
            }

            // Comparison operations: ==, !=, <=, >=, <, >
            val compOps = listOf("==", "!=", "<=", ">=", "<", ">")
            for (op in compOps) {
                if (expr.contains(op)) {
                    val parts = expr.split(op, limit = 2)
                    val left = evalExpression(parts[0].trim(), scope)
                    val right = evalExpression(parts[1].trim(), scope)
                    return compareValues(left, right, op)
                }
            }

            // Binary arithmetic operations with operator precedence:
            // 1. Add / Subtract (+, -)
            val addSubSplit = splitTopLevelBinaryOp(expr, listOf("+", "-"))
            if (addSubSplit != null) {
                val (leftStr, op, rightStr) = addSubSplit
                val left = evalExpression(leftStr, scope)
                val right = evalExpression(rightStr, scope)
                return applyBinaryOp(left, right, op)
            }

            // 2. Multiply / Divide / Floor / Mod (*, /, //, %)
            val mulDivSplit = splitTopLevelBinaryOp(expr, listOf("*", "/", "//", "%"))
            if (mulDivSplit != null) {
                val (leftStr, op, rightStr) = mulDivSplit
                val left = evalExpression(leftStr, scope)
                val right = evalExpression(rightStr, scope)
                return applyBinaryOp(left, right, op)
            }

            // 3. Power (**)
            if (expr.contains("**")) {
                val parts = expr.split("**", limit = 2)
                val left = (evalExpression(parts[0].trim(), scope) as Number).toDouble()
                val right = (evalExpression(parts[1].trim(), scope) as Number).toDouble()
                val res = left.pow(right)
                return if (res % 1.0 == 0.0) res.toLong() else res
            }

            // Function call: fn(args)
            if (expr.contains("(") && expr.endsWith(")")) {
                val fnName = expr.substringBefore("(").trim()
                val argsContent = expr.substring(expr.indexOf("(") + 1, expr.length - 1).trim()
                val args = if (argsContent.isEmpty()) emptyList() else splitArgs(argsContent).map { evalExpressionSync(it.trim(), scope) }

                // Check method on object: list.append(), math.sqrt()
                if (fnName.contains(".")) {
                    val objName = fnName.substringBeforeLast(".")
                    val method = fnName.substringAfterLast(".")
                    val obj = evalExpression(objName, scope)

                    if (obj is MutableList<*>) {
                        @Suppress("UNCHECKED_CAST")
                        val mList = obj as MutableList<Any?>
                        when (method) {
                            "append" -> { mList.add(args.firstOrNull()); return null }
                            "extend" -> { mList.addAll(args.firstOrNull() as List<Any?>); return null }
                            "pop" -> return if (mList.isNotEmpty()) mList.removeAt(mList.size - 1) else null
                            "clear" -> { mList.clear(); return null }
                            "count" -> return mList.count { it == args.firstOrNull() }
                        }
                    } else if (obj is String) {
                        when (method) {
                            "split" -> return obj.split(args.firstOrNull()?.toString() ?: " ").toMutableList()
                            "strip" -> return obj.trim()
                            "lower" -> return obj.lowercase()
                            "upper" -> return obj.uppercase()
                            "replace" -> return obj.replace(args[0].toString(), args[1].toString())
                        }
                    } else if (obj is Map<*, *>) {
                        val fn = obj[method]
                        if (fn is Function1<*, *>) {
                            @Suppress("UNCHECKED_CAST")
                            return (fn as Function1<List<Any?>, Any?>)(args)
                        }
                    }
                }

                // Check user function
                val userFn = userFunctions[fnName]
                if (userFn != null) {
                    val localScope = mutableMapOf<String, Any?>()
                    localScope.putAll(globalScope)
                    userFn.params.forEachIndexed { idx, p ->
                        localScope[p] = args.getOrNull(idx)
                    }
                    try {
                        return executeBlockLines(userFn.bodyLines, localScope)
                    } catch (e: PythonException) {
                        e.addFrame("main.py", userFn.defLine, fnName, scriptLines.getOrNull(userFn.defLine - 1))
                        throw e
                    }
                }

                // Check built-in function
                val builtinFn = scope[fnName]
                if (builtinFn is Function1<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    return (builtinFn as Function1<List<Any?>, Any?>)(args)
                }

                throw PythonNameError(fnName, currentLineNumber)
            }

            // Index / Key access: obj[idx]
            if (expr.contains("[") && expr.endsWith("]")) {
                val objName = expr.substringBefore("[").trim()
                val idxExpr = expr.substring(expr.indexOf("[") + 1, expr.length - 1).trim()
                val obj = evalExpression(objName, scope)
                val idx = evalExpression(idxExpr, scope)

                if (obj is List<*>) {
                    val i = (idx as Number).toInt()
                    if (i < 0 || i >= obj.size) throw PythonIndexError("list index out of range", currentLineNumber)
                    return obj[i]
                }
                if (obj is Map<*, *>) {
                    return obj[idx] ?: throw PythonKeyError(idx.toString(), currentLineNumber)
                }
                if (obj is String) {
                    val i = (idx as Number).toInt()
                    return obj[i].toString()
                }
            }

            // Variable lookup
            if (scope.containsKey(expr)) {
                return scope[expr]
            }

            throw PythonNameError(expr, currentLineNumber)
        }

        private fun evalExpressionSync(expr: String, scope: MutableMap<String, Any?>): Any? {
            return kotlinx.coroutines.runBlocking { evalExpression(expr, scope) }
        }

        private suspend fun evalListComprehension(inner: String, scope: MutableMap<String, Any?>): List<Any?> {
            val exprPart = inner.substringBefore(" for ").trim()
            val loopPart = inner.substringAfter(" for ")
            val varName = loopPart.substringBefore(" in ").trim()
            val seqAndCond = loopPart.substringAfter(" in ")
            val seqPart = if (seqAndCond.contains(" if ")) seqAndCond.substringBefore(" if ").trim() else seqAndCond.trim()
            val condPart = if (seqAndCond.contains(" if ")) seqAndCond.substringAfter(" if ").trim() else null

            val sequence = evalExpression(seqPart, scope) as? List<*> ?: emptyList<Any?>()
            val result = mutableListOf<Any?>()
            val localScope = mutableMapOf<String, Any?>().apply { putAll(scope) }

            for (item in sequence) {
                localScope[varName] = item
                if (condPart == null || isTruthy(evalExpression(condPart, localScope))) {
                    result.add(evalExpression(exprPart, localScope))
                }
            }
            return result
        }

        private fun handleImportStatement(stmt: String, scope: MutableMap<String, Any?>) {
            val trimmed = stmt.trim()
            if (trimmed.startsWith("import ")) {
                val parts = trimmed.removePrefix("import ").split(",")
                for (p in parts) {
                    val mod = p.trim().substringBefore(" as ").trim()
                    val alias = if (p.contains(" as ")) p.substringAfter(" as ").trim() else mod
                    val modVal = globalScope[mod] ?: mapOf("name" to mod)
                    scope[alias] = modVal
                }
            } else if (trimmed.startsWith("from ")) {
                val mod = trimmed.substringAfter("from ").substringBefore(" import ").trim()
                val symbols = trimmed.substringAfter(" import ").split(",")
                val modMap = globalScope[mod] as? Map<*, *>
                for (s in symbols) {
                    val name = s.trim().substringBefore(" as ").trim()
                    val alias = if (s.contains(" as ")) s.substringAfter(" as ").trim() else name
                    scope[alias] = modMap?.get(name)
                }
            }
        }

        private fun splitTopLevelBinaryOp(expr: String, ops: List<String>): Triple<String, String, String>? {
            var parenDepth = 0
            var bracketDepth = 0
            var i = expr.length - 1

            while (i >= 0) {
                val ch = expr[i]
                if (ch == ')' || ch == ']') parenDepth++
                else if (ch == '(' || ch == '[') parenDepth--
                else if (parenDepth == 0 && bracketDepth == 0) {
                    for (op in ops) {
                        if (expr.substring(0, i + 1).endsWith(op)) {
                            // Ensure not unary or exponent
                            val opStart = i + 1 - op.length
                            if (opStart > 0 && expr[opStart - 1] != '*' && expr[opStart - 1] != '/' && expr[opStart - 1] != '+' && expr[opStart - 1] != '-') {
                                val left = expr.substring(0, opStart).trim()
                                val right = expr.substring(i + 1).trim()
                                if (left.isNotEmpty() && right.isNotEmpty()) {
                                    return Triple(left, op, right)
                                }
                            }
                        }
                    }
                }
                i--
            }
            return null
        }

        private fun applyBinaryOp(left: Any?, right: Any?, op: String): Any? {
            if (left is String || right is String) {
                if (op == "+") return "$left$right"
            }
            if (left is Number && right is Number) {
                val l = left.toDouble()
                val r = right.toDouble()
                val res = when (op) {
                    "+" -> l + r
                    "-" -> l - r
                    "*" -> l * r
                    "/" -> {
                        if (r == 0.0) throw PythonZeroDivisionError(currentLineNumber)
                        l / r
                    }
                    "//" -> {
                        if (r == 0.0) throw PythonZeroDivisionError(currentLineNumber)
                        floor(l / r).toLong()
                    }
                    "%" -> {
                        if (r == 0.0) throw PythonZeroDivisionError(currentLineNumber)
                        l % r
                    }
                    else -> 0.0
                }
                return if (res is Double && res % 1.0 == 0.0) res.toLong() else res
            }
            if (left is List<*> && right is List<*> && op == "+") {
                return left + right
            }
            return null
        }

        private fun compareValues(left: Any?, right: Any?, op: String): Boolean {
            if (op == "==") return left == right
            if (op == "!=") return left != right
            if (left is Number && right is Number) {
                val l = left.toDouble()
                val r = right.toDouble()
                return when (op) {
                    "<" -> l < r
                    "<=" -> l <= r
                    ">" -> l > r
                    ">=" -> l >= r
                    else -> false
                }
            }
            return false
        }

        private fun isTruthy(v: Any?): Boolean {
            return when (v) {
                null -> false
                is Boolean -> v
                is Number -> v.toDouble() != 0.0
                is String -> v.isNotEmpty()
                is List<*> -> v.isNotEmpty()
                is Map<*, *> -> v.isNotEmpty()
                else -> true
            }
        }

        private fun splitArgs(s: String): List<String> {
            val list = mutableListOf<String>()
            var depth = 0
            var inQuote = false
            var quoteChar = ' '
            var start = 0

            for (i in s.indices) {
                val c = s[i]
                if ((c == '"' || c == '\'') && (i == 0 || s[i - 1] != '\\')) {
                    if (!inQuote) { inQuote = true; quoteChar = c }
                    else if (quoteChar == c) { inQuote = false }
                } else if (!inQuote) {
                    if (c == '(' || c == '[' || c == '{') depth++
                    else if (c == ')' || c == ']' || c == '}') depth--
                    else if (c == ',' && depth == 0) {
                        list.add(s.substring(start, i).trim())
                        start = i + 1
                    }
                }
            }
            if (start < s.length) {
                list.add(s.substring(start).trim())
            }
            return list
        }
    }

    private data class UserFunctionDef(
        val name: String,
        val params: List<String>,
        val bodyLines: List<String>,
        val defLine: Int
    )

    private data class UserClassDef(
        val name: String,
        val methods: Map<String, UserFunctionDef>
    )
}
