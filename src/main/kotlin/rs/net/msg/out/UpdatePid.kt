package rs.net.msg.out

import rs.net.msg.out.game.ServerGameMessage
import rs.net.msg.out.game.ServerGameProt.Companion.UPDATE_PID
import rs.net.msg.out.game.ServerGameProtPriority.IMMEDIATE

class UpdatePid(private val uid: Int, val members: Boolean) :
    ServerGameMessage(IMMEDIATE, UPDATE_PID, {
        pbool(members)
        p2_alt1(uid)
})