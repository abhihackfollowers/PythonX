package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.PythonHighlighter
import com.example.model.LintIssue
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBackground
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
fun CodeEditor(
    code: String,
    cursorPosition: Int = 0,
    onCodeChange: (String, Int) -> Unit,
    issues: List<LintIssue>,
    onQuickFix: (LintIssue) -> Unit,
    onFixWithAi: () -> Unit,
    onAutoFixAll: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = code, selection = TextRange(cursorPosition.coerceIn(0, code.length))))
    }

    // Keep internal text state synchronized with external code updates
    LaunchedEffect(code) {
        if (textFieldValue.text != code) {
            val safeSelection = textFieldValue.selection.end.coerceIn(0, code.length)
            textFieldValue = textFieldValue.copy(
                text = code,
                selection = TextRange(safeSelection)
            )
        }
    }

    LaunchedEffect(cursorPosition) {
        val safeSelection = cursorPosition.coerceIn(0, textFieldValue.text.length)
        if (textFieldValue.selection.end != safeSelection) {
            textFieldValue = textFieldValue.copy(selection = TextRange(safeSelection))
        }
    }

    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    val lines = code.split("\n")
    val totalLines = lines.size.coerceAtLeast(1)
    val errorLineMap = issues.groupBy { it.line }

    var selectedIssue by remember { mutableStateOf<LintIssue?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .testTag("code_editor_container")
    ) {
        // Active Quick-Fix Banner when an error is clicked or detected
        selectedIssue?.let { issue ->
            ErrorBanner(
                issue = issue,
                onDismiss = { selectedIssue = null },
                onApplyFix = {
                    onQuickFix(issue)
                    selectedIssue = null
                },
                onAutoFixAll = {
                    onAutoFixAll()
                    selectedIssue = null
                },
                onAiFix = {
                    onFixWithAi()
                    selectedIssue = null
                }
            )
        }

        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScroll)
            ) {
                // Line Number Gutter
                Column(
                    modifier = Modifier
                        .width(44.dp)
                        .background(CyberSurface)
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    for (lineIndex in 1..totalLines) {
                        val hasError = errorLineMap.containsKey(lineIndex)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .height(22.dp)
                                .fillMaxWidth()
                        ) {
                            if (hasError) {
                                Surface(
                                    shape = CircleShape,
                                    color = CyberError,
                                    modifier = Modifier
                                        .size(6.dp)
                                        .testTag("error_dot_$lineIndex"),
                                    onClick = {
                                        selectedIssue = errorLineMap[lineIndex]?.firstOrNull()
                                    }
                                ) {}
                                Spacer(modifier = Modifier.width(4.dp))
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            Text(
                                text = "$lineIndex",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (hasError) CyberError else CyberTextMuted
                            )
                        }
                    }
                }

                // Code Area with Syntax Highlighting
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .horizontalScroll(horizontalScroll)
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { newVal ->
                            textFieldValue = newVal
                            onCodeChange(newVal.text, newVal.selection.end)
                        },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = CyberTextPrimary
                        ),
                        cursorBrush = SolidColor(CyberPrimary),
                        visualTransformation = {
                            androidx.compose.ui.text.input.TransformedText(
                                text = PythonHighlighter.highlight(it.text, issues),
                                offsetMapping = androidx.compose.ui.text.input.OffsetMapping.Identity
                            )
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("python_code_input")
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    issue: LintIssue,
    onDismiss: () -> Unit,
    onApplyFix: () -> Unit,
    onAutoFixAll: () -> Unit,
    onAiFix: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .testTag("error_fix_banner"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberError.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = CyberError,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Line ${issue.line}: ${issue.message}",
                    color = CyberTextPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = CyberTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (issue.quickFix != null) {
                    Button(
                        onClick = onApplyFix,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("⚡ ${issue.quickFix}", fontSize = 11.sp)
                    }
                }

                Button(
                    onClick = onAutoFixAll,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberAmber),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(30.dp).testTag("fix_all_errors_btn")
                ) {
                    Text("⚡ Fix All Errors", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }

                Button(
                    onClick = onAiFix,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = "AI Fix",
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fix with AI", fontSize = 11.sp)
                }
            }
        }
    }
}
