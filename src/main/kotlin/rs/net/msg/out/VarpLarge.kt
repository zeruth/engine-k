package rs.net.msg.out

import rs.engine.entity.Player
import rs.net.msg.out.game.ServerGameMessage
import rs.net.msg.out.game.ServerGameProt.Companion.VARP_LARGE
import rs.net.msg.out.game.ServerGameProtPriority.IMMEDIATE

class VarpLarge(
    player: Player,
    private val varp: Int,
    private val value: Int,
) : ServerGameMessage(IMMEDIATE, VARP_LARGE, player, {
        p4_alt3(value)
        p2_alt1(varp)
})