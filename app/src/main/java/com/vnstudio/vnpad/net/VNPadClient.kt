package com.vnstudio.vnpad.net

import android.util.Log
import com.vnstudio.vnpad.model.NodeSchema
import com.vnstudio.vnpad.model.Pad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/** Where and how to reach a VNStudio host, obtained via QR pairing. */
data class ConnectionInfo(val ip: String, val port: Int, val token: String) {
    val url: String get() = "ws://$ip:$port"
}

enum class ConnStatus { Disconnected, Connecting, Connected, AuthFailed, Error }

private const val TAG = "VNPadClient"

/**
 * WebSocket client for the VNStudio LAN server. Authenticates with the pairing
 * token, then sends `add_node` / `set_param` commands. All I/O is off the main
 * thread (OkHttp dispatcher); state is exposed as [StateFlow]s.
 */
class VNPadClient {
    private val json = Json { ignoreUnknownKeys = true }
    private val http = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var socket: WebSocket? = null
    private var info: ConnectionInfo? = null

    private val _status = MutableStateFlow(ConnStatus.Disconnected)
    val status: StateFlow<ConnStatus> = _status.asStateFlow()

    private val _schemas = MutableStateFlow<List<NodeSchema>>(emptyList())
    val schemas: StateFlow<List<NodeSchema>> = _schemas.asStateFlow()

    fun connect(target: ConnectionInfo) {
        disconnect()
        info = target
        _status.value = ConnStatus.Connecting
        val request = Request.Builder().url(target.url).build()
        socket = http.newWebSocket(request, listener)
    }

    fun disconnect() {
        socket?.close(1000, "bye")
        socket = null
        if (_status.value != ConnStatus.Disconnected) _status.value = ConnStatus.Disconnected
    }

    /** Send an add_node command built from a pad's config. */
    fun sendPad(pad: Pad): Boolean {
        val ws = socket ?: return false
        if (_status.value != ConnStatus.Connected) return false
        val params: JsonObject = runCatching {
            json.parseToJsonElement(pad.paramsJson).jsonObject
        }.getOrDefault(JsonObject(emptyMap()))
        val msg = buildJsonObject {
            put("type", "add_node")
            put("node_type", pad.nodeType)
            put("params", params)
        }
        return ws.send(msg.toString())
    }

    /** Ask the host for its node schemas to populate the editor's picker. */
    fun requestSchemas() {
        socket?.send("""{"type":"get_schemas"}""")
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            val token = info?.token ?: return
            val hello = buildJsonObject {
                put("type", "hello")
                put("token", token)
            }
            webSocket.send(hello.toString())
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val obj = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return
            when ((obj["type"] as? JsonPrimitive)?.content) {
                "welcome" -> {
                    _status.value = ConnStatus.Connected
                    requestSchemas()
                }
                "error" -> _status.value = ConnStatus.AuthFailed
                "schemas" -> parseSchemas(obj)
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.w(TAG, "socket failure: ${t.message}")
            if (_status.value != ConnStatus.AuthFailed) _status.value = ConnStatus.Error
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (_status.value == ConnStatus.Connected) _status.value = ConnStatus.Disconnected
        }
    }

    private fun parseSchemas(obj: JsonObject) {
        val arr = obj["nodes"]?.jsonArray ?: return
        val parsed = arr.mapNotNull { el ->
            runCatching { json.decodeFromJsonElement(NodeSchema.serializer(), el) }.getOrNull()
        }
        _schemas.value = parsed.filter { it.type.isNotBlank() }
    }
}
