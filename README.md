# VNPad

A **stream-deck for [VNStudio](../VNStudio)** — an Android phone/tablet grid of
programmable pads. Each pad adds a node to the VNStudio canvas over the local
network. Inspired by the RØDECaster Pro II pad layout.

## How it connects

```
 Android (VNPad)                    Desktop (VNStudio / Tauri)
 ┌───────────────┐   Wi-Fi / LAN    ┌──────────────────────────────┐
 │  pad grid     │  ws://ip:8770    │  Rust WS server (src-tauri)   │
 │  tap → add_node├─────────────────►  auth by token                │
 │               │                  │  emit "vnpad-command" ────────┼──► React
 └───────────────┘                  │  engine stays on 127.0.0.1     │    addNode()
                                     └──────────────────────────────┘
```

- **Transport:** WebSocket over the shared Wi-Fi LAN.
- **Pairing:** VNStudio shows a QR (`VNPad` button, top-left). The phone scans
  it; the QR carries `{ip, port, token}`. A per-session token authenticates the
  connection so a stray device can't inject nodes. Manual IP/port/code entry is
  the fallback.
- **The engine is never exposed.** Commands land in the Tauri Rust server, are
  authenticated, then re-emitted to the React frontend, which already knows how
  to apply `{type:"add_node", node_type, params}`.

## VNStudio side (in the VNStudio repo)

- `src-tauri/src/vnpad.rs` — LAN WS server, token, QR SVG, schema cache.
- `src-tauri/src/main.rs` — starts the server, registers commands.
- `src/components/vnpad/VNPadPairing.tsx` — QR pairing modal.
- `src/App.tsx` — `vnpad-command` listener + schema push to Rust.

## Android app (this repo)

Kotlin + Jetpack Compose. Single module `:app`.

| Area | File |
|------|------|
| Pad model + palette | `model/Pad.kt` |
| Node schema (from host) | `model/NodeSchema.kt` |
| WebSocket client | `net/VNPadClient.kt` |
| Persistence (DataStore) | `data/PadStore.kt` |
| ViewModel | `ui/VNPadViewModel.kt` |
| Pad grid + status | `ui/PadGridScreen.kt` |
| The pad key (glow/press) | `ui/components/PadButton.kt` |
| QR pairing + camera | `ui/PairingScreen.kt` |
| Pad editor | `ui/PadEditorScreen.kt` |

## Build & run

Requires JDK 21 (AGP 8.7 rejects JDK 25).

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
./gradlew :app:assembleDebug          # build
./gradlew :app:installDebug           # install on a connected device
```

Then, in VNStudio, click **VNPad** (top-left), scan the QR, and start tapping.

## Usage

1. Both devices on the same Wi-Fi.
2. VNStudio → **VNPad** button → QR modal.
3. VNPad app → QR icon → scan (or type the address).
4. Tap a pad → a node appears on the VNStudio canvas.
5. Tap the pencil to enter edit mode: add pads, delete, or long-press a pad to
   program its label, target node type, colour, icon, and default params.

## Status

MVP. Preset node + params per pad. Node-type autocomplete is fed by the host's
live schema list. Drag-to-reorder and multi-page paging are stubbed in the model
(`page` field, `movePad`) but not yet wired to gestures.
