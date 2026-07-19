package com.vnstudio.vnpad.ui

/** Where an item sits in the grid, in cell units. */
data class Placement(val index: Int, val col: Int, val row: Int, val spanX: Int, val spanY: Int)

data class PackResult(val placements: List<Placement>, val cols: Int, val rows: Int)

/**
 * First-fit packing: walk the items in order and drop each into the first free
 * spot (scanning row-major) big enough for its span — the same auto-placement
 * rule CSS grid uses. Spans wider than [cols] are clamped so nothing is lost.
 */
fun packGrid(spans: List<Pair<Int, Int>>, cols: Int): PackResult {
    if (cols < 1) return PackResult(emptyList(), 1, 0)

    val occupancy = mutableListOf<BooleanArray>()
    fun row(r: Int): BooleanArray {
        while (occupancy.size <= r) occupancy.add(BooleanArray(cols))
        return occupancy[r]
    }
    fun fits(r: Int, c: Int, spanX: Int, spanY: Int): Boolean {
        if (c + spanX > cols) return false
        for (rr in r until r + spanY) {
            val line = row(rr)
            for (cc in c until c + spanX) if (line[cc]) return false
        }
        return true
    }

    val placements = ArrayList<Placement>(spans.size)
    spans.forEachIndexed { index, (rawX, rawY) ->
        val spanX = rawX.coerceIn(1, cols)
        val spanY = rawY.coerceAtLeast(1)
        var r = 0
        search@ while (true) {
            for (c in 0 until cols) {
                if (fits(r, c, spanX, spanY)) {
                    for (rr in r until r + spanY) {
                        val line = row(rr)
                        for (cc in c until c + spanX) line[cc] = true
                    }
                    placements.add(Placement(index, c, r, spanX, spanY))
                    break@search
                }
            }
            r++
        }
    }

    val rows = placements.maxOfOrNull { it.row + it.spanY } ?: 0
    return PackResult(placements, cols, rows)
}
