package com.vnstudio.vnpad.model

import kotlinx.serialization.Serializable

/**
 * Board configuration. Pads auto-fit to fill the screen, so no fixed grid size
 * is stored — only the named [pages] the user swipes between (e.g. "Filters",
 * "Analysis").
 */
@Serializable
data class GridSettings(
    val pages: List<String> = listOf("Main"),
) {
    companion object {
        const val MAX_PAGES = 8
    }
}
