package com.vnstudio.vnpad.model

import kotlinx.serialization.Serializable

/**
 * Minimal view of a VNStudio node schema, as served by the Rust server's
 * `get_schemas` reply. Only the fields the pad editor needs are decoded; the
 * decoder ignores everything else.
 */
@Serializable
data class NodeSchema(
    val type: String,
    val label: String = "",
    val category: String = "",
)
