package rs.net.msg.out

import rs.net.msg.out.game.ServerGameMessage
import rs.net.msg.out.game.ServerGameProt.Companion.IF_CLOSE
import rs.net.msg.out.game.ServerGameProtPriority.BUFFERED

object IfClose : ServerGameMessage(BUFFERED, IF_CLOSE)