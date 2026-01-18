package rs.net.msg.out

import rs.net.msg.out.game.ServerGameMessage
import rs.net.msg.out.game.ServerGameProt.Companion.RESET_CLIENT_VARCACHE
import rs.net.msg.out.game.ServerGameProtPriority.IMMEDIATE

object ResetClientVarCache : ServerGameMessage(IMMEDIATE, RESET_CLIENT_VARCACHE)