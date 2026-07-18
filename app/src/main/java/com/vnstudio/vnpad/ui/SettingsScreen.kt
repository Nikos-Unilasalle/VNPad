package com.vnstudio.vnpad.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vnstudio.vnpad.model.GridSettings
import com.vnstudio.vnpad.ui.theme.VnAccent
import com.vnstudio.vnpad.ui.theme.VnSurface
import com.vnstudio.vnpad.ui.theme.VnSurfaceHi
import com.vnstudio.vnpad.ui.theme.VnTextDim

@Composable
fun SettingsScreen(
    settings: GridSettings,
    onAddPage: (String) -> Unit,
    onRenamePage: (Int, String) -> Unit,
    onDeletePage: (Int) -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().background(VnSurface).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Pages", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Pads fill the screen automatically. Add boards to swipe between (e.g. Filters, Analysis).",
                color = VnTextDim, fontSize = 12.sp,
            )
            settings.pages.forEachIndexed { index, name ->
                PageRow(
                    name = name,
                    canDelete = settings.pages.size > 1,
                    onRename = { onRenamePage(index, it) },
                    onDelete = { onDeletePage(index) },
                )
            }
            if (settings.pages.size < GridSettings.MAX_PAGES) {
                AddPageButton { onAddPage("Page ${settings.pages.size + 1}") }
            }
        }
    }
}

@Composable
private fun PageRow(name: String, canDelete: Boolean, onRename: (String) -> Unit, onDelete: () -> Unit) {
    var text by remember(name) { mutableStateOf(name) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        if (text != name) {
            IconButton(onClick = { onRename(text) }) {
                Icon(Icons.Filled.Check, contentDescription = "Rename", tint = VnAccent)
            }
        }
        if (canDelete) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete page", tint = Color(0xFFEF4444))
            }
        }
    }
}

@Composable
private fun AddPageButton(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, VnSurfaceHi, RoundedCornerShape(10.dp))
            .clickableNoRipple(onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Text("Add page", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp))
    }
}
