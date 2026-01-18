package rs.engine

data class CoordGrid(
    val x: Int,
    val z: Int,
    val level: Int
) {
    companion object {
        fun zone(pos: Int) = pos shr 3
        fun zoneCenter(pos: Int) = zone(pos) - 6
        fun zoneOrigin(pos: Int) = zoneCenter(pos) shl 3
        fun mapsquare(pos: Int) = pos shr 6
        fun local(pos: Int, origin: Int) = pos - zoneOrigin(origin)
    }
}