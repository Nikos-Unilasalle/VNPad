package com.vnstudio.vnpad.data

import com.vnstudio.vnpad.model.Pad

/**
 * Starter layout shown before the user customises anything. Uses node types
 * known to exist in VNStudio so the pads work out of the box.
 */
fun defaultPads(): List<Pad> = listOf(
    Pad(id = "p1", label = "Image", nodeType = "input_image", icon = "Image", colorHex = 0xFFE76E50),
    Pad(id = "p2", label = "Video", nodeType = "input_movie", icon = "Videocam", colorHex = 0xFFF9A64E),
    Pad(id = "p3", label = "Note", nodeType = "canvas_note", icon = "StickyNote2", colorHex = 0xFFF5C400),
    Pad(id = "p4", label = "Help", nodeType = "llm_conversation", icon = "AutoAwesome", colorHex = 0xFF2A9D90),
)
