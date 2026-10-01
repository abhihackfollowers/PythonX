package com.example

import com.example.data.PipRepository
import com.example.engine.PythonIntelliSenseEngine
import com.example.engine.PythonRuntime
import com.example.engine.PythonSyntaxLinter
import com.example.engine.ScientificSolver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ExampleUnitTest {

    @Test
    fun testPythonSyntaxLinter_missingColon() {
        val badCode = "def greet(name)\n    print(name)"
        val issues = PythonSyntaxLinter.lint(badCode)
        assertTrue("Should detect missing colon in def statement", issues.any { it.message.contains("Expected ':'") })
    }

    @Test
    fun testPythonSyntaxLinter_invalidOperators() {
        val badCode = "x = 5\nif x === 5:\n    print(x)"
        val issues = PythonSyntaxLinter.lint(badCode)
        assertTrue("Should detect invalid === operator", issues.any { it.message.contains("===") })
    }

    @Test
    fun testPythonSyntaxLinter_unclosedBracket() {
        val badCode = "items = [1, 2, 3"
        val issues = PythonSyntaxLinter.lint(badCode)
        assertTrue("Should detect unclosed bracket", issues.any { it.message.contains("never closed") })
    }

    @Test
    fun testPythonSyntaxLinter_autoFixAll() {
        val brokenCode = """
            def calculate_square(x)
                if x === null:
                    return 0
                return x * x
        """.trimIndent()

        val (fixedCode, fixesCount) = PythonSyntaxLinter.autoFixAll(brokenCode)
        assertTrue("Should fix at least 2 issues", fixesCount >= 2)
        assertTrue("Colon should be added to def", fixedCode.contains("def calculate_square(x):"))
        assertTrue("=== null should be replaced with == None", fixedCode.contains("== None"))
    }

    @Test
    fun testScientificSolver_linearSystem() {
        // 2x + y = 5
        // x + 3y = 5
        // Solution: x = 2, y = 1
        val A = ScientificSolver.Matrix(listOf(listOf(2.0, 1.0), listOf(1.0, 3.0)))
        val b = listOf(5.0, 5.0)
        val x = ScientificSolver.solveLinearSystem(A, b)
        assertEquals(2.0, x[0], 1e-4)
        assertEquals(1.0, x[1], 1e-4)
        assertEquals(5.0, A.determinant(), 1e-4)
    }

    @Test
    fun testScientificSolver_integration() {
        // \int_0^3 x^2 dx = 9.0
        val res = ScientificSolver.numericalIntegrate({ x -> x * x }, 0.0, 3.0)
        assertEquals(9.0, res, 1e-2)
    }

    @Test
    fun testScientificSolver_equationRoots() {
        // x^2 - 16 = 0 -> roots -4, 4
        val roots = ScientificSolver.solveEquation("x**2 - 16 = 0")
        assertEquals(2, roots.size)
        assertTrue(roots.any { abs(it - 4.0) < 1e-3 })
        assertTrue(roots.any { abs(it - (-4.0)) < 1e-3 })
    }

    @Test
    fun testScientificSolver_pymtosTopology() {
        val solver = ScientificSolver.PyMotosSolver(20, 10, 0.5)
        val result = solver.runOptimization(5)
        assertEquals("Converged (Karush-Kuhn-Tucker optimal)", result["status"])
        assertTrue((result["final_compliance"] as Double) < 100.0)
    }

    @Test
    fun testPythonRuntime_executionWithNumPy() = runBlocking {
        val runtime = PythonRuntime()
        val code = """
            import numpy as np
            arr = np.array([[1.0, 2.0], [3.0, 4.0]])
            det = np.linalg.det(arr)
            print("DETERMINANT:", det)
        """.trimIndent()

        runtime.execute(code) { "input" }
        assertFalse("Runtime should complete cleanly", runtime.isCurrentlyRunning())
    }

    @Test
    fun testPythonRuntime_termuxTraceback() = runBlocking {
        val runtime = PythonRuntime()
        val brokenCode = """
            def broken_fn():
                return 10 / 0
            
            broken_fn()
        """.trimIndent()

        val events = mutableListOf<String>()
        val job = launch {
            runtime.consoleEvents.collect {
                events.add(it.text)
            }
        }

        runtime.execute(brokenCode) { "input" }
        job.cancel()

        assertTrue("Should output Traceback", events.any { it.contains("Traceback (most recent call last):") })
        assertTrue("Should identify ZeroDivisionError", events.any { it.contains("ZeroDivisionError: division by zero") })
    }

    @Test
    fun testTerminalOutputHighlighter_tracebackParsing() {
        val sampleTraceback = """
            Traceback (most recent call last):
              File "main.py", line 14, in <module>
                res = calculate_roots(a, b, c)
              File "main.py", line 6, in calculate_roots
                return 10 / 0
            ZeroDivisionError: division by zero
        """.trimIndent()

        val info = com.example.engine.TerminalOutputHighlighter.parseTracebackInfo(sampleTraceback)
        assertNotNull("Should detect traceback info", info)
        assertEquals("main.py", info!!.fileName)
        assertEquals(6, info.lineNumber)
        assertEquals("calculate_roots", info.functionName)
        assertEquals("ZeroDivisionError", info.errorType)
        assertEquals("division by zero", info.errorMessage)

        val event = com.example.model.ConsoleEvent(com.example.model.ConsoleStreamType.STDERR, sampleTraceback)
        val highlighted = com.example.engine.TerminalOutputHighlighter.highlightTerminalOutput(event)
        assertTrue("Highlighted text should contain error message", highlighted.text.contains("division by zero"))
        assertTrue("Span styles should be present for formatting", highlighted.spanStyles.isNotEmpty())
    }
}
