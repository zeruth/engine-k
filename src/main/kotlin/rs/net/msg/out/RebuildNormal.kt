package rs.net.msg.out

import rs.net.msg.out.game.ServerGameMessage
import rs.net.msg.out.game.ServerGameProt.Companion.REBUILD_NORMAL
import rs.net.msg.out.game.ServerGameProtPriority.IMMEDIATE

class RebuildNormal(val zoneX: Int, val zoneZ: Int, val mapsquares: MutableSet<Int>) :
    ServerGameMessage(IMMEDIATE, REBUILD_NORMAL, encode = {
        p2(zoneZ)
        p2_alt3(zoneX)
})