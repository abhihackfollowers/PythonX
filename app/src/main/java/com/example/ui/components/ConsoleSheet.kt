package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DetectedTracebackInfo
import com.example.engine.TerminalOutputHighlighter
import com.example.model.ConsoleEvent
import com.example.model.ConsoleStreamType
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCoral
import com.example.ui.theme.CyberError
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberSecondary
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceHighlight
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

@Composable
fun ConsoleSheet(
    events: List<ConsoleEvent>,
    isRunning: Boolean,
    interactivePrompt: String?,
    onSendInput: (String) -> Unit,
    onRunCommand: (String) -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit,
    onToggleExpand: () -> Unit,
    isExpanded: Boolean,
    onJumpToLine: ((Int) -> Unit)? = null,
    onAiFixError: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var inputFieldValue by remember { mutableStateOf("") }
    var terminalCmdValue by remember { mutableStateOf("") }

    val hasErrors = events.any { it.type == ConsoleStreamType.STDERR }

    LaunchedEffect(events.size) {
        if (events.isNotEmpty()) {
            listState.animateScrollToItem(events.size - 1)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("console_bottom_sheet"),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (hasErrors) Color(0x60EF4444) else CyberSurfaceHighlight)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberSurfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Console",
                    tint = if (hasErrors) Color(0xFFEF4444) else CyberPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PYTHON TERMINAL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = CyberTextPrimary
                )

                if (hasErrors) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0x30EF4444)
                    ) {
                        Text(
                            text = "TRACEBACK DETECTED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFCA5A5),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                if (isRunning) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = CyberSecondary,
                        modifier = Modifier.size(8.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "RUNNING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberSecondary
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Stop button
                if (isRunning) {
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("console_stop_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = CyberError,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Clear button
                IconButton(
                    onClick = onClear,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("console_clear_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ClearAll,
                        contentDescription = "Clear",
                        tint = CyberTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Expand / Collapse toggle
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("console_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = "Toggle",
                        tint = CyberTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Quick Pip shortcut chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF070B12))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "pip list" to "pip list",
                    "pip install numpy" to "pip install numpy",
                    "pip install scipy" to "pip install scipy",
                    "pip install pymoto" to "pip install pymoto",
                    "pip install requests" to "pip install requests"
                ).forEach { (label, cmd) ->
                    Surface(
                        onClick = { onRunCommand(cmd) },
                        shape = RoundedCornerShape(4.dp),
                        color = CyberSurfaceHighlight
                    ) {
                        Text(
                            text = label,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = CyberPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Syntax-Highlighted Console Output Stream
            val consoleHeight = if (isExpanded) 280.dp else 140.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(consoleHeight)
                    .background(Color(0xFF070B12))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                if (events.isEmpty()) {
                    Text(
                        text = "Python 3.12 (ARM64) | PIP 24.0 | SciPy & PyMTOS Ready\nType 'pip install <pkg>' or tap ▶ Run to execute scripts.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = CyberTextMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    SelectionContainer {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(events) { event ->
                                val highlighted = remember(event) {
                                    TerminalOutputHighlighter.highlightTerminalOutput(event)
                                }
                                val tracebackInfo = remember(event) {
                                    if (event.type == ConsoleStreamType.STDERR || event.text.contains("Traceback")) {
                                        TerminalOutputHighlighter.parseTracebackInfo(event.text)
                                    } else null
                                }

                                val gutterColor = when (event.type) {
                                    ConsoleStreamType.STDERR -> Color(0xFFEF4444)
                                    ConsoleStreamType.STDOUT -> Color(0xFF10B981)
                                    ConsoleStreamType.SYSTEM -> Color(0xFF0EA5E9)
                                    ConsoleStreamType.PROMPT -> Color(0xFFF59E0B)
                                    ConsoleStreamType.INPUT_ECHO -> Color(0xFF64748B)
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        // Colored gutter indicator for quick visual scanning
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(18.dp)
                                                .background(gutterColor, RoundedCornerShape(2.dp))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = highlighted,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // Interactive Traceback Quick Actions Banner
                                    tracebackInfo?.let { info ->
                                        TracebackActionBanner(
                                            info = info,
                                            onJumpToLine = onJumpToLine,
                                            onAiFix = onAiFixError
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Interactive input prompt bar (when input() is waiting)
            AnimatedVisibility(visible = interactivePrompt != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberSurfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = interactivePrompt ?: "input: ",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = CyberAmber,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedTextField(
                        value = inputFieldValue,
                        onValueChange = { inputFieldValue = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("console_input_field"),
                        singleLine = true,
                        placeholder = { Text("Enter response and press Send...", fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputFieldValue.isNotEmpty()) {
                                onSendInput(inputFieldValue)
                                inputFieldValue = ""
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberPrimary,
                            unfocusedBorderColor = CyberSurfaceHighlight,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            onSendInput(inputFieldValue)
                            inputFieldValue = ""
                        },
                        modifier = Modifier.testTag("console_send_input_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = CyberPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Command / PIP CLI input bar (when not waiting on input())
            if (interactivePrompt == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberSurfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$ ",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = CyberSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = terminalCmdValue,
                        onValueChange = { terminalCmdValue = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("terminal_cli_input"),
                        singleLine = true,
                        placeholder = { Text("Run command (e.g. pip install scipy)...", fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (terminalCmdValue.isNotBlank()) {
                                onRunCommand(terminalCmdValue)
                                terminalCmdValue = ""
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberPrimary,
                            unfocusedBorderColor = CyberSurfaceHighlight,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            if (terminalCmdValue.isNotBlank()) {
                                onRunCommand(terminalCmdValue)
                                terminalCmdValue = ""
                            }
                        },
                        modifier = Modifier.testTag("terminal_cli_run_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Execute",
                            tint = CyberSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TracebackActionBanner(
    info: DetectedTracebackInfo,
    onJumpToLine: ((Int) -> Unit)?,
    onAiFix: ((String) -> Unit)?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp, start = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1B1422),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7F1D1D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = "Error Diagnostics",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "${info.errorType} at line ${info.lineNumber}",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFCA5A5),
                modifier = Modifier.weight(1f)
            )

            if (onJumpToLine != null) {
                Button(
                    onClick = { onJumpToLine(info.lineNumber) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Jump to line",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Line ${info.lineNumber}", fontSize = 10.sp, color = Color(0xFFE2E8F0))
                }
            }

            if (onAiFix != null) {
                Button(
                    onClick = {
                        val prompt = "Fix this error: ${info.errorType}: ${info.errorMessage} at line ${info.lineNumber}"
                        onAiFix(prompt)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF831843)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = "Fix with AI",
                        tint = Color(0xFFF472B6),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("AI Fix", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
