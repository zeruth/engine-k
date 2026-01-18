package rs.engine.zone

// https://gist.github.com/Z-Kris/90e687fd1502ed095804393f550ebfcc
class ZoneGrid {
    companion object {
        const val GRID_SIZE = 2048
        const val INT_BITS = 5
        const val INT_BITS_FLAG = (1 shl INT_BITS) - 1
        const val DEFAULT_GRID_SIZE = GRID_SIZE * (GRID_SIZE shr INT_BITS)
    }

    val grid: IntArray = IntArray(DEFAULT_GRID_SIZE)

    private fun index(zoneX: Int, zoneY: Int): Int {
        return (zoneX shl INT_BITS) or (zoneY ushr INT_BITS)
    }

    fun flag(zoneX: Int, zoneY: Int) {
        val i = index(zoneX, zoneY)
        grid[i] = grid[i] or (1 shl (zoneY and INT_BITS_FLAG))
    }

    fun unflag(zoneX: Int, zoneY: Int) {
        val i = index(zoneX, zoneY)
        grid[i] = grid[i] and (1 shl (zoneY and INT_BITS_FLAG)).inv()
    }
}