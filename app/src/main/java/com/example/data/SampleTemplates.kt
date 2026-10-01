package com.example.data

import com.example.model.WorkspaceFile

object SampleTemplates {
    val MAIN_PY = """
# Welcome to PythonX AI - Next-Gen Desktop Python IDE
# Real-time IntelliSense | Live Syntax Linting | Visual Pip | AI Copilot

import math
import time

def greet(developer: str) -> None:
    print(f"🚀 Hello, {developer}!")
    print("Welcome to PythonX AI on Android.")
    print("=" * 44)

def calculate_primes(limit: int) -> list[int]:
    primes = []
    for num in range(2, limit + 1):
        is_prime = True
        for i in range(2, int(math.isqrt(num)) + 1):
            if num % i == 0:
                is_prime = False
                break
        if is_prime:
            primes.append(num)
    return primes

# Run main demonstration
greet("Pythonista")
print("Computing prime numbers up to 50...")
prime_list = calculate_primes(50)
print(f"Discovered {len(prime_list)} primes: {prime_list}")

# Interactive greeting
user_name = input("Enter your name: ")
print(f"Nice to code with you, {user_name}! Tap ▶ Run anytime to test.")
""".trimIndent()

    val FIBONACCI_PY = """
# Performance Benchmark: Recursive vs Iterative Fibonacci
import time

def fib_recursive(n: int) -> int:
    if n <= 1:
        return n
    return fib_recursive(n - 1) + fib_recursive(n - 2)

def fib_iterative(n: int) -> int:
    if n <= 1:
        return n
    a, b = 0, 1
    for _ in range(2, n + 1):
        a, b = b, a + b
    return b

target = 25
print(f"=== Benchmarking Fibonacci({target}) ===")

# Benchmark Recursive
t0 = time.time()
res_rec = fib_recursive(target)
t1 = time.time()
print(f"Recursive: result = {res_rec}, elapsed = {((t1 - t0) * 1000):.2f} ms")

# Benchmark Iterative
t0 = time.time()
res_it = fib_iterative(target)
t1 = time.time()
print(f"Iterative: result = {res_it}, elapsed = {((t1 - t0) * 1000):.3f} ms")

print("✨ Iterative is drastically faster!")
""".trimIndent()

    val DATA_ANALYTICS_PY = """
# Data Processing & Summary Statistics
import math

dataset = [12.5, 45.2, 33.8, 89.1, 72.4, 61.0, 94.8, 55.3, 41.9, 83.2]

def summarize(data: list[float]) -> dict:
    n = len(data)
    total = sum(data)
    mean = total / n
    variance = sum((x - mean) ** 2 for x in data) / n
    std_dev = math.sqrt(variance)
    
    sorted_data = sorted(data)
    median = sorted_data[n // 2] if n % 2 != 0 else (sorted_data[n // 2 - 1] + sorted_data[n // 2]) / 2
    
    return {
        "count": n,
        "sum": round(total, 2),
        "mean": round(mean, 2),
        "median": round(median, 2),
        "std_dev": round(std_dev, 2),
        "min": min(data),
        "max": max(data)
    }

stats = summarize(dataset)
print("📊 Dataset Analysis Report:")
print("-" * 30)
for k, v in stats.items():
    print(f" • {k.ljust(10)}: {v}")
""".trimIndent()

    val SMART_CALCULATOR_PY = """
# Interactive Command Line Calculator
print("=== PythonX Interactive Calculator ===")
print("Available operations: add (+), sub (-), mul (*), div (/)")

num1_str = input("Enter first number: ")
op = input("Enter operator (+, -, *, /): ")
num2_str = input("Enter second number: ")

try:
    a = float(num1_str)
    b = float(num2_str)
    
    if op == "+":
        result = a + b
    elif op == "-":
        result = a - b
    elif op == "*":
        result = a * b
    elif op == "/":
        if b == 0:
            raise ZeroDivisionError("Cannot divide by zero")
        result = a / b
    else:
        result = "Unknown operator"

    print(f"Result: {a} {op} {b} = {result}")
except Exception as e:
    print(f"Error during calculation: {e}")
""".trimIndent()

    val ALGORITHM_PY = """
# Sorting Algorithms & Binary Search
def quicksort(arr):
    if len(arr) <= 1:
        return arr
    pivot = arr[len(arr) // 2]
    left = [x for x in arr if x < pivot]
    middle = [x for x in arr if x == pivot]
    right = [x for x in arr if x > pivot]
    return quicksort(left) + middle + quicksort(right)

def binary_search(arr, target):
    low = 0
    high = len(arr) - 1
    while low <= high:
        mid = (low + high) // 2
        if arr[mid] == target:
            return mid
        elif arr[mid] < target:
            low = mid + 1
        else:
            high = mid - 1
    return -1

raw = [64, 34, 25, 12, 22, 11, 90, 42, 88, 3]
print(f"Original list: {raw}")
sorted_list = quicksort(raw)
print(f"Sorted list:   {sorted_list}")

query = 42
pos = binary_search(sorted_list, query)
print(f"Binary search for {query}: found at index {pos}")
""".trimIndent()

    val SCIENTIFIC_SOLVER_PY = """
# === PyMTOS & Scientific Solver Suite ===
# Linear Algebra, Root Finding, Numerical Integration & Symbolic Equations
import numpy as np
import scipy
from sympy import solve

print("=== 1. Linear System Solver (A x = b) ===")
# Solving:
# 2x + y = 5
# x + 3y = 5
A = np.array([[2.0, 1.0], [1.0, 3.0]])
b = [5.0, 5.0]
x_sol = np.linalg.solve(A, b)
print(f"Matrix A:\n{A}")
print(f"Solution vector x = {x_sol}")
print(f"Determinant det(A) = {np.linalg.det(A)}")

print("\n=== 2. Numerical Integration (Definite Integral) ===")
# \int_0^3 x^2 dx = [x^3 / 3]_0^3 = 27 / 3 = 9.0
quad_fn = lambda x: x[0] ** 2
res, err = scipy.integrate.quad(quad_fn, 0.0, 3.0)
print(f"Integral of x^2 from 0 to 3: {res:.4f} (error estimate: {err})")

print("\n=== 3. Symbolic Equation Solver ===")
# Solving x^2 - 16 = 0
roots = solve("x**2 - 16 = 0")
print(f"Exact roots of (x^2 - 16 = 0): {roots}")

roots_quad = solve("x**2 - 5*x + 6 = 0")
print(f"Exact roots of (x^2 - 5x + 6 = 0): {roots_quad}")
""".trimIndent()

    val PYMTOS_SOLVER_PY = """
# === PyMTOS / pyMOTO Topology Optimization Solver ===
# Modular 2D Structural Mechanics Compliance Minimization
import pymtos

print("Initializing PyMTOS 2D Structural Mesh Domain...")
# 30x10 mesh with 50% target volume fraction
solver = pymtos.Solver(30, 10, 0.5)

print(f"Engine: {pymtos.version}")
print("Running compliance minimization optimization...")
result = pymtos.optimize(solver, 10)

print(f"Status: {result['status']}")
print(f"Final Compliance: {result['final_compliance']:.4f}")
print("Compliance convergence history:")
for idx, c in enumerate(result['history']):
    print(f"  Iteration {idx + 1:02d}: compliance = {c:.3f}")

print("\nTopology optimization converged successfully!")
""".trimIndent()

    fun getInitialWorkspace(): List<WorkspaceFile> = listOf(
        WorkspaceFile(
            name = "main.py",
            content = MAIN_PY,
            isMainEntry = true
        ),
        WorkspaceFile(
            name = "scientific_solver.py",
            content = SCIENTIFIC_SOLVER_PY
        ),
        WorkspaceFile(
            name = "pymtos_topology.py",
            content = PYMTOS_SOLVER_PY
        ),
        WorkspaceFile(
            name = "fibonacci_benchmark.py",
            content = FIBONACCI_PY
        ),
        WorkspaceFile(
            name = "data_analytics.py",
            content = DATA_ANALYTICS_PY
        ),
        WorkspaceFile(
            name = "smart_calculator.py",
            content = SMART_CALCULATOR_PY
        ),
        WorkspaceFile(
            name = "algorithm_visualizer.py",
            content = ALGORITHM_PY
        )
    )
}
