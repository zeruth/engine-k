package rs.net.msg.out

import rs.net.msg.out.game.ServerGameMessage
import rs.net.msg.out.game.ServerGameProt.Companion.VARP_LARGE
import rs.net.msg.out.game.ServerGameProtPriority.IMMEDIATE

class VarpLarge(
    private val varp: Int,
    private val value: Int,
) : ServerGameMessage(IMMEDIATE, VARP_LARGE, {
        p4_alt3(value)
        p2_alt1(varp)
})