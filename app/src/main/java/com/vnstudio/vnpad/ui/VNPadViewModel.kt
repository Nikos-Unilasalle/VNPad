package com.vnstudio.vnpad.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnstudio.vnpad.data.PadStore
import com.vnstudio.vnpad.data.defaultPads
import com.vnstudio.vnpad.model.GridSettings
import com.vnstudio.vnpad.model.NodeSchema
import com.vnstudio.vnpad.model.Pad
import com.vnstudio.vnpad.net.ConnStatus
import com.vnstudio.vnpad.net.ConnectionInfo
import com.vnstudio.vnpad.net.VNPadClient
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Ties the persisted pad layout to the live LAN connection. */
class VNPadViewModel(app: Application) : AndroidViewModel(app) {
    private val store = PadStore(app.applicationContext)
    private val client = VNPadClient()

    val pads: StateFlow<List<Pad>> =
        store.pads.stateIn(viewModelScope, SharingStarted.Eagerly, defaultPads())

    val connection: StateFlow<ConnectionInfo?> =
        store.connection.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val settings: StateFlow<GridSettings> =
        store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, GridSettings())

    val status: StateFlow<ConnStatus> = client.status
    val schemas: StateFlow<List<NodeSchema>> = client.schemas

    init {
        // Auto-reconnect to the last paired host on launch.
        viewModelScope.launch {
            store.connection.first()?.let { client.connect(it) }
        }
    }

    fun pair(info: ConnectionInfo) {
        viewModelScope.launch { store.saveConnection(info) }
        client.connect(info)
    }

    fun reconnect() {
        connection.value?.let { client.connect(it) }
    }

    fun disconnect() = client.disconnect()

    /** Fire a pad. Returns false if not connected (UI shows a hint). */
    fun trigger(pad: Pad): Boolean = client.sendPad(pad)

    fun upsertPad(pad: Pad) {
        viewModelScope.launch {
            val current = pads.value
            val next = if (current.any { it.id == pad.id }) {
                current.map { if (it.id == pad.id) pad else it }
            } else {
                current + pad
            }
            store.savePads(next)
        }
    }

    fun deletePad(id: String) {
        viewModelScope.launch { store.savePads(pads.value.filterNot { it.id == id }) }
    }

    fun movePad(from: Int, to: Int) {
        val list = pads.value.toMutableList()
        if (from !in list.indices || to !in list.indices) return
        list.add(to, list.removeAt(from))
        viewModelScope.launch { store.savePads(list) }
    }

    /** Persist a page's pads in a new order (drag-to-reorder). Other pages untouched. */
    fun reorderPage(page: Int, newOrder: List<Pad>) {
        val others = pads.value.filterNot { it.page == page }
        viewModelScope.launch { store.savePads(others + newOrder) }
    }

    // ---- Pages ----

    fun addPage(name: String) = updateSettings {
        if (it.pages.size >= GridSettings.MAX_PAGES) it
        else it.copy(pages = it.pages + name.trim().ifBlank { "Page ${it.pages.size + 1}" })
    }

    fun renamePage(index: Int, name: String) = updateSettings {
        if (index !in it.pages.indices) it
        else it.copy(pages = it.pages.toMutableList().apply { this[index] = name.trim().ifBlank { "Page ${index + 1}" } })
    }

    /** Delete a page: drop its pads and shift later pages' pads down one index. */
    fun deletePage(index: Int) {
        val current = settings.value
        if (current.pages.size <= 1 || index !in current.pages.indices) return
        val newPages = current.pages.toMutableList().apply { removeAt(index) }
        updateSettings { it.copy(pages = newPages) }
        val remapped = pads.value
            .filterNot { it.page == index }
            .map { if (it.page > index) it.copy(page = it.page - 1) else it }
        viewModelScope.launch { store.savePads(remapped) }
    }

    private fun updateSettings(transform: (GridSettings) -> GridSettings) {
        viewModelScope.launch { store.saveSettings(transform(settings.value)) }
    }
}
