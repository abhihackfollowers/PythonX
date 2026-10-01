package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiCopilotService
import com.example.ai.CopilotResponse
import com.example.ai.CopilotTask
import com.example.data.AppDatabase
import com.example.data.PipRepository
import com.example.data.WorkspaceRepository
import com.example.engine.PythonHighlighter
import com.example.engine.PythonIntelliSenseEngine
import com.example.engine.PythonRuntime
import com.example.engine.PythonSyntaxLinter
import com.example.model.ConsoleEvent
import com.example.model.ConsoleStreamType
import com.example.model.IntelliSenseItem
import com.example.model.LintIssue
import com.example.model.PipPackage
import com.example.model.WorkspaceFile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class IdeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val workspaceRepo = WorkspaceRepository(database.workspaceDao())
    val pipRepo = PipRepository(database.pipDao())
    private val aiService = AiCopilotService()
    private val runtime = PythonRuntime()
    val apiKeyManager = com.example.data.ApiKeyManager(application)

    private val _geminiApiKey = MutableStateFlow(apiKeyManager.getGeminiApiKey())
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun clearToast() {
        _toastMessage.value = null
    }

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun saveGeminiApiKey(key: String) {
        apiKeyManager.setGeminiApiKey(key)
        _geminiApiKey.value = key
        _toastMessage.value = "Gemini API Key saved securely!"
    }

    fun clearGeminiApiKey() {
        apiKeyManager.clearGeminiApiKey()
        _geminiApiKey.value = ""
        _toastMessage.value = "Custom API Key removed."
    }

    // Workspace files flow
    val workspaceFiles: StateFlow<List<WorkspaceFile>> = workspaceRepo.allFiles
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Active File & Editor state
    private val _activeFile = MutableStateFlow<WorkspaceFile?>(null)
    val activeFile: StateFlow<WorkspaceFile?> = _activeFile.asStateFlow()

    private val _codeText = MutableStateFlow("")
    val codeText: StateFlow<String> = _codeText.asStateFlow()

    private val _cursorPosition = MutableStateFlow(0)
    val cursorPosition: StateFlow<Int> = _cursorPosition.asStateFlow()

    // Live IntelliSense suggestions
    private val _suggestions = MutableStateFlow<List<IntelliSenseItem>>(emptyList())
    val suggestions: StateFlow<List<IntelliSenseItem>> = _suggestions.asStateFlow()

    // Live Linting & Syntax Issues
    private val _lintIssues = MutableStateFlow<List<LintIssue>>(emptyList())
    val lintIssues: StateFlow<List<LintIssue>> = _lintIssues.asStateFlow()

    // Console state
    private val _consoleEvents = MutableStateFlow<List<ConsoleEvent>>(emptyList())
    val consoleEvents: StateFlow<List<ConsoleEvent>> = _consoleEvents.asStateFlow()

    private val _isExecutionRunning = MutableStateFlow(false)
    val isExecutionRunning: StateFlow<Boolean> = _isExecutionRunning.asStateFlow()

    private val _isConsoleExpanded = MutableStateFlow(false)
    val isConsoleExpanded: StateFlow<Boolean> = _isConsoleExpanded.asStateFlow()

    private val _interactivePrompt = MutableStateFlow<String?>(null)
    val interactivePrompt: StateFlow<String?> = _interactivePrompt.asStateFlow()

    private var inputContinuation: ((String) -> Unit)? = null

    // Pip Package Manager State
    val pipPackages: StateFlow<List<PipPackage>> = pipRepo.packages
    private val _isPipSheetOpen = MutableStateFlow(false)
    val isPipSheetOpen: StateFlow<Boolean> = _isPipSheetOpen.asStateFlow()
    private val _pipSearchQuery = MutableStateFlow("")
    val pipSearchQuery: StateFlow<String> = _pipSearchQuery.asStateFlow()
    private val _pipStatusMessage = MutableStateFlow<String?>(null)
    val pipStatusMessage: StateFlow<String?> = _pipStatusMessage.asStateFlow()

    // AI Copilot state
    private val _isCopilotOpen = MutableStateFlow(false)
    val isCopilotOpen: StateFlow<Boolean> = _isCopilotOpen.asStateFlow()
    private val _isCopilotLoading = MutableStateFlow(false)
    val isCopilotLoading: StateFlow<Boolean> = _isCopilotLoading.asStateFlow()
    private val _copilotResponse = MutableStateFlow<CopilotResponse?>(null)
    val copilotResponse: StateFlow<CopilotResponse?> = _copilotResponse.asStateFlow()
    private val _copilotPromptInput = MutableStateFlow("")
    val copilotPromptInput: StateFlow<String> = _copilotPromptInput.asStateFlow()

    // Workspace Drawer state
    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen: StateFlow<Boolean> = _isDrawerOpen.asStateFlow()

    private var lintJob: Job? = null
    private var saveJob: Job? = null

    init {
        // Collect console streams from runtime
        viewModelScope.launch {
            runtime.consoleEvents.collect { event ->
                _consoleEvents.value = _consoleEvents.value + event
            }
        }

        // Initialize active file once database is ready
        viewModelScope.launch {
            workspaceRepo.ensureDefaultTemplates()
            workspaceFiles.collect { files ->
                if (_activeFile.value == null && files.isNotEmpty()) {
                    val mainFile = files.firstOrNull { it.isMainEntry } ?: files.first()
                    selectFile(mainFile)
                }
            }
        }
    }

    fun selectFile(file: WorkspaceFile) {
        _activeFile.value = file
        _codeText.value = file.content
        _cursorPosition.value = 0
        triggerLinting(file.content)
        _suggestions.value = emptyList()
    }

    fun updateCode(newCode: String, newCursor: Int = _cursorPosition.value) {
        _codeText.value = newCode
        _cursorPosition.value = newCursor.coerceIn(0, newCode.length)

        // 1. Update live IntelliSense suggestions
        _suggestions.value = PythonIntelliSenseEngine.getSuggestions(newCode, _cursorPosition.value)

        // 2. Debounced background static linting
        triggerLinting(newCode)

        // 3. Auto-save file to database with debounce
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(800)
            _activeFile.value?.let { current ->
                workspaceRepo.saveFile(current.copy(content = newCode))
            }
        }
    }

    private fun triggerLinting(code: String) {
        lintJob?.cancel()
        lintJob = viewModelScope.launch {
            delay(300)
            _lintIssues.value = PythonSyntaxLinter.lint(code)
        }
    }

    fun applySuggestion(item: IntelliSenseItem) {
        val code = _codeText.value
        val cursor = _cursorPosition.value
        val textBefore = code.substring(0, cursor)
        val wordMatch = Regex("""[a-zA-Z0-9_]+$""").find(textBefore)

        val prefixLen = wordMatch?.value?.length ?: 0
        val replaceStart = cursor - prefixLen
        val newText = StringBuilder(code)
            .replace(replaceStart, cursor, item.insertText)
            .toString()

        val newCursor = replaceStart + item.insertText.length + item.cursorOffset
        _suggestions.value = emptyList()
        updateCode(newText, newCursor)
    }

    fun jumpToLine(lineNumber: Int) {
        val lines = _codeText.value.split("\n")
        val targetLine = (lineNumber - 1).coerceIn(0, (lines.size - 1).coerceAtLeast(0))
        var offset = 0
        for (i in 0 until targetLine) {
            offset += lines[i].length + 1
        }
        _cursorPosition.value = offset
        _toastMessage.value = "📍 Navigated to line $lineNumber"
    }

    fun insertAccessory(token: String) {
        val code = _codeText.value
        val cursor = _cursorPosition.value

        // Check if token is bracket or quote for auto-close
        if (token.length == 1 && token[0] in listOf('(', '[', '{', '"', '\'')) {
            val res = PythonIntelliSenseEngine.handleAutoClose(code, cursor, token[0])
            if (res != null) {
                updateCode(res.first, res.second)
                return
            }
        }

        // Standard insertion
        val newText = StringBuilder(code).insert(cursor, token).toString()
        val newCursor = cursor + token.length
        updateCode(newText, newCursor)
    }

    fun handleEnterKey() {
        val code = _codeText.value
        val cursor = _cursorPosition.value
        val (newText, newCursor) = PythonIntelliSenseEngine.handleAutoIndent(code, cursor)
        updateCode(newText, newCursor)
    }

    fun applyQuickFix(issue: LintIssue) {
        if (issue.replacement != null) {
            val lines = _codeText.value.split("\n").toMutableList()
            if (issue.line - 1 in lines.indices) {
                lines[issue.line - 1] = issue.replacement
                updateCode(lines.joinToString("\n"))
            }
        }
    }

    // Execution & Runtime Controls
    fun runPythonScript() {
        val code = _codeText.value
        if (code.isBlank()) return

        _isConsoleExpanded.value = true
        _isExecutionRunning.value = true

        viewModelScope.launch {
            runtime.execute(
                code = code,
                onInputRequested = { prompt ->
                    _interactivePrompt.value = prompt
                    suspendCancellableCoroutine<String> { continuation ->
                        inputContinuation = { userInput ->
                            continuation.resume(userInput)
                        }
                    }
                }
            )
            _isExecutionRunning.value = false
            _interactivePrompt.value = null
        }
    }

    fun submitConsoleInput(input: String) {
        val prompt = _interactivePrompt.value ?: ""
        _consoleEvents.value = _consoleEvents.value + ConsoleEvent(ConsoleStreamType.INPUT_ECHO, "$prompt$input")
        _interactivePrompt.value = null
        inputContinuation?.invoke(input)
        inputContinuation = null
    }

    fun stopPythonScript() {
        runtime.stopExecution()
        _isExecutionRunning.value = false
        _interactivePrompt.value = null
        inputContinuation?.invoke("")
        inputContinuation = null
    }

    fun autoFixAllSyntax() {
        val (fixedCode, fixesCount) = PythonSyntaxLinter.autoFixAll(_codeText.value)
        if (fixesCount > 0) {
            updateCode(fixedCode)
            _toastMessage.value = "⚡ Real-time auto-fix resolved $fixesCount syntax issue${if (fixesCount > 1) "s" else ""}!"
        } else {
            _toastMessage.value = "Code is already syntax-clean!"
        }
    }

    fun executeTerminalCommand(rawCommand: String) {
        val cmd = rawCommand.trim()
        if (cmd.isEmpty()) return
        _isConsoleExpanded.value = true
        _consoleEvents.value = _consoleEvents.value + ConsoleEvent(ConsoleStreamType.INPUT_ECHO, "$ $cmd")

        viewModelScope.launch {
            if (cmd.startsWith("pip ")) {
                val parts = cmd.split(" ").filter { it.isNotBlank() }
                val subCmd = parts.getOrNull(1)?.lowercase()
                when (subCmd) {
                    "install" -> {
                        val pkgName = parts.getOrNull(2)
                        if (pkgName.isNullOrBlank()) {
                            _consoleEvents.value = _consoleEvents.value + ConsoleEvent(
                                ConsoleStreamType.STDERR,
                                "ERROR: You must give at least one requirement to install (see 'pip --help')"
                            )
                        } else {
                            pipRepo.installPackage(
                                packageName = pkgName,
                                onLog = { log ->
                                    _consoleEvents.value = _consoleEvents.value + ConsoleEvent(ConsoleStreamType.STDOUT, log)
                                },
                                onProgress = { _, _ -> }
                            )
                        }
                    }
                    "uninstall" -> {
                        val pkgName = parts.getOrNull(2)
                        if (pkgName.isNullOrBlank()) {
                            _consoleEvents.value = _consoleEvents.value + ConsoleEvent(
                                ConsoleStreamType.STDERR,
                                "ERROR: You must specify a package to uninstall"
                            )
                        } else {
                            pipRepo.uninstallPackage(pkgName) { log ->
                                _consoleEvents.value = _consoleEvents.value + ConsoleEvent(ConsoleStreamType.STDOUT, log)
                            }
                        }
                    }
                    "list" -> {
                        val pkgs = pipPackages.value.filter { it.isInstalled }
                        _consoleEvents.value = _consoleEvents.value + ConsoleEvent(
                            ConsoleStreamType.STDOUT,
                            "Package            Version\n------------------ --------"
                        )
                        pkgs.forEach { p ->
                            val padded = p.name.padEnd(19)
                            _consoleEvents.value = _consoleEvents.value + ConsoleEvent(
                                ConsoleStreamType.STDOUT,
                                "$padded${p.installedVersion ?: p.latestVersion}"
                            )
                        }
                    }
                    "show" -> {
                        val pkgName = parts.getOrNull(2)
                        val pkg = pipPackages.value.firstOrNull { it.name.equals(pkgName, ignoreCase = true) }
                        if (pkg != null) {
                            _consoleEvents.value = _consoleEvents.value + ConsoleEvent(
                                ConsoleStreamType.STDOUT,
                                "Name: ${pkg.name}\nVersion: ${pkg.installedVersion ?: pkg.latestVersion}\nSummary: ${pkg.summary}\nAuthor: ${pkg.author}\nLicense: ${pkg.license}\nLocation: /data/data/com.aistudio.pythonx/site-packages"
                            )
                        } else {
                            _consoleEvents.value = _consoleEvents.value + ConsoleEvent(
                                ConsoleStreamType.STDERR,
                                "WARNING: Package(s) not found: $pkgName"
                            )
                        }
                    }
                    else -> {
                        _consoleEvents.value = _consoleEvents.value + ConsoleEvent(
                            ConsoleStreamType.STDOUT,
                            "pip 24.0 from /site-packages (python 3.12)\nUsage: pip <command> [options]\nCommands:\n  install       Install packages from PyPI.\n  uninstall     Uninstall packages.\n  list          List installed packages.\n  show          Show information about installed packages."
                        )
                    }
                }
            } else {
                runtime.execute(cmd) { "" }
            }
        }
    }

    fun installAnyPackage(packageName: String) {
        viewModelScope.launch {
            _pipStatusMessage.value = "Fetching $packageName from PyPI..."
            val installed = pipRepo.installPackage(
                packageName = packageName,
                onLog = { msg -> _pipStatusMessage.value = msg },
                onProgress = { _, msg -> _pipStatusMessage.value = msg }
            )
            if (installed != null) {
                _toastMessage.value = "Successfully installed ${installed.name} from PIP!"
            }
        }
    }

    fun clearConsole() {
        _consoleEvents.value = emptyList()
    }

    fun toggleConsole() {
        _isConsoleExpanded.value = !_isConsoleExpanded.value
    }

    // Pip Package Manager Controls
    fun openPipManager() {
        _isPipSheetOpen.value = true
    }

    fun closePipManager() {
        _isPipSheetOpen.value = false
        _pipStatusMessage.value = null
    }

    fun setPipSearchQuery(q: String) {
        _pipSearchQuery.value = q
    }

    fun searchPyPI() {
        val query = _pipSearchQuery.value.trim()
        if (query.isEmpty()) return
        viewModelScope.launch {
            _pipStatusMessage.value = "Searching PyPI index for '$query'..."
            val result = pipRepo.searchPyPI(query)
            if (result != null) {
                _pipStatusMessage.value = "Found ${result.name} (${result.latestVersion})"
            } else {
                _pipStatusMessage.value = "Package '$query' not found or offline."
            }
        }
    }

    fun installPackage(pkg: PipPackage) {
        viewModelScope.launch {
            pipRepo.installPackage(pkg.name) { _, msg ->
                _pipStatusMessage.value = msg
            }
        }
    }

    fun uninstallPackage(pkg: PipPackage) {
        viewModelScope.launch {
            pipRepo.uninstallPackage(pkg.name)
            _pipStatusMessage.value = "Uninstalled ${pkg.name}"
        }
    }

    fun copySnippetToEditor(snippet: String) {
        val current = _codeText.value
        val updated = if (current.isBlank()) snippet else "$current\n\n# --- Imported snippet ---\n$snippet"
        updateCode(updated)
        closePipManager()
    }

    // AI Copilot Controls
    fun openAiCopilot(task: CopilotTask = CopilotTask.EXPLAIN) {
        _isCopilotOpen.value = true
        _copilotResponse.value = null
        executeCopilotTask(task)
    }

    fun closeAiCopilot() {
        _isCopilotOpen.value = false
    }

    fun setCopilotPromptInput(prompt: String) {
        _copilotPromptInput.value = prompt
    }

    fun executeCopilotTask(task: CopilotTask) {
        _isCopilotLoading.value = true
        viewModelScope.launch {
            val response = aiService.executeTask(
                task = task,
                code = _codeText.value,
                userPrompt = _copilotPromptInput.value,
                issues = _lintIssues.value,
                customApiKey = _geminiApiKey.value
            )
            _copilotResponse.value = response
            _isCopilotLoading.value = false
        }
    }

    fun applyCopilotPatch() {
        val patched = _copilotResponse.value?.patchedCode
        if (!patched.isNullOrBlank()) {
            updateCode(patched)
            closeAiCopilot()
        }
    }

    // Workspace File Tree Controls
    fun toggleDrawer() {
        _isDrawerOpen.value = !_isDrawerOpen.value
    }

    fun createNewFile(fileName: String) {
        viewModelScope.launch {
            val id = workspaceRepo.createFile(fileName)
            val newFile = workspaceRepo.getFileById(id)
            if (newFile != null) {
                selectFile(newFile)
            }
        }
    }

    fun deleteFile(file: WorkspaceFile) {
        viewModelScope.launch {
            workspaceRepo.deleteFile(file)
            val remaining = workspaceFiles.value.filter { it.id != file.id }
            if (remaining.isNotEmpty()) {
                selectFile(remaining.first())
            }
        }
    }
}
