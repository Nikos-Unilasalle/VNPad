package com.vnstudio.vnpad.model

import kotlinx.serialization.Serializable

/**
 * One programmable pad. Tapping it sends an `add_node` command to VNStudio.
 *
 * [paramsJson] is a raw JSON object string of default params merged into the
 * new node. [colorHex] is packed ARGB. [icon] is a Material icon key resolved
 * by [com.vnstudio.vnpad.ui.PadIcons].
 */
@Serializable
data class Pad(
    val id: String,
    val label: String,
    val nodeType: String,
    val paramsJson: String = "{}",
    val colorHex: Long = 0xFF007CF0,
    val icon: String = "Bolt",
    val page: Int = 0,
)

/** A named palette entry offered in the editor's colour picker. */
data class PadColor(val name: String, val argb: Long)

// Palette drawn from the VNPad gradient:
// #e76e50 → #f9a64e → #f5c400 → #2a9d90 → #274754 (+ an interpolated green).
val PAD_PALETTE: List<PadColor> = listOf(
    PadColor("Coral", 0xFFE76E50),
    PadColor("Orange", 0xFFF9A64E),
    PadColor("Yellow", 0xFFF5C400),
    PadColor("Green", 0xFF7FB05A),
    PadColor("Teal", 0xFF2A9D90),
    PadColor("Slate", 0xFF274754),
)
