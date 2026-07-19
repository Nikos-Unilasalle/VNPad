package com.vnstudio.vnpad.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.vnstudio.vnpad.R
import com.vnstudio.vnpad.model.GridSettings
import com.vnstudio.vnpad.model.Pad
import com.vnstudio.vnpad.net.ConnStatus
import com.vnstudio.vnpad.ui.components.PadButton
import com.vnstudio.vnpad.ui.theme.VnAccent
import com.vnstudio.vnpad.ui.theme.VnBg
import com.vnstudio.vnpad.ui.theme.VnSurface
import com.vnstudio.vnpad.ui.theme.VnSurfaceHi
import com.vnstudio.vnpad.ui.theme.VnTextDim
import kotlin.math.roundToInt

private val GAP = 2.dp

@Composable
fun PadGridScreen(
    pads: List<Pad>,
    settings: GridSettings,
    status: ConnStatus,
    editing: Boolean,
    fullscreen: Boolean,
    onToggleEdit: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onOpenSettings: () -> Unit,
    onPair: () -> Unit,
    onTapPad: (Pad) -> Unit,
    onEditPad: (Pad) -> Unit,
    onDeletePad: (Pad) -> Unit,
    onAddPad: (page: Int) -> Unit,
    onReorderPage: (page: Int, newOrder: List<Pad>) -> Unit,
) {
    val feedback = rememberPadFeedback()
    val pageCount = settings.pages.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            if (!fullscreen) {
                TopBar(
                    status = status,
                    editing = editing,
                    onToggleEdit = onToggleEdit,
                    onToggleFullscreen = onToggleFullscreen,
                    onOpenSettings = onOpenSettings,
                    onPair = onPair,
                )
                PageIndicator(pages = settings.pages, current = pagerState.currentPage)
            }

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                AutoFitPadGrid(
                    pads = pads.filter { it.page == page },
                    editing = editing,
                    onTapPad = onTapPad,
                    onEditPad = onEditPad,
                    onDeletePad = onDeletePad,
                    onAddPad = { onAddPad(page) },
                    onReorder = { onReorderPage(page, it) },
                    onPressFeedback = { feedback.tap() },
                )
            }
        }

        // Fullscreen hides the chrome, so keep one discreet way back out.
        if (fullscreen) {
            IconButton(
                onClick = onToggleFullscreen,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.35f)),
            ) {
                Icon(
                    Icons.Filled.FullscreenExit,
                    contentDescription = "Exit fullscreen",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * Lays the page's pads out to fill the screen. Pads can span several cells
 * (individual sizes), so placement is a first-fit pack; the column count is
 * chosen to maximise cell size for the current width×height, which keeps the
 * board full and re-adapts on rotation.
 *
 * Drag-to-reorder: a pad's drag handle (edit mode only) reports raw pointer
 * deltas. The dragged pad leaves the flow and floats under the finger, while
 * [liveOrder] reorders on the fly — the drop target is hit-tested against the
 * packed rects, so it works with mixed pad sizes. Released order goes to
 * [onReorder].
 */
@Composable
private fun AutoFitPadGrid(
    pads: List<Pad>,
    editing: Boolean,
    onTapPad: (Pad) -> Unit,
    onEditPad: (Pad) -> Unit,
    onDeletePad: (Pad) -> Unit,
    onAddPad: () -> Unit,
    onReorder: (List<Pad>) -> Unit,
    onPressFeedback: () -> Unit,
) {
    var liveOrder by remember(pads) { mutableStateOf(pads) }
    var dragId by remember { mutableStateOf<String?>(null) }
    var dragOrigin by remember { mutableStateOf(Offset.Zero) } // slot top-left (box-local px) at drag start
    var dragOffset by remember { mutableStateOf(Offset.Zero) } // raw cumulative pointer delta

    // Leaving edit mode mid-drag shouldn't strand a floating pad.
    LaunchedEffect(editing) { if (!editing) dragId = null }

    val density = LocalDensity.current
    val hasAddTile = editing

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().padding(GAP),
        contentAlignment = Alignment.Center,
    ) {
        if (liveOrder.isEmpty() && !hasAddTile) {
            Text("Tap ✏ then + to add a pad", color = VnTextDim, fontSize = 13.sp)
            return@BoxWithConstraints
        }

        // Spans of everything laid out: the pads, plus the trailing add tile.
        val spans = liveOrder.map { it.spanX to it.spanY } + if (hasAddTile) listOf(1 to 1) else emptyList()
        val widest = spans.maxOf { it.first }
        val totalCells = spans.sumOf { it.first * it.second }

        // Try each column count; keep the one giving the biggest cell.
        var bestPack = packGrid(spans, widest)
        var cell = 0.dp
        for (c in widest..maxOf(widest, totalCells)) {
            val pack = packGrid(spans, c)
            if (pack.rows < 1) continue
            val cw = (maxWidth - GAP * (c - 1)) / c
            val ch = (maxHeight - GAP * (pack.rows - 1)) / pack.rows
            val candidate = minOf(cw, ch)
            if (candidate > cell) { cell = candidate; bestPack = pack }
        }

        val nCols = bestPack.cols
        val nRows = bestPack.rows
        val cellPx = with(density) { cell.toPx() }
        val gapPx = with(density) { GAP.toPx() }
        val stridePx = cellPx + gapPx
        fun spanPx(span: Int) = span * cellPx + (span - 1) * gapPx

        val gridWidthPx = spanPx(nCols)
        val gridHeightPx = spanPx(nRows)
        // Matches contentAlignment = Center, so offsets agree with the layout.
        val originX = (with(density) { maxWidth.toPx() } - gridWidthPx) / 2f
        val originY = (with(density) { maxHeight.toPx() } - gridHeightPx) / 2f

        val slotOf = bestPack.placements.associateBy { it.index }
        val orderIndex = liveOrder.withIndex().associate { (i, pad) -> pad.id to i }

        // Compose in a STABLE order (the incoming `pads`), not in the live
        // reordered order: keyed by id, each pad keeps its identity — and with
        // it the in-flight drag gesture — while only its offset changes.
        pads.forEach { pad ->
            val index = orderIndex[pad.id] ?: return@forEach
            val placement = slotOf[index] ?: return@forEach
            val isDragged = pad.id == dragId

            val x = originX + placement.col * stridePx
            val y = originY + placement.row * stridePx
            // The dragged pad ignores its live slot and tracks the finger from
            // the slot it started in, so reflowing peers don't yank it around.
            val posX = if (isDragged) dragOrigin.x + dragOffset.x else x
            val posY = if (isDragged) dragOrigin.y + dragOffset.y else y

            key(pad.id) {
                PadButton(
                    pad = pad,
                    editing = editing,
                    isDragging = isDragged,
                    onTap = { onTapPad(pad) },
                    onLongPress = { onEditPad(pad) },
                    onDelete = { onDeletePad(pad) },
                    onPressFeedback = onPressFeedback,
                    onDragStart = {
                        dragId = pad.id
                        dragOrigin = Offset(x, y)
                        dragOffset = Offset.Zero
                    },
                    onDrag = { delta ->
                        dragOffset += delta
                        val cx = dragOrigin.x + dragOffset.x + spanPx(placement.spanX) / 2f
                        val cy = dragOrigin.y + dragOffset.y + spanPx(placement.spanY) / 2f
                        val target = bestPack.placements.firstOrNull { p ->
                            val px = originX + p.col * stridePx
                            val py = originY + p.row * stridePx
                            cx >= px && cx <= px + spanPx(p.spanX) &&
                                cy >= py && cy <= py + spanPx(p.spanY)
                        }?.index?.takeIf { it < liveOrder.size }
                        val current = liveOrder.indexOfFirst { it.id == dragId }
                        if (target != null && current >= 0 && target != current) {
                            liveOrder = liveOrder.toMutableList().apply { add(target, removeAt(current)) }
                        }
                    },
                    onDragEnd = { dragId = null; onReorder(liveOrder) },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .zIndex(if (isDragged) 10f else 0f)
                        .offset { IntOffset(posX.roundToInt(), posY.roundToInt()) }
                        .size(
                            width = with(density) { spanPx(placement.spanX).toDp() },
                            height = with(density) { spanPx(placement.spanY).toDp() },
                        ),
                )
            }
        }

        slotOf[liveOrder.size]?.let { placement ->
            AddPadTile(
                Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        IntOffset(
                            (originX + placement.col * stridePx).roundToInt(),
                            (originY + placement.row * stridePx).roundToInt(),
                        )
                    }
                    .size(
                        width = with(density) { spanPx(placement.spanX).toDp() },
                        height = with(density) { spanPx(placement.spanY).toDp() },
                    ),
                onAddPad,
            )
        }
    }
}

@Composable
private fun TopBar(
    status: ConnStatus,
    editing: Boolean,
    onToggleEdit: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onOpenSettings: () -> Unit,
    onPair: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(VnSurface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(VnBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(30.dp),
            )
        }
        Text(
            "V N P A D",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(start = 10.dp),
        )
        Box(Modifier.padding(start = 12.dp)) { StatusPill(status) }
        Spacer(Modifier.weight(1f))
        IconGroup(
            editing = editing,
            onToggleEdit = onToggleEdit,
            onToggleFullscreen = onToggleFullscreen,
            onOpenSettings = onOpenSettings,
            onPair = onPair,
        )
    }
}

@Composable
private fun StatusPill(status: ConnStatus) {
    val (color, text) = when (status) {
        ConnStatus.Connected -> Color(0xFF22C55E) to "CONNECTED"
        ConnStatus.Connecting -> Color(0xFFF59E0B) to "CONNECTING"
        ConnStatus.AuthFailed -> Color(0xFFEF4444) to "AUTH_FAILED"
        ConnStatus.Error -> Color(0xFFEF4444) to "OFFLINE"
        ConnStatus.Disconnected -> VnTextDim to "NOT_PAIRED"
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun IconGroup(
    editing: Boolean,
    onToggleEdit: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onOpenSettings: () -> Unit,
    onPair: () -> Unit,
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, VnSurfaceHi, RoundedCornerShape(10.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onToggleEdit) {
            Icon(
                if (editing) Icons.Filled.Check else Icons.Filled.Edit,
                contentDescription = "Edit layout",
                tint = if (editing) VnAccent else Color.White,
            )
        }
        Divider()
        IconButton(onClick = onToggleFullscreen) {
            Icon(Icons.Filled.Fullscreen, contentDescription = "Fullscreen", tint = Color.White)
        }
        Divider()
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.GridView, contentDescription = "Pages", tint = Color.White)
        }
        Divider()
        IconButton(onClick = onPair) {
            Icon(Icons.Filled.QrCodeScanner, contentDescription = "Pair", tint = Color.White)
        }
    }
}

@Composable
private fun Divider() {
    Box(Modifier.width(1.dp).height(22.dp).background(VnSurfaceHi))
}

@Composable
private fun PageIndicator(pages: List<String>, current: Int) {
    Row(
        Modifier.fillMaxWidth().background(VnBg).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            pages.getOrElse(current) { "" }.uppercase(),
            color = VnTextDim,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(end = 10.dp),
        )
        pages.forEachIndexed { i, _ ->
            val on = i == current
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(if (on) 8.dp else 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (on) VnAccent else VnTextDim.copy(alpha = 0.4f))
            )
        }
    }
}

@Composable
private fun AddPadTile(modifier: Modifier, onAddPad: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .border(2.dp, VnTextDim.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .clickableNoRipple(onAddPad),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Add, contentDescription = "Add pad", tint = VnTextDim, modifier = Modifier.size(30.dp))
    }
}
