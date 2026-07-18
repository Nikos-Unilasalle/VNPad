package com.vnstudio.vnpad.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lens
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector

/** Curated icon set offered in the editor and resolved for pad rendering. */
val PAD_ICONS: Map<String, ImageVector> = linkedMapOf(
    "Bolt" to Icons.Filled.Bolt,
    "Image" to Icons.Filled.Image,
    "Videocam" to Icons.Filled.Videocam,
    "Camera" to Icons.Filled.Camera,
    "StickyNote2" to Icons.Filled.StickyNote2,
    "AutoAwesome" to Icons.Filled.AutoAwesome,
    "FilterAlt" to Icons.Filled.FilterAlt,
    "Lens" to Icons.Filled.Lens,
    "Crop" to Icons.Filled.Crop,
    "Palette" to Icons.Filled.Palette,
    "Tune" to Icons.Filled.Tune,
    "GridView" to Icons.Filled.GridView,
    "HelpOutline" to Icons.Filled.HelpOutline,
)

fun iconFor(name: String): ImageVector = PAD_ICONS[name] ?: Icons.Filled.Bolt
