package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.CopilotTask
import com.example.ui.components.AccessoryBar
import com.example.ui.components.AiCopilotSheet
import com.example.ui.components.CodeEditor
import com.example.ui.components.ConsoleSheet
import com.example.ui.components.IntelliSensePopup
import com.example.ui.components.PipManagerDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.WorkspaceDrawer
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberError
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberSecondary
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberTertiary
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.viewmodel.IdeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainIdeScreen(
    viewModel: IdeViewModel = viewModel()
) {
    val activeFile by viewModel.activeFile.collectAsState()
    val codeText by viewModel.codeText.collectAsState()
    val cursorPosition by viewModel.cursorPosition.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val lintIssues by viewModel.lintIssues.collectAsState()
    val consoleEvents by viewModel.consoleEvents.collectAsState()
    val isRunning by viewModel.isExecutionRunning.collectAsState()
    val isConsoleExpanded by viewModel.isConsoleExpanded.collectAsState()
    val interactivePrompt by viewModel.interactivePrompt.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val pipPackages by viewModel.pipPackages.collectAsState()
    val isPipOpen by viewModel.isPipSheetOpen.collectAsState()
    val pipSearchQuery by viewModel.pipSearchQuery.collectAsState()
    val pipStatusMessage by viewModel.pipStatusMessage.collectAsState()

    val isCopilotOpen by viewModel.isCopilotOpen.collectAsState()
    val isCopilotLoading by viewModel.isCopilotLoading.collectAsState()
    val copilotResponse by viewModel.copilotResponse.collectAsState()
    val copilotPromptInput by viewModel.copilotPromptInput.collectAsState()

    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()

    val workspaceFiles by viewModel.workspaceFiles.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Auto-dismiss toast
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(3000)
            viewModel.clearToast()
        }
    }

    // Handle back button cleanly
    BackHandler(enabled = drawerState.isOpen || isPipOpen || isCopilotOpen || isSettingsOpen || isConsoleExpanded) {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            isPipOpen -> viewModel.closePipManager()
            isCopilotOpen -> viewModel.closeAiCopilot()
            isSettingsOpen -> viewModel.closeSettings()
            isConsoleExpanded -> viewModel.toggleConsole()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            WorkspaceDrawer(
                files = workspaceFiles,
                activeFile = activeFile,
                onSelectFile = { viewModel.selectFile(it) },
                onCreateFile = { viewModel.createNewFile(it) },
                onDeleteFile = { viewModel.deleteFile(it) },
                onClose = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PythonX",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CyberTertiary.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "AI",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberTertiary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            activeFile?.let { file ->
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "/ ${file.name}",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberTextMuted
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("menu_drawer_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Explorer",
                                tint = CyberTextPrimary
                            )
                        }
                    },
                    actions = {
                        // Real-time 1-Click Auto-Fix Button (when issues are detected)
                        if (lintIssues.isNotEmpty()) {
                            Button(
                                onClick = { viewModel.autoFixAllSyntax() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberAmber),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .padding(end = 4.dp)
                                    .testTag("quick_autofix_top_btn")
                            ) {
                                Text(
                                    text = "⚡ Fix ${lintIssues.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }

                        // Lint Status Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (lintIssues.isEmpty()) CyberSecondary.copy(alpha = 0.15f) else CyberError.copy(alpha = 0.15f),
                            modifier = Modifier.padding(end = 6.dp).testTag("lint_status_pill")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (lintIssues.isEmpty()) CyberSecondary else CyberError,
                                    modifier = Modifier.size(6.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (lintIssues.isEmpty()) "Clean" else "${lintIssues.size} Issues",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (lintIssues.isEmpty()) CyberSecondary else CyberError
                                )
                            }
                        }

                        // Pip Button
                        IconButton(
                            onClick = { viewModel.openPipManager() },
                            modifier = Modifier.testTag("pip_manager_top_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = "Pip",
                                tint = CyberPrimary
                            )
                        }

                        // AI Copilot Button
                        IconButton(
                            onClick = { viewModel.openAiCopilot(CopilotTask.EXPLAIN) },
                            modifier = Modifier.testTag("ai_copilot_top_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "AI Copilot",
                                tint = CyberTertiary
                            )
                        }

                        // Settings & API Key Button
                        IconButton(
                            onClick = { viewModel.openSettings() },
                            modifier = Modifier.testTag("settings_top_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = CyberTextPrimary
                            )
                        }

                        // Run Button
                        Button(
                            onClick = { viewModel.runPythonScript() },
                            enabled = !isRunning,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .padding(end = 8.dp)
                                .testTag("run_script_btn")
                        ) {
                            if (isRunning) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Run",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Run", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = CyberSurface
                    )
                )
            },
            containerColor = CyberBackground
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Code Editor (takes remaining upper space)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        CodeEditor(
                            code = codeText,
                            cursorPosition = cursorPosition,
                            onCodeChange = { newCode, newCursor ->
                                viewModel.updateCode(newCode, newCursor)
                            },
                            issues = lintIssues,
                            onQuickFix = { viewModel.applyQuickFix(it) },
                            onFixWithAi = { viewModel.openAiCopilot(CopilotTask.AUTO_FIX) },
                            onAutoFixAll = { viewModel.autoFixAllSyntax() }
                        )

                        // Floating IntelliSense popup anchored near bottom of editor
                        IntelliSensePopup(
                            suggestions = suggestions,
                            onSelect = { viewModel.applySuggestion(it) },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp)
                        )
                    }

                    // Keyboard Accessory Bar
                    AccessoryBar(
                        onInsert = { token ->
                            viewModel.insertAccessory(token)
                        }
                    )

                    // Docked Execution Console with PIP CLI support & syntax-highlighted terminal
                    ConsoleSheet(
                        events = consoleEvents,
                        isRunning = isRunning,
                        interactivePrompt = interactivePrompt,
                        onSendInput = { viewModel.submitConsoleInput(it) },
                        onRunCommand = { viewModel.executeTerminalCommand(it) },
                        onStop = { viewModel.stopPythonScript() },
                        onClear = { viewModel.clearConsole() },
                        onToggleExpand = { viewModel.toggleConsole() },
                        isExpanded = isConsoleExpanded,
                        onJumpToLine = { line -> viewModel.jumpToLine(line) },
                        onAiFixError = { errorContext ->
                            viewModel.openAiCopilot(CopilotTask.AUTO_FIX)
                            viewModel.setCopilotPromptInput(errorContext)
                        }
                    )
                }

                // Toast Notification Overlay
                AnimatedVisibility(
                    visible = toastMessage != null,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                ) {
                    toastMessage?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CyberSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberPrimary.copy(alpha = 0.5f)),
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = msg,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = CyberTextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { viewModel.clearToast() },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = CyberTextMuted, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }

                // Visual Pip Manager Dialog
                if (isPipOpen) {
                    PipManagerDialog(
                        packages = pipPackages,
                        searchQuery = pipSearchQuery,
                        statusMessage = pipStatusMessage,
                        onSearchChange = { viewModel.setPipSearchQuery(it) },
                        onSearchSubmit = { viewModel.searchPyPI() },
                        onInstall = { viewModel.installPackage(it) },
                        onUninstall = { viewModel.uninstallPackage(it) },
                        onCopySnippet = { viewModel.copySnippetToEditor(it) },
                        onDismiss = { viewModel.closePipManager() }
                    )
                }

                // AI Copilot Sheet
                if (isCopilotOpen) {
                    AiCopilotSheet(
                        isLoading = isCopilotLoading,
                        response = copilotResponse,
                        promptInput = copilotPromptInput,
                        onPromptChange = { viewModel.setCopilotPromptInput(it) },
                        onRunTask = { viewModel.executeCopilotTask(it) },
                        onApplyPatch = { viewModel.applyCopilotPatch() },
                        onDismiss = { viewModel.closeAiCopilot() }
                    )
                }

                // Settings & Custom API Key Dialog
                if (isSettingsOpen) {
                    SettingsDialog(
                        currentApiKey = geminiApiKey,
                        onSaveApiKey = { viewModel.saveGeminiApiKey(it) },
                        onClearApiKey = { viewModel.clearGeminiApiKey() },
                        onDismiss = { viewModel.closeSettings() }
                    )
                }
            }
        }
    }
}
