package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.PipPackage
import com.example.ui.theme.CyberAmber
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
fun PipManagerDialog(
    packages: List<PipPackage>,
    searchQuery: String,
    statusMessage: String?,
    onSearchChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onInstall: (PipPackage) -> Unit,
    onUninstall: (PipPackage) -> Unit,
    onCopySnippet: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Data Science", "Networking", "Visualization", "Scientific", "Web")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("pip_manager_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = CyberSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberSurfaceHighlight)
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
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = "Pip",
                        tint = CyberPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "VISUAL PIP PACKAGE MANAGER",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = CyberTextPrimary
                        )
                        Text(
                            text = "Install & manage PyPI packages in local runtime",
                            fontSize = 11.sp,
                            color = CyberTextMuted
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_pip_btn")) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = CyberTextPrimary
                        )
                    }
                }

                // Search Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pip_search_input"),
                        singleLine = true,
                        placeholder = { Text("Search or install any package (e.g. torch, fastapi)...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = CyberPrimary)
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberPrimary,
                            unfocusedBorderColor = CyberSurfaceHighlight,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onSearchSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                        modifier = Modifier.height(54.dp).testTag("pip_search_btn")
                    ) {
                        Text("Search")
                    }
                }

                // Direct One-Tap PIP Install Card for any typed package
                if (searchQuery.isNotBlank()) {
                    val targetName = searchQuery.trim().lowercase()
                    val isAlreadyInstalled = packages.any { it.name.equals(targetName, ignoreCase = true) && it.isInstalled }
                    if (!isAlreadyInstalled) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = CyberPrimary.copy(alpha = 0.12f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberPrimary.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "pip install $targetName",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = CyberPrimary
                                    )
                                    Text(
                                        text = "Download and link package from PyPI into local Python environment",
                                        fontSize = 11.sp,
                                        color = CyberTextSecondary
                                    )
                                }
                                Button(
                                    onClick = {
                                        onInstall(
                                            PipPackage(
                                                name = targetName,
                                                summary = "Package $targetName installed from PyPI",
                                                latestVersion = "latest",
                                                category = "PyPI",
                                                sampleUsage = "import $targetName\nprint('Imported $targetName successfully')"
                                            )
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.testTag("direct_pip_install_btn")
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = "Install", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Install PIP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Live status message banner
                statusMessage?.let { msg ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        color = CyberPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "📦 $msg",
                            fontSize = 12.sp,
                            color = CyberPrimary,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Category chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSel = cat == selectedCategory
                        Surface(
                            onClick = { selectedCategory = cat },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) CyberPrimary else CyberSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color(0xFF070B12) else CyberTextPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = CyberSurfaceHighlight, modifier = Modifier.padding(top = 8.dp))

                // Packages List
                val filtered = packages.filter {
                    val matchCat = selectedCategory == "All" || it.category == selectedCategory
                    val matchQuery = searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.summary.contains(searchQuery, ignoreCase = true)
                    matchCat && matchQuery
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered) { pkg ->
                        PackageCard(
                            pkg = pkg,
                            onInstall = { onInstall(pkg) },
                            onUninstall = { onUninstall(pkg) },
                            onCopySnippet = { onCopySnippet(pkg.sampleUsage) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PackageCard(
    pkg: PipPackage,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onCopySnippet: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("package_card_${pkg.name}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (pkg.isInstalled) CyberSecondary.copy(alpha = 0.5f) else CyberSurfaceHighlight
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pkg.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyberTextPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = CyberSurfaceHighlight
                ) {
                    Text(
                        text = "v${pkg.latestVersion}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (pkg.isInstalled) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = CyberSecondary.copy(alpha = 0.2f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Installed",
                                tint = CyberSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "INSTALLED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = pkg.summary,
                fontSize = 12.sp,
                color = CyberTextSecondary,
                lineHeight = 16.sp
            )

            if (pkg.author.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Author: ${pkg.author} | License: ${pkg.license}",
                    fontSize = 10.sp,
                    color = CyberTextMuted
                )
            }

            // Progress bar if installing
            if (pkg.isInstalling) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { pkg.installProgress },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = CyberSecondary,
                    trackColor = CyberSurfaceHighlight
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pkg.isInstalled) {
                    OutlinedButton(
                        onClick = onUninstall,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberError),
                        modifier = Modifier.height(34.dp).testTag("uninstall_btn_${pkg.name}")
                    ) {
                        Text("Uninstall", fontSize = 11.sp)
                    }
                } else {
                    Button(
                        onClick = onInstall,
                        enabled = !pkg.isInstalling,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
                        modifier = Modifier.height(34.dp).testTag("install_btn_${pkg.name}")
                    ) {
                        if (pkg.isInstalling) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Install",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Install", fontSize = 11.sp)
                        }
                    }
                }

                if (pkg.sampleUsage.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onCopySnippet,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberPrimary),
                        modifier = Modifier.height(34.dp).testTag("copy_snippet_btn_${pkg.name}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Snippet",
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Use in Code", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
