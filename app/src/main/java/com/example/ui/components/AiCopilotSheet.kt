package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.CopilotResponse
import com.example.ai.CopilotTask
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberSecondary
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceHighlight
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberTertiary
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

@Composable
fun AiCopilotSheet(
    isLoading: Boolean,
    response: CopilotResponse?,
    promptInput: String,
    onPromptChange: (String) -> Unit,
    onRunTask: (CopilotTask) -> Unit,
    onApplyPatch: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("ai_copilot_sheet"),
            shape = RoundedCornerShape(16.dp),
            color = CyberSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberTertiary.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberSurfaceVariant)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "AI Copilot",
                        tint = CyberTertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PYTHONX AI COPILOT",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = CyberTextPrimary
                        )
                        Text(
                            text = "Next-Gen AI Code Generator & Autonomous Bug Fixer",
                            fontSize = 11.sp,
                            color = CyberTextMuted
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_copilot_btn")) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = CyberTextPrimary
                        )
                    }
                }

                // Action chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = { onRunTask(CopilotTask.EXPLAIN) },
                        shape = RoundedCornerShape(8.dp),
                        color = CyberSurfaceVariant,
                        modifier = Modifier.weight(1f).testTag("copilot_explain_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = "Explain", tint = CyberPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Explain", fontSize = 11.sp, color = CyberTextPrimary)
                        }
                    }

                    Surface(
                        onClick = { onRunTask(CopilotTask.AUTO_FIX) },
                        shape = RoundedCornerShape(8.dp),
                        color = CyberSurfaceVariant,
                        modifier = Modifier.weight(1f).testTag("copilot_autofix_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = "Auto Fix", tint = CyberSecondary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Auto-Fix", fontSize = 11.sp, color = CyberTextPrimary)
                        }
                    }

                    Surface(
                        onClick = { onRunTask(CopilotTask.REFACTOR) },
                        shape = RoundedCornerShape(8.dp),
                        color = CyberSurfaceVariant,
                        modifier = Modifier.weight(1f).testTag("copilot_refactor_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = "Refactor", tint = CyberTertiary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refactor", fontSize = 11.sp, color = CyberTextPrimary)
                        }
                    }
                }

                // Custom Prompt Input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = onPromptChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copilot_prompt_input"),
                        singleLine = true,
                        placeholder = { Text("Prompt AI: e.g. Write a binary search algorithm...", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberTertiary,
                            unfocusedBorderColor = CyberSurfaceHighlight,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onRunTask(CopilotTask.GENERATE) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberTertiary),
                        modifier = Modifier.height(54.dp).testTag("copilot_generate_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Generate", modifier = Modifier.size(16.dp))
                    }
                }

                HorizontalDivider(color = CyberSurfaceHighlight, modifier = Modifier.padding(vertical = 8.dp))

                // Content Output Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    if (isLoading) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = CyberTertiary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "PythonX AI is thinking & synthesizing code...",
                                fontSize = 13.sp,
                                color = CyberTextSecondary
                            )
                        }
                    } else if (response != null) {
                        val scroll = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scroll)
                        ) {
                            // Explanation
                            Text(
                                text = response.explanation,
                                fontSize = 13.sp,
                                color = CyberTextPrimary,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Patched Code block preview if available
                            response.patchedCode?.let { patch ->
                                Text(
                                    text = "SUGGESTED PYTHON CODE PATCH",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberSecondary,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF070B12)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberSurfaceHighlight)
                                ) {
                                    Text(
                                        text = patch,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = CyberSecondary,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = "AI",
                                tint = CyberTertiary.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Choose an AI action above or type a prompt.",
                                fontSize = 13.sp,
                                color = CyberTextMuted
                            )
                        }
                    }
                }

                // Bottom apply button if patched code exists
                if (response?.patchedCode != null) {
                    HorizontalDivider(color = CyberSurfaceHighlight)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onApplyPatch,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("apply_copilot_patch_btn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Apply", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Apply Patch to Editor", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
