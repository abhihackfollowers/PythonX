package com.example.data

import com.example.model.PipPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class PipRepository(private val pipDao: PipDao? = null) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val _packages = MutableStateFlow<List<PipPackage>>(defaultPackages())
    val packages: StateFlow<List<PipPackage>> = _packages.asStateFlow()

    suspend fun initDatabaseSync() = withContext(Dispatchers.IO) {
        pipDao?.let { dao ->
            if (dao.countPackages() == 0) {
                dao.insertPackages(defaultPackages())
            }
        }
    }

    suspend fun installPackage(
        packageName: String,
        onLog: ((String) -> Unit)? = null,
        onProgress: (Float, String) -> Unit
    ): PipPackage? = withContext(Dispatchers.IO) {
        val trimmed = packageName.trim().lowercase()
        updatePackageStatus(trimmed, isInstalling = true, progress = 0.1f)
        onLog?.invoke("Collecting $trimmed...")
        onProgress(0.1f, "Connecting to PyPI index: https://pypi.org/pypi/$trimmed/json")
        delay(250)

        // Fetch metadata from PyPI
        var packageData: PipPackage? = _packages.value.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
        try {
            val url = "https://pypi.org/pypi/$trimmed/json"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val json = JSONObject(body)
                        val info = json.getJSONObject("info")
                        val realName = info.optString("name", trimmed)
                        val version = info.optString("version", "1.0.0")
                        val summary = info.optString("summary", "Python library from PyPI")
                        val author = info.optString("author", "PyPI Maintainer")
                        val license = info.optString("license", "Open Source")
                        val homePage = info.optString("project_url", "https://pypi.org/project/$trimmed")

                        packageData = PipPackage(
                            name = realName,
                            summary = summary,
                            latestVersion = version,
                            installedVersion = version,
                            author = author,
                            license = license,
                            homePage = homePage,
                            category = "PyPI",
                            isInstalled = true,
                            isInstalling = false,
                            installProgress = 1f,
                            sampleUsage = "import $realName\nprint('Loaded $realName version:', getattr($realName, '__version__', '$version'))"
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Offline fallback
        }

        val finalPkg = packageData?.copy(
            isInstalled = true,
            installedVersion = packageData?.latestVersion ?: "1.0.0",
            isInstalling = false,
            installProgress = 1f
        ) ?: PipPackage(
            name = trimmed,
            summary = "Python package $trimmed installed via PyPI",
            latestVersion = "1.0.0",
            installedVersion = "1.0.0",
            category = "Installed",
            isInstalled = true,
            isInstalling = false,
            installProgress = 1f,
            sampleUsage = "import $trimmed\nprint('$trimmed ready!')"
        )

        onLog?.invoke("  Downloading $trimmed-${finalPkg.latestVersion}-py3-none-any.whl (42 kB)")
        onProgress(0.5f, "Downloading wheel archive for $trimmed...")
        delay(350)

        onLog?.invoke("Installing collected packages: ${finalPkg.name}")
        onProgress(0.85f, "Configuring site-packages runtime bindings...")
        delay(300)

        // Save to Room and Memory
        pipDao?.insertPackage(finalPkg)
        _packages.value = listOf(finalPkg) + _packages.value.filterNot { it.name.equals(finalPkg.name, ignoreCase = true) }

        onLog?.invoke("Successfully installed ${finalPkg.name}-${finalPkg.latestVersion}")
        onProgress(1f, "Successfully installed ${finalPkg.name}!")
        finalPkg
    }

    suspend fun uninstallPackage(packageName: String, onLog: ((String) -> Unit)? = null) = withContext(Dispatchers.IO) {
        val trimmed = packageName.trim().lowercase()
        onLog?.invoke("Found existing installation: $trimmed")
        delay(200)
        onLog?.invoke("Uninstalling $trimmed...")
        delay(250)

        val updated = _packages.value.map { pkg ->
            if (pkg.name.equals(trimmed, ignoreCase = true)) {
                val mod = pkg.copy(isInstalled = false, installedVersion = null, isInstalling = false, installProgress = 0f)
                pipDao?.insertPackage(mod)
                mod
            } else pkg
        }
        _packages.value = updated
        onLog?.invoke("Successfully uninstalled $trimmed")
    }

    suspend fun searchPyPI(query: String): PipPackage? = withContext(Dispatchers.IO) {
        val trimmed = query.trim().lowercase()
        val existing = _packages.value.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
        if (existing != null) return@withContext existing

        try {
            val url = "https://pypi.org/pypi/$trimmed/json"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@use null
                    val json = JSONObject(body)
                    val info = json.getJSONObject("info")
                    val name = info.optString("name", trimmed)
                    val version = info.optString("version", "1.0.0")
                    val summary = info.optString("summary", "Python library available on PyPI.")
                    val author = info.optString("author", "PyPI Contributor")
                    val license = info.optString("license", "Open Source")

                    val newPkg = PipPackage(
                        name = name,
                        summary = summary,
                        latestVersion = version,
                        author = author,
                        license = license,
                        category = "PyPI",
                        isInstalled = false,
                        sampleUsage = "import $name\nprint('Loaded $name version:', getattr($name, '__version__', 'unknown'))"
                    )

                    pipDao?.insertPackage(newPkg)
                    _packages.value = listOf(newPkg) + _packages.value
                    return@withContext newPkg
                }
            }
        } catch (_: Exception) {
            // Offline or network error
        }
        null
    }

    private fun updatePackageStatus(name: String, isInstalling: Boolean, progress: Float) {
        _packages.value = _packages.value.map { pkg ->
            if (pkg.name.equals(name, ignoreCase = true)) {
                pkg.copy(isInstalling = isInstalling, installProgress = progress)
            } else pkg
        }
    }

    companion object {
        fun defaultPackages(): List<PipPackage> = listOf(
            PipPackage(
                name = "numpy",
                summary = "Fundamental package for scientific computing with Python arrays and matrices.",
                latestVersion = "1.26.4",
                installedVersion = "1.26.4",
                author = "Travis Oliphant et al.",
                license = "BSD-3-Clause",
                category = "Data Science",
                isInstalled = true,
                sampleUsage = "import numpy as np\n\narr = np.array([1, 2, 3, 4, 5])\nprint('Mean:', np.mean(arr))\nprint('Dot product:', np.dot(arr, arr))"
            ),
            PipPackage(
                name = "requests",
                summary = "Python HTTP for Humans. Elegant and simple HTTP library for REST APIs.",
                latestVersion = "2.32.3",
                installedVersion = "2.32.3",
                author = "Kenneth Reitz",
                license = "Apache 2.0",
                category = "Networking",
                isInstalled = true,
                sampleUsage = "import requests\n\nr = requests.get('https://api.github.com')\nprint('Status code:', r.status_code)\nprint('JSON:', r.json())"
            ),
            PipPackage(
                name = "pandas",
                summary = "Powerful data structures for data analysis, time series, and statistics.",
                latestVersion = "2.2.2",
                author = "Wes McKinney",
                license = "BSD-3-Clause",
                category = "Data Science",
                isInstalled = false,
                sampleUsage = "import pandas as pd\n\ndf = pd.DataFrame({'Name': ['Alice', 'Bob'], 'Score': [95, 88]})\nprint(df.describe())"
            ),
            PipPackage(
                name = "matplotlib",
                summary = "Python plotting package for publication-quality figures, charts, and visualizations.",
                latestVersion = "3.9.0",
                author = "John D. Hunter",
                license = "PSF",
                category = "Visualization",
                isInstalled = false,
                sampleUsage = "import matplotlib.pyplot as plt\n\nx = [1, 2, 3, 4]\ny = [1, 4, 9, 16]\nplt.plot(x, y)\nplt.title('Sample Plot')\nplt.show()"
            ),
            PipPackage(
                name = "scipy",
                summary = "Fundamental algorithms for scientific computing in Python (optimization, linear algebra).",
                latestVersion = "1.13.1",
                author = "SciPy Developers",
                license = "BSD-3-Clause",
                category = "Scientific",
                isInstalled = false,
                sampleUsage = "from scipy import stats\n\nres = stats.describe([1, 2, 3, 4, 5])\nprint(res)"
            ),
            PipPackage(
                name = "flask",
                summary = "A lightweight WSGI web application framework designed to make getting started quick and easy.",
                latestVersion = "3.0.3",
                author = "Armin Ronacher",
                license = "BSD-3-Clause",
                category = "Web",
                isInstalled = false,
                sampleUsage = "from flask import Flask\napp = Flask(__name__)\n\n@app.route('/')\ndef home():\n    return 'Hello World'\n\n# app.run()"
            ),
            PipPackage(
                name = "beautifulsoup4",
                summary = "Screen-scraping library to extract data out of HTML and XML files.",
                latestVersion = "4.12.3",
                author = "Leonard Richardson",
                license = "MIT",
                category = "Web",
                isInstalled = false,
                sampleUsage = "from bs4 import BeautifulSoup\nsoup = BeautifulSoup('<h1>Title</h1>', 'html.parser')\nprint(soup.h1.text)"
            ),
            PipPackage(
                name = "sympy",
                summary = "Computer algebra system for symbolic mathematics (calculus, equations, matrices).",
                latestVersion = "1.12.1",
                author = "SymPy Development Team",
                license = "BSD-3-Clause",
                category = "Scientific",
                isInstalled = false,
                sampleUsage = "import sympy as sp\nx = sp.Symbol('x')\nprint(sp.diff(sp.sin(x)**2, x))"
            ),
            PipPackage(
                name = "rich",
                summary = "Rich text and beautiful terminal formatting in the terminal.",
                latestVersion = "13.7.1",
                author = "Will McGugan",
                license = "MIT",
                category = "Utilities",
                isInstalled = false,
                sampleUsage = "from rich import print\nprint('[bold green]Hello[/bold green] [magenta]World[/magenta]!')"
            ),
            PipPackage(
                name = "fastapi",
                summary = "FastAPI framework, high performance, easy to learn, fast to code, ready for production.",
                latestVersion = "0.111.0",
                author = "Sebastián Ramírez",
                license = "MIT",
                category = "Web",
                isInstalled = false,
                sampleUsage = "from fastapi import FastAPI\napp = FastAPI()\n@app.get('/')\ndef read_root(): return {'Hello': 'World'}"
            ),
            PipPackage(
                name = "pymoto",
                summary = "PyMTOS / pyMOTO: Modular Topology Optimization and Scientific Mechanics Solver.",
                latestVersion = "1.2.0",
                installedVersion = "1.2.0",
                author = "A. A. T. M. Delissen",
                license = "GPL-3.0",
                category = "Scientific",
                isInstalled = true,
                sampleUsage = "import pymtos\nsolver = pymtos.Solver(30, 10, 0.5)\nres = pymtos.optimize(solver, 10)\nprint('Optimized:', res['final_compliance'])"
            )
        )
    }
}
