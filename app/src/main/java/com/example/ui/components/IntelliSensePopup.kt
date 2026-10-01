package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.IntelliSenseItem
import com.example.model.IntelliSenseType
import com.example.ui.theme.CyberAmber
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
fun IntelliSensePopup(
    suggestions: List<IntelliSenseItem>,
    onSelect: (IntelliSenseItem) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = suggestions.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .testTag("intellisense_popup"),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberPrimary.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                // Header badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ INTELLISENSE AUTO-COMPLETE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberPrimary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${suggestions.size} matches",
                        fontSize = 10.sp,
                        color = CyberTextMuted
                    )
                }

                HorizontalDivider(color = CyberSurfaceHighlight)

                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(suggestions) { item ->
                        SuggestionRow(item = item, onClick = { onSelect(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    item: IntelliSenseItem,
    onClick: () -> Unit
) {
    val (typeColor, typeIcon, typeLabel) = when (item.type) {
        IntelliSenseType.KEYWORD -> Triple(CyberTertiary, Icons.Default.Key, "kw")
        IntelliSenseType.BUILTIN -> Triple(CyberPrimary, Icons.Default.Functions, "fn")
        IntelliSenseType.FUNCTION -> Triple(CyberSecondary, Icons.Default.Functions, "def")
        IntelliSenseType.VARIABLE -> Triple(CyberAmber, Icons.Default.DataObject, "var")
        IntelliSenseType.CLASS -> Triple(Color(0xFF60A5FA), Icons.Default.ViewInAr, "class")
        IntelliSenseType.SNIPPET -> Triple(Color(0xFFF43F5E), Icons.Default.Code, "snip")
        IntelliSenseType.MODULE -> Triple(Color(0xFF14B8A6), Icons.Default.Extension, "mod")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("suggestion_item_${item.label}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Type Badge
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = typeColor.copy(alpha = 0.15f),
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = typeLabel,
                    tint = typeColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Label
        Text(
            text = item.label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = CyberTextPrimary
        )

        // Detail / Signature
        if (item.detail.isNotEmpty()) {
            Text(
                text = item.detail,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = CyberTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
