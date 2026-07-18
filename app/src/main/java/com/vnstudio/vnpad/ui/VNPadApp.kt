package com.vnstudio.vnpad.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vnstudio.vnpad.model.Pad
import kotlinx.coroutines.launch

private sealed interface Nav {
    data object Grid : Nav
    data object Pairing : Nav
    data object Settings : Nav
    data class Editor(val pad: Pad?, val page: Int) : Nav
}

@Composable
fun VNPadApp(vm: VNPadViewModel = viewModel()) {
    val pads by vm.pads.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val schemas by vm.schemas.collectAsStateWithLifecycle()

    var nav by remember { mutableStateOf<Nav>(Nav.Grid) }
    var editing by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val padSkin = rememberPadSkin()

    CompositionLocalProvider(LocalPadSkin provides padSkin) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            modifier = Modifier.fillMaxSize(),
        ) { inner ->
            val content = Modifier.fillMaxSize().padding(inner)
            when (val current = nav) {
                is Nav.Grid -> Box(content) {
                    PadGridScreen(
                        pads = pads,
                        settings = settings,
                        status = status,
                        editing = editing,
                        onToggleEdit = { editing = !editing },
                        onOpenSettings = { nav = Nav.Settings },
                        onPair = { nav = Nav.Pairing },
                        onTapPad = { pad ->
                            val sent = vm.trigger(pad)
                            if (!sent) scope.launch { snackbar.showSnackbar("Not connected — tap the QR icon to pair.") }
                        },
                        onEditPad = { nav = Nav.Editor(it, it.page) },
                        onDeletePad = { vm.deletePad(it.id) },
                        onAddPad = { page -> nav = Nav.Editor(null, page) },
                    )
                }

                is Nav.Pairing -> Box(content) {
                    PairingScreen(
                        onPaired = { info -> vm.pair(info); nav = Nav.Grid },
                        onBack = { nav = Nav.Grid },
                    )
                }

                is Nav.Settings -> Box(content) {
                    SettingsScreen(
                        settings = settings,
                        onAddPage = vm::addPage,
                        onRenamePage = vm::renamePage,
                        onDeletePage = vm::deletePage,
                        onBack = { nav = Nav.Grid },
                    )
                }

                is Nav.Editor -> Box(content) {
                    PadEditorScreen(
                        initial = current.pad,
                        schemas = schemas,
                        defaultPage = current.page,
                        onSave = { pad -> vm.upsertPad(pad); nav = Nav.Grid },
                        onBack = { nav = Nav.Grid },
                    )
                }
            }
        }
    }
}
