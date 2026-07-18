package com.vnstudio.vnpad.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vnstudio.vnpad.model.NodeSchema
import com.vnstudio.vnpad.model.PAD_PALETTE
import com.vnstudio.vnpad.model.Pad
import com.vnstudio.vnpad.ui.theme.VnAccent
import com.vnstudio.vnpad.ui.theme.VnSurface
import com.vnstudio.vnpad.ui.theme.VnSurfaceHi
import com.vnstudio.vnpad.ui.theme.VnTextDim
import kotlinx.serialization.json.Json
import java.util.UUID

/** Program a single pad: label, target node, colour, icon, default params. */
@Composable
fun PadEditorScreen(
    initial: Pad?,
    schemas: List<NodeSchema>,
    defaultPage: Int = 0,
    onSave: (Pad) -> Unit,
    onBack: () -> Unit,
) {
    val id = remember { initial?.id ?: UUID.randomUUID().toString() }
    var label by rememberSaveable { mutableStateOf(initial?.label ?: "New pad") }
    var nodeType by rememberSaveable { mutableStateOf(initial?.nodeType ?: "") }
    var color by rememberSaveable { mutableStateOf(initial?.colorHex ?: PAD_PALETTE.first().argb) }
    var icon by rememberSaveable { mutableStateOf(initial?.icon ?: "Bolt") }
    var params by rememberSaveable { mutableStateOf(initial?.paramsJson ?: "{}") }

    val paramsValid = remember(params) { isJsonObject(params) }
    val canSave = nodeType.isNotBlank() && paramsValid

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().background(VnSurface).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Edit pad", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.weight(1f))
            IconButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        Pad(
                            id = id, label = label.trim().ifBlank { "Pad" },
                            nodeType = nodeType.trim(), paramsJson = params.ifBlank { "{}" },
                            colorHex = color, icon = icon, page = initial?.page ?: defaultPage,
                        )
                    )
                },
            ) { Icon(Icons.Filled.Check, contentDescription = "Save", tint = if (canSave) VnAccent else VnTextDim) }
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            SectionLabel("Label")
            OutlinedTextField(value = label, onValueChange = { label = it }, singleLine = true, modifier = Modifier.fillMaxWidth())

            SectionLabel("Node")
            NodePicker(schemas = schemas, selectedType = nodeType, onSelect = { nodeType = it })

            SectionLabel("Colour")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PAD_PALETTE.forEach { c ->
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(c.argb))
                            .border(if (c.argb == color) 3.dp else 0.dp, Color.White, RoundedCornerShape(12.dp))
                            .clickableNoRipple { color = c.argb },
                    )
                }
            }

            SectionLabel("Icon")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PAD_ICONS.forEach { (name, vector) ->
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (name == icon) VnAccent.copy(alpha = 0.25f) else VnSurfaceHi)
                            .border(if (name == icon) 2.dp else 0.dp, VnAccent, RoundedCornerShape(12.dp))
                            .clickableNoRipple { icon = name },
                        contentAlignment = Alignment.Center,
                    ) { Icon(vector, contentDescription = name, tint = Color.White, modifier = Modifier.size(22.dp)) }
                }
            }

            SectionLabel("Default params (JSON)")
            OutlinedTextField(
                value = params,
                onValueChange = { params = it },
                isError = !paramsValid,
                supportingText = { if (!paramsValid) Text("Must be a JSON object", color = Color(0xFFEF4444)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Name-based node picker with autocomplete. Shows the human node names from the
 * VNStudio menu; the underlying node type is stored transparently. Falls back to
 * a raw type field when the node list hasn't loaded yet (not connected).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodePicker(
    schemas: List<NodeSchema>,
    selectedType: String,
    onSelect: (String) -> Unit,
) {
    if (schemas.isEmpty()) {
        OutlinedTextField(
            value = selectedType,
            onValueChange = onSelect,
            singleLine = true,
            label = { Text("Node type") },
            supportingText = { Text("Connect to VNStudio to pick from the node list.", color = VnTextDim) },
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }

    val selectedLabel = remember(selectedType, schemas) {
        schemas.firstOrNull { it.type == selectedType }?.label ?: selectedType
    }
    var query by rememberSaveable { mutableStateOf(selectedLabel) }
    var expanded by remember { mutableStateOf(false) }

    val matches = remember(query, schemas) {
        schemas.asSequence()
            .filter { query.isBlank() || it.label.contains(query, true) || it.type.contains(query, true) }
            .sortedBy { it.label.lowercase() }
            .take(40)
            .toList()
    }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; expanded = true },
            label = { Text("Search nodes") },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            supportingText = {
                if (selectedType.isNotBlank()) Text(selectedType, color = VnTextDim, fontSize = 11.sp)
                else Text("Pick a node from the list", color = VnTextDim, fontSize = 11.sp)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded && matches.isNotEmpty(), onDismissRequest = { expanded = false }) {
            matches.forEach { s ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(s.label, color = Color.White, fontWeight = FontWeight.Medium)
                            if (s.category.isNotBlank()) Text(s.category, color = VnTextDim, fontSize = 11.sp)
                        }
                    },
                    onClick = { onSelect(s.type); query = s.label; expanded = false },
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), color = VnTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
}

private val editorJson = Json { ignoreUnknownKeys = true }

private fun isJsonObject(text: String): Boolean = runCatching {
    editorJson.parseToJsonElement(text.ifBlank { "{}" })
    text.ifBlank { "{}" }.trimStart().startsWith("{")
}.getOrDefault(false)
