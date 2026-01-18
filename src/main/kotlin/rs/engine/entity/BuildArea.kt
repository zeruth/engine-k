package rs.engine.entity

import rs.engine.CoordGrid
import rs.engine.zone.ZoneMap
import rs.net.msg.out.RebuildNormal
import rs.net.msg.out.game.ServerGameMessage.Companion.write

class BuildArea(val player: Player) {
    val loadedZones = mutableSetOf<Int>()
    val activeZones = mutableSetOf<Int>()
    val mapsquares = mutableSetOf<Int>()

    var lastBuild = -1

    fun clear(reconnecting: Boolean) {
        if (!reconnecting) {
            loadedZones.clear()
            activeZones.clear()
            mapsquares.clear()
        }
    }

    fun rebuildZones() {
        activeZones.clear()

        val centerX = CoordGrid.zone(this.player.x)
        val centerZ = CoordGrid.zone(this.player.z)

        val originX = CoordGrid.zone(this.player.originX)
        val originZ = CoordGrid.zone(this.player.originZ)

        val leftX = originX - 6
        val rightX = originX + 6
        val topZ = originZ + 6
        val bottomZ = originZ - 6

        for (x in centerX - 3..centerX + 3) {
            for (z in centerZ - 3..centerZ + 3) {
                // check if the zone is within the build area
                if (x !in leftX..rightX || z > topZ || z < bottomZ) {
                    continue;
                }
                activeZones.add(ZoneMap.zoneIndex(x shl 3, z shl 3, player.level))
            }
        }
    }

    fun rebuildNormal(reconnect: Boolean = false) {
        val originX = CoordGrid.zone(this.player.originX)
        val originZ = CoordGrid.zone(this.player.originZ)

        val reloadLeftX = (originX - 4) shl 3
        val reloadRightX = (originX + 5) shl 3
        val reloadTopZ = (originZ + 5) shl 3
        val reloadBottomZ = (originZ - 4) shl 3

        // if the build area should be regenerated, do so now
        if (player.x < reloadLeftX || player.z < reloadBottomZ || player.x > reloadRightX - 1 || player.z > reloadTopZ - 1 || reconnect) {
            val zoneX = CoordGrid.zone(this.player.x)
            val zoneZ = CoordGrid.zone(this.player.z)

            mapsquares.clear()
            val minX = zoneX - 6
            val maxX = zoneX + 6
            val minZ = zoneZ - 6
            val maxZ = zoneZ + 6

            // build area is 13x13 zones (8*13 = 104 tiles), so we need to load 6 zones in each direction
            for (x in minX..maxX) {
                val mx = CoordGrid.mapsquare(x shl 3)
                for (z in minZ..maxZ) {
                    val mz = CoordGrid.mapsquare(z shl 3)
                    mapsquares.add((mx shl 8) or mz)
                }
            }

            RebuildNormal(zoneX, zoneZ, mapsquares).write(player)
        }
    }
}