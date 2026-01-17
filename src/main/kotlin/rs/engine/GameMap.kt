package rs.engine

import rs.cache.config.NpcType
import rs.cache.config.ObjType
import rs.engine.entity.EntityLifeCycle.RESPAWN
import rs.engine.entity.Npc
import rs.engine.entity.Obj
import rs.engine.zone.Zone
import rs.engine.zone.ZoneMap
import rs.io.Packet
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
    }

    private val multimap = mutableSetOf<Int>()
    private val freemap = mutableSetOf<Int>()

    init {
        load(multimap, File("./data/maps/multiway.csv"))
        load(freemap, File("./data/maps/free2play.csv"))

        val dir = "./data/pack/server/maps/"
        val file = File(dir)
        val maps = file.listFiles()!!.filter { it.name.startsWith("m") }
        var totalNpcs = 0
        var totalObjs = 0
        maps.forEachIndexed { index, file ->
            val coord = file.name.substring(1 until file.name.length).split("_")
            val mx = coord[0].toInt()
            val mz = coord[1].toInt()
            val mapsquareX = mx shl 6
            val mapsquareZ = mz shl 6

            totalNpcs += loadNPCs(Packet.load(File("${dir}n${mx}_${mz}")), mapsquareX, mapsquareZ)
            totalObjs += loadObjs(Packet.load(File("${dir}o${mx}_${mz}")), mapsquareX, mapsquareZ)

            //collision
            val lands = IntArray(MAPSQUARE)
        }
        Logger.messageColor = Logger.Color.GREEN
        Logger.info("World", "Loaded $totalNpcs Npc spawns")
        Logger.messageColor = Logger.Color.GREEN
        Logger.info("World", "Loaded $totalObjs Obj spawns")
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

    fun loadGround(lands: ByteArray, packet: Packet, mapsquareX: Int, mapsquareZ: Int) {
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
                                    lands[this.packCoord(x, z, level)] = (opcode - 49).toByte()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun isFreeToPlay(x: Int, z: Int): Boolean {
        return freemap.contains(ZoneMap.zoneIndex(x, z, 0))
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

}