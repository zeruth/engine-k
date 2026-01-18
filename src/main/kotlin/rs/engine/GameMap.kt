package rs.engine

import rs.cache.config.LocType
import rs.cache.config.NpcType
import rs.cache.config.ObjType
import rs.engine.entity.EntityLifeCycle.RESPAWN
import rs.engine.entity.Loc
import rs.engine.entity.Npc
import rs.engine.entity.Obj
import rs.engine.zone.Zone
import rs.engine.zone.ZoneGrid
import rs.engine.zone.ZoneMap
import rs.io.Packet
import rsmod.LocAngle
import rsmod.LocLayer
import rsmod.PathFinder
import util.Logger
import java.io.File

class GameMap(val members: Boolean) {
    companion object {
        const val OPEN = 0x0
        const val BLOCK_MAP_SQUARE = 0x1
        const val LINK_BELOW = 0x2
        const val REMOVE_ROOFS = 0x4
        const val VISIBLE_BELOW = 0x8
        const val NOT_LOW_DETAIL = 0x10

        const val Y = 4
        const val X = 64
        const val Z = 64

        const val MAPSQUARE = X * Y * Z

        val rsmod = PathFinder
    }

    private val multimap = mutableSetOf<Int>()
    private val freemap = mutableSetOf<Int>()

    init {
        load(multimap, File("./data/maps/multiway.csv"))
        load(freemap, File("./data/maps/free2play.csv"))

        val path = "./data/pack/server/maps/"
        val file = File(path)
        val maps = file.listFiles()!!.filter { it.name.startsWith("m") }
        var totalZones = 0
        var totalNpcs = 0
        var totalObjs = 0
        var totalLocs = 0
        maps.forEachIndexed { index, file ->
            val coord = file.name.substring(1 until file.name.length).split("_")
            val mx = coord[0].toInt()
            val mz = coord[1].toInt()
            val mapsquareX = mx shl 6
            val mapsquareZ = mz shl 6

            totalNpcs += loadNPCs(Packet.load(File("${path}n${mx}_${mz}")), mapsquareX, mapsquareZ)
            totalObjs += loadObjs(Packet.load(File("${path}o${mx}_${mz}")), mapsquareX, mapsquareZ)

            //collision
            val lands = IntArray(MAPSQUARE) // 4 * 64 * 64 size is guaranteed for lands
            totalZones += loadGround(lands, Packet.load(File("${path}m${mx}_${mz}")), mapsquareX, mapsquareZ)
            totalLocs += loadLocs(lands, Packet.load(File("${path}l${mx}_${mz}")), mapsquareX, mapsquareZ)
        }
        Logger.messageColor = Logger.Color.GREEN
        Logger.info("GameMap", "Loaded $totalZones Zones")
        Logger.messageColor = Logger.Color.GREEN
        Logger.info("GameMap", "Loaded $totalLocs Locs")
        Logger.messageColor = Logger.Color.GREEN
        Logger.info("GameMap", "Loaded $totalNpcs Npcs")
        Logger.messageColor = Logger.Color.GREEN
        Logger.info("GameMap", "Loaded $totalObjs Objs")

    }

    fun load(map: MutableSet<Int>, file: File) {
        val lines = file.readLines()
        for (line in lines) {
            if (line.startsWith("//") || line.isBlank())
                continue

            val args = line.split("_").map { it.toInt() }
            val y = args[0]
            val mx = args[1]
            val mz = args[2]
            val lx = args[3]
            val lz = args[4]
            if (lx % 8 != 0 || lz % 8 != 0) {
                println("CSV map line is not aligned to a zone: $line")
            }
            map.add(ZoneMap.zoneIndex(((mx shl 6) + lx), (mz shl 6) + lz, y))
        }
    }

    fun loadNPCs(packet: Packet, mapsquareX: Int, mapsquareZ: Int) : Int {
        var total = 0
        while (packet.available() > 0) {
            val coord = unpackCoord(packet.g2())
            val x = coord.x
            val z = coord.z
            val level = coord.level

            val absoluteX = mapsquareX + x
            val absoluteZ = mapsquareZ + z

            val count = packet.g1()
            for (i in 0 until count) {
                val id = packet.g2()
                if (!members && !isFreeToPlay(absoluteX, absoluteZ)) {
                    continue
                }

                val npcType = NpcType.get(id)
                if (npcType == null) {
                    println("Invalid npc type ${id} in map m${mapsquareX shr 6}_${mapsquareZ shr 6}.jm2")
                    continue
                }

                val size = npcType.size
                val npc = Npc(level, absoluteX, absoluteZ, size, size, RESPAWN, World.getNextNid(), npcType.id, npcType.moverestrict, npcType.blockwalk)
                if (!npcType.members || members) {
                    World.addNpc(npc, -1)
                    total += 1
                }
            }
        }

        return total
    }

    fun loadObjs(packet: Packet, mapsquareX: Int, mapsquareZ: Int) : Int {
        var total = 0
        while (packet.available() > 0) {
            val coord = unpackCoord(packet.g2())
            val x = coord.x
            val z = coord.z
            val level = coord.level

            val absoluteX = mapsquareX + x
            val absoluteZ = mapsquareZ + z

            val count = packet.g1()
            for (i in 0 until count) {
                val id = packet.g2()
                val count = packet.g1()
                if (!members && !isFreeToPlay(absoluteX, absoluteZ)) {
                    continue
                }

                val objType = ObjType.get(id)!!
                val obj = Obj(level, absoluteX, absoluteZ, RESPAWN, objType.id, count)
                if (!objType.members || members) {
                    ZoneMap.zone(x, z, level).addStaticObj(obj)
                    total += 1
                }
            }
        }
        return total
    }

    fun loadGround(lands: IntArray, packet: Packet, mapsquareX: Int, mapsquareZ: Int) : Int {
        for (level in 0 until Y) {
            for (x in 0 until X) {
                for (z in 0 until Z) {
                    while (true) {
                        val opcode = packet.g1()
                        when (opcode) {
                            0 -> break
                            1 -> {
                                packet.move(1)
                                break
                            }
                            else -> {
                                if (opcode <= 49)
                                    packet.move(1)
                                else if (opcode <= 81) {
                                    lands[this.packCoord(x, z, level)] = (opcode - 49)
                                }
                            }
                        }
                    }
                }
            }
        }

        var i = 0

        for (level in 0 until Y) {
            for (x in 0 until X) {
                val absoluteX = x + mapsquareX

                for (z in 0 until Z) {
                    val absoluteZ = z + mapsquareZ

                    if (!this.members && !isFreeToPlay(absoluteX, absoluteZ) && !bordersFreeToPlay(absoluteX, absoluteZ)) {
                        continue
                    }

                    if (x % 7 == 0 && z % 7 == 0) {
                        rsmod.allocateIfAbsent(absoluteX, absoluteZ, level);
                        i += 1
                    }

                    val land = lands[packCoord(x, z, level)]

                    if (land and REMOVE_ROOFS != OPEN) {
                        changeRoofCollision(absoluteX, absoluteZ, level, true);
                    }

                    if (land and BLOCK_MAP_SQUARE != BLOCK_MAP_SQUARE) {
                        continue
                    }

                    val bridged: Boolean = if (level == 1) {
                        (land and LINK_BELOW) == LINK_BELOW
                    } else {
                        (lands[packCoord(x, z, 1)] and LINK_BELOW) == LINK_BELOW
                    }

                    val actualLevel = if (bridged) level - 1 else level

                    if (actualLevel < 0) {
                        continue
                    }

                    changeLandCollision(absoluteX, absoluteZ, actualLevel, true);
                }
            }
        }
        return i
    }

    fun loadLocs(lands: IntArray, packet: Packet, mapsquareX: Int, mapsquareZ: Int) : Int {
        var total = 0
        var locId = -1
        var locIdOffset = packet.gsmarts()

        while (locIdOffset != 0) {
            locId += locIdOffset

            var coord = 0
            var coordOffset = packet.gsmarts()

            while (coordOffset != 0) {
                coord += coordOffset - 1
                val position = unpackCoord(coord)
                val x = position.x
                val z = position.z
                val level = position.level

                val info = packet.g1()
                coordOffset = packet.gsmarts()

                val absoluteX = x + mapsquareX
                val absoluteZ = z + mapsquareZ

                if (!members && !isFreeToPlay(absoluteX, absoluteZ) && !bordersFreeToPlay(absoluteX, absoluteZ)) {
                    continue
                }

                val bridged: Boolean = if (level == 1) {
                    (lands[coord] and LINK_BELOW) == LINK_BELOW
                } else {
                    (lands[packCoord(x, z, 1)] and LINK_BELOW) == LINK_BELOW
                }

                val actualLevel = if (bridged) level - 1 else level

                if (actualLevel < 0) {
                    continue
                }

                val type = LocType.get(locId) ?: throw RuntimeException("Invalid loc type $locId")

                val width = type.width
                val length = type.length
                val shape = info shr 2
                val angle = info and 0x3

                if (type.blockwalk){
                    changeLocCollision(shape, angle, type.blockrange, length, width, type.active, absoluteX, absoluteZ, actualLevel, true);
                }

                ZoneMap.zone(x, z, level).addStaticLoc(Loc(actualLevel, absoluteX, absoluteZ, width, length, RESPAWN, locId, shape, angle))
                total += 1
            }
            locIdOffset = packet.gsmarts();
        }
        return total
    }

    fun getZone(x: Int, z: Int, level: Int) : Zone {
        return ZoneMap.zone(x, z, level)
    }

    fun getZoneGrid(level: Int) : ZoneGrid {
        return ZoneMap.grid(level)
    }

    fun isFreeToPlay(x: Int, z: Int): Boolean {
        return freemap.contains(ZoneMap.zoneIndex(x, z, 0))
    }

    fun bordersFreeToPlay(x: Int, z: Int): Boolean {
        return isFreeToPlay(x + 1, z) || isFreeToPlay(x - 1, z) || isFreeToPlay(x, z + 1) || isFreeToPlay(x, z - 1)
    }

    private fun unpackCoord(packed: Int): CoordGrid {
        val z = packed and 0x3F
        val x = (packed shr 6) and 0x3F
        val level = (packed shr 12) and 0x3
        return CoordGrid(x = x, z = z, level = level)
    }

    private fun packCoord(x: Int, z: Int, level: Int): Int {
        return (z and 0x3F) or
                ((x and 0x3F) shl 6) or
                ((level and 0x3) shl 12)
    }

    fun changeRoofCollision(x: Int, z: Int, level: Int, add: Boolean) {
        rsmod.changeRoof(x, z, level, add)
    }

    fun changeLandCollision(x: Int, z: Int, level: Int, add: Boolean) {
        rsmod.changeFloor(x, z, level, add)
    }

    private fun changeLocCollision(
        shape: Int,
        angle: Int,
        blockrange: Boolean,
        length: Int,
        width: Int,
        active: Int,
        x: Int,
        z: Int,
        level: Int,
        add: Boolean
    ) {
        val locLayer = rsmod.locShapeLayer(shape)
        if (locLayer == LocLayer.WALL) {
            rsmod.changeWall(x, z, level, angle, shape, blockrange, false, add)
        } else if (locLayer == LocLayer.GROUND) {
            if (angle == LocAngle.NORTH || angle == LocAngle.SOUTH) {
                rsmod.changeLoc(x, z, level, length, width, blockrange, false, add)
            } else {
                rsmod.changeLoc(x, z, level, width, length, blockrange, false, add)
            }
        } else if (locLayer == LocLayer.GROUND_DECOR) {
            if (active == 1) {
                rsmod.changeFloor(x, z, level, add)
            }
        }
    }
}