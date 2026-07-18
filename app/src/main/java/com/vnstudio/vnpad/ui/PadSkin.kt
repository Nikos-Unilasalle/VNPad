package com.vnstudio.vnpad.ui

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

/**
 * Optional realistic pad skin. Drop a color-neutral button PNG (thin square
 * frame, raised glossy button, transparent background) at this asset name. It
 * is drawn over each pad's coloured face with a Multiply blend, so white/glass
 * areas let the colour show through while the frame and shadows stay dark. If
 * the file is absent, [rememberPadSkin] returns null and pads keep their flat
 * gradient look — nothing breaks.
 */
const val PAD_SKIN_ASSET = "pad_skin.png"

/** The loaded skin bitmap, or null when the asset is missing. */
val LocalPadSkin = staticCompositionLocalOf<ImageBitmap?> { null }

@Composable
fun rememberPadSkin(): ImageBitmap? {
    val context = LocalContext.current
    return remember {
        runCatching {
            context.assets.open(PAD_SKIN_ASSET).use { stream ->
                BitmapFactory.decodeStream(stream).asImageBitmap()
            }
        }.getOrNull()
    }
}
