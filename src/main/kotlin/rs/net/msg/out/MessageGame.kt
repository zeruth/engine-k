package rs.net.msg.out

import rs.engine.entity.Player
import rs.io.Packet
import rs.net.msg.out.game.ServerGameMessage
import rs.net.msg.out.game.ServerGameProt.Companion.MESSAGE_GAME
import rs.net.msg.out.game.ServerGameProtPriority.IMMEDIATE

class MessageGame(
    player: Player,
    private val message: String,
) : ServerGameMessage(MESSAGE_GAME, IMMEDIATE, player) {
    override fun encode(buf: Packet) {
        buf.pjstr(message)
    }
}