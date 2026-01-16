package rs.engine.zone

import rs.engine.CoordGrid

object ZoneMap {
    val zones = HashMap<Int, Zone>()
    val grids = HashMap<Int, ZoneGrid>()

    fun zoneIndex(x: Int, z: Int, level: Int): Int {
        return ((x shr 3) and 0x7FF) or
                (((z shr 3) and 0x7FF) shl 11) or
                ((level and 0x3) shl 22)
    }

    fun unpackIndex(index: Int): CoordGrid {
        val x = (index and 0x7FF) shl 3
        val z = ((index shr 11) and 0x7FF) shl 3
        val level = index shr 22
        return CoordGrid(x, z, level)
    }

    fun zone(x: Int, z: Int, level: Int) = zoneByIndex(zoneIndex(x, z, level))

    fun zoneByIndex(index: Int): Zone {
        var zone = zones[index]
        if (zone == null){
            zone = Zone(index)
            zones[index] = zone
        }
        return zone
    }

    fun grid(level: Int): ZoneGrid {
        var grid = grids[level]
        if (grid == null){
            grid = ZoneGrid()
            grids[level] = grid
        }
        return grid
    }

    fun zoneCount() = zones.size

    fun locCount(): Int {
        var total = 0
        for (zone in zones.values)
            total += zone.locsCount
        return total
    }

    fun objCount(): Int {
        var total = 0
        for (zone in zones.values)
            total += zone.objsCount
        return total
    }
}