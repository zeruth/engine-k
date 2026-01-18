package rs.net.msg.out

import rs.net.msg.out.game.ServerGameMessage
import rs.net.msg.out.game.ServerGameProt.Companion.MESSAGE_GAME
import rs.net.msg.out.game.ServerGameProtPriority.IMMEDIATE

class MessageGame(private val message: String) :
    ServerGameMessage(IMMEDIATE, MESSAGE_GAME, {
        pjstr(message)
})