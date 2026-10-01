package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberSurfaceHighlight
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberTextPrimary

@Composable
fun AccessoryBar(
    onInsert: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickTokens = listOf(
        "TAB" to "    ",
        ":" to ":",
        "(" to "(",
        ")" to ")",
        "[" to "[",
        "]" to "]",
        "{" to "{",
        "}" to "}",
        "\"" to "\"",
        "'" to "'",
        "=" to " = ",
        "==" to " == ",
        "!=" to " != ",
        "->" to " -> ",
        "_" to "_",
        "." to ".",
        "," to ", ",
        "+" to " + ",
        "-" to " - ",
        "*" to " * ",
        "/" to " / ",
        "def" to "def ",
        "self" to "self",
        "return" to "return "
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("accessory_bar"),
        color = CyberSurfaceVariant,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickTokens.forEach { (label, token) ->
                Surface(
                    onClick = { onInsert(token) },
                    shape = RoundedCornerShape(6.dp),
                    color = CyberSurfaceHighlight,
                    modifier = Modifier.testTag("accessory_btn_$label")
                ) {
                    Text(
                        text = label,
                        color = if (label == "TAB" || label == "def" || label == "return") CyberPrimary else CyberTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
