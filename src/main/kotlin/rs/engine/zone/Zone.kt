package rs.engine.zone

import rs.engine.World
import rs.engine.entity.Loc
import rs.engine.entity.Npc
import rs.engine.entity.Obj
import rs.engine.entity.PathingEntity
import rs.engine.entity.Player
import rs.util.LinkList
import java.util.LinkedList

class Zone(val index: Int) {
    companion object {
        private const val SIZE = 8 * 8
        private const val LOCS = SIZE shl 2
        private const val OBJS = (SIZE shl 1) + 1
    }

    val x: Int
    val z: Int
    val level: Int

    private val players = LinkList<Player>()
    private val npcs = LinkList<Npc>()
    private val locs = LinkList<Loc>()
    private val objs = LinkList<Obj>()
    var playersCount = 0
    var npcsCount = 0
    var locsCount = 0
    var objsCount = 0

    init {
        val coord = ZoneMap.unpackIndex(index)
        x = coord.x shr 3
        z = coord.z shr 3
        level = coord.level
    }

    fun addStaticObj(obj: Obj) {
        objs.addTail(obj)
        objsCount += 1
        obj.isActive = true
    }

    fun addStaticLoc(loc: Loc) {
        locs.addTail(loc)
        locsCount += 1
        loc.isActive = true
    }

    fun enter(entity: PathingEntity) {
        if (entity is Player) {
            players.addTail(entity)
            playersCount += 1
            World.gameMap.getZoneGrid(level).flag(x, z)
        } else if (entity is Npc) {
            npcs.addTail(entity)
            npcsCount += 1
        }
    }
}