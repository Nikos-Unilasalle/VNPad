package com.vnstudio.vnpad.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vnstudio.vnpad.model.Pad
import com.vnstudio.vnpad.ui.LocalPadSkin
import com.vnstudio.vnpad.ui.iconFor
import com.vnstudio.vnpad.ui.theme.JetBrainsMono
import kotlin.math.roundToInt

/**
 * A single stream-deck pad: a glossy, coloured key that visibly *lights up* when
 * held — the face brightens, the outer glow swells and a rim highlight kicks in.
 * Press fires [onPressFeedback] (haptic + click); a normal tap fires [onTap];
 * long-press (or any tap in [editing] mode) fires [onLongPress]. In [editing]
 * mode a drag handle appears; dragging it reports raw pointer deltas via
 * [onDragStart]/[onDrag]/[onDragEnd] so the parent grid can reorder pads.
 * [isDragging] renders this instance lifted (used for the floating drag copy).
 */
@Composable
fun PadButton(
    pad: Pad,
    editing: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onDelete: () -> Unit,
    onPressFeedback: () -> Unit = {},
    isDragging: Boolean = false,
    onDragStart: () -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else if (isDragging) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 900f),
        label = "padScale",
    )
    // 0 = resting, 1 = fully lit. Drives glow, sheen and rim together.
    val lit by animateFloatAsState(
        targetValue = if (pressed || isDragging) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 700f),
        label = "padLit",
    )

    val base = Color(pad.colorHex)
    val top = lerp(base, Color.White, 0.32f)
    val bottom = lerp(base, Color.Black, 0.12f)
    // Always-on coloured glow (backlit feel), swelling further while held/dragged.
    val glow = 24f + 22f * lit + if (isDragging) 14f else 0f

    Box(
        modifier = modifier
            .scale(scale)
            // Real coloured halo drawn behind, overflowing into the gaps — a
            // device-independent backlit glow (colored elevation shadows are too
            // faint on their own). Swells while held.
            .drawBehind {
                val radius = size.minDimension * (0.85f + 0.25f * lit)
                val k = 0.75f + 0.25f * lit
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to base.copy(alpha = 0.85f * k),
                            0.45f to base.copy(alpha = 0.45f * k),
                            1.0f to Color.Transparent,
                        ),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            }
            .shadow(
                elevation = glow.dp,
                shape = RoundedCornerShape(6.dp),
                ambientColor = base,
                spotColor = base,
            )
            .clip(RoundedCornerShape(6.dp))
            .background(Brush.linearGradient(listOf(top, base, bottom)))
            .border(
                BorderStroke(1.dp, lerp(base, Color.White, 0.35f + 0.4f * lit).copy(alpha = 0.5f + 0.4f * lit)),
                RoundedCornerShape(6.dp),
            )
            .pointerInput(editing, pad.id) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onPressFeedback()
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { if (editing) onLongPress() else onTap() },
                    onLongPress = { onLongPress() },
                )
            },
    ) {
        // Resting glassy sheen at the top-left.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                        radius = 220f,
                    )
                )
        )
        // Realistic skin drawn with a Multiply blend: the neutral button's white
        // face lets the pad colour through, while the dark frame and shadows stay
        // dark. Missing asset → nothing drawn, flat pad remains.
        LocalPadSkin.current?.let { skin ->
            Canvas(Modifier.fillMaxSize()) {
                drawImage(
                    image = skin,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(skin.width, skin.height),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    blendMode = BlendMode.Multiply,
                )
            }
        }

        // Emissive wash: add the pad colour back additively so the key looks lit
        // from within (counters the Multiply darkening, gives punch + emission).
        Canvas(Modifier.fillMaxSize()) {
            drawRect(color = base, alpha = 0.30f, blendMode = BlendMode.Plus)
        }

        // Light-up wash: a white bloom that fades in while held.
        if (lit > 0.01f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.30f * lit),
                                Color.White.copy(alpha = 0.06f * lit),
                                Color.Transparent,
                            )
                        )
                    )
            )
        }

        Icon(
            imageVector = iconFor(pad.icon),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .size(22.dp),
        )

        // Label: centred both ways, in a darkened shade of the pad's own colour
        // so it reads against the lit face.
        Text(
            text = pad.label,
            color = lerp(base, Color.Black, 0.62f),
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.ExtraBold,
            fontSize = pad.textSize.sp,
            lineHeight = (pad.textSize * 1.15f).sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        )

        if (editing) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .pointerInput(pad.id) { detectTapGestures(onTap = { onDelete() }) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(15.dp))
            }

            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .pointerInput(pad.id) {
                        detectDragGestures(
                            onDragStart = { onDragStart() },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                            onDrag = { change, dragAmount -> change.consume(); onDrag(dragAmount) },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.DragIndicator, contentDescription = "Drag to reorder", tint = Color.White, modifier = Modifier.size(15.dp))
            }
        }
    }
}
