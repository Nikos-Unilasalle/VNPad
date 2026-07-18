package com.vnstudio.vnpad.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.vnstudio.vnpad.net.ConnectionInfo
import com.vnstudio.vnpad.ui.theme.VnAccent
import com.vnstudio.vnpad.ui.theme.VnSurface
import com.vnstudio.vnpad.ui.theme.VnTextDim
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.Executors

/** Pairing: scan the QR shown by VNStudio, or type the address by hand. */
@Composable
fun PairingScreen(onPaired: (ConnectionInfo) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCamera = granted }

    Column(Modifier.fillMaxSize().padding(bottom = 16.dp)) {
        Row(onBack)

        if (hasCamera) {
            QrScanner(
                onResult = { payload -> parsePairing(payload)?.let(onPaired) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp)),
            )
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Button(onClick = { permLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Enable camera to scan")
                }
            }
        }

        Text(
            "or enter the address manually",
            color = VnTextDim,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        ManualEntry(onPaired)
    }
}

@Composable
private fun Row(onBack: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().background(VnSurface).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Text("Pair with VNStudio", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
    }
}

@Composable
private fun ManualEntry(onPaired: (ConnectionInfo) -> Unit) {
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8770") }
    var token by remember { mutableStateOf("") }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(value = ip, onValueChange = { ip = it }, label = { Text("Host IP") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = port, onValueChange = { port = it.filter(Char::isDigit) }, label = { Text("Port") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = token, onValueChange = { token = it.uppercase() }, label = { Text("Code") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                val p = port.toIntOrNull() ?: return@Button
                if (ip.isNotBlank() && token.isNotBlank()) onPaired(ConnectionInfo(ip.trim(), p, token.trim()))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Connect", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun QrScanner(onResult: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }
    var handled by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown(); scanner.close() }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = CameraPreview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(executor) { proxy ->
                        scanBarcode(proxy, scanner) { value ->
                            if (!handled) { handled = true; ContextCompat.getMainExecutor(ctx).execute { onResult(value) } }
                        }
                    }
                    runCatching {
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize(),
        )
        // Framing guide.
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .fillMaxWidth(0.7f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .border(3.dp, VnAccent, RoundedCornerShape(18.dp)),
            )
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun scanBarcode(
    proxy: ImageProxy,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onValue: (String) -> Unit,
) {
    val media = proxy.image
    if (media == null) { proxy.close(); return }
    val input = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
    scanner.process(input)
        .addOnSuccessListener { codes ->
            codes.firstOrNull { it.valueType == Barcode.TYPE_TEXT || it.rawValue != null }
                ?.rawValue?.let(onValue)
        }
        .addOnCompleteListener { proxy.close() }
}

private val pairingJson = Json { ignoreUnknownKeys = true }

/** Decode the `{ip, port, token}` payload embedded in the QR. */
private fun parsePairing(payload: String): ConnectionInfo? = runCatching {
    val obj = pairingJson.parseToJsonElement(payload).jsonObject
    ConnectionInfo(
        ip = obj["ip"]!!.jsonPrimitive.content,
        port = obj["port"]!!.jsonPrimitive.content.toInt(),
        token = obj["token"]!!.jsonPrimitive.content,
    )
}.getOrNull()
