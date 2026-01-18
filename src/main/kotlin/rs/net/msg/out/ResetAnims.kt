package rs.net.msg.out

import rs.net.msg.out.game.ServerGameMessage
import rs.net.msg.out.game.ServerGameProt.Companion.RESET_ANIMS
import rs.net.msg.out.game.ServerGameProtPriority.IMMEDIATE

// todo: what should priority be?
object ResetAnims : ServerGameMessage(IMMEDIATE, RESET_ANIMS)