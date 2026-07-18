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
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

import kotlin.math.ceil

private val GAP = 2.dp

@Composable
fun PadGridScreen(
    pads: List<Pad>,
    settings: GridSettings,
    status: ConnStatus,
    editing: Boolean,
    onToggleEdit: () -> Unit,
    onOpenSettings: () -> Unit,
    onPair: () -> Unit,
    onTapPad: (Pad) -> Unit,
    onEditPad: (Pad) -> Unit,
    onDeletePad: (Pad) -> Unit,
    onAddPad: (page: Int) -> Unit,
) {
    val feedback = rememberPadFeedback()
    val pageCount = settings.pages.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Column(Modifier.fillMaxSize()) {
        TopBar(status = status, editing = editing, onToggleEdit = onToggleEdit, onOpenSettings = onOpenSettings, onPair = onPair)
        PageIndicator(pages = settings.pages, current = pagerState.currentPage)

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            AutoFitPadGrid(
                pads = pads.filter { it.page == page },
                editing = editing,
                onTapPad = onTapPad,
                onEditPad = onEditPad,
                onDeletePad = onDeletePad,
                onAddPad = { onAddPad(page) },
                onPressFeedback = { feedback.tap() },
            )
        }
    }
}

/**
 * Lays the page's pads out to fill the screen: picks the column count that
 * maximises square cell size for the current width×height, so the board looks
 * full and re-adapts on rotation. An add tile trails the pads in edit mode.
 */
@Composable
private fun AutoFitPadGrid(
    pads: List<Pad>,
    editing: Boolean,
    onTapPad: (Pad) -> Unit,
    onEditPad: (Pad) -> Unit,
    onDeletePad: (Pad) -> Unit,
    onAddPad: () -> Unit,
    onPressFeedback: () -> Unit,
) {
    val count = pads.size + if (editing) 1 else 0

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().padding(GAP),
        contentAlignment = Alignment.Center,
    ) {
        if (count == 0) {
            Text("Tap ✏ then + to add a pad", color = VnTextDim, fontSize = 13.sp)
            return@BoxWithConstraints
        }

        // Try every column count; keep the one giving the biggest square cell.
        var nCols = 1
        var best = 0.dp
        for (c in 1..count) {
            val r = ceil(count / c.toFloat()).toInt()
            val cw = (maxWidth - GAP * (c - 1)) / c
            val ch = (maxHeight - GAP * (r - 1)) / r
            val candidate = minOf(cw, ch)
            if (candidate > best) { best = candidate; nCols = c }
        }
        val cell = best
        val nRows = ceil(count / nCols.toFloat()).toInt()

        Column(verticalArrangement = Arrangement.spacedBy(GAP)) {
            for (r in 0 until nRows) {
                Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
                    for (c in 0 until nCols) {
                        val i = r * nCols + c
                        if (i >= count) break
                        val slot = Modifier.size(cell)
                        if (i < pads.size) {
                            PadButton(
                                pad = pads[i],
                                editing = editing,
                                onTap = { onTapPad(pads[i]) },
                                onLongPress = { onEditPad(pads[i]) },
                                onDelete = { onDeletePad(pads[i]) },
                                onPressFeedback = onPressFeedback,
                                modifier = slot,
                            )
                        } else {
                            AddPadTile(slot, onAddPad)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    status: ConnStatus,
    editing: Boolean,
    onToggleEdit: () -> Unit,
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
        IconGroup(editing = editing, onToggleEdit = onToggleEdit, onOpenSettings = onOpenSettings, onPair = onPair)
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
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.GridView, contentDescription = "Grid & pages", tint = Color.White)
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
