package rs.net.msg.out

import rs.net.msg.out.game.ServerGameMessage
import rs.net.msg.out.game.ServerGameProt.Companion.UPDATE_IGNORELIST
import rs.net.msg.out.game.ServerGameProtPriority.BUFFERED

class UpdateIgnoreList(val names: Array<Long>) :
    ServerGameMessage(BUFFERED, UPDATE_IGNORELIST, encode = {
        for (name in names) {
            p8(name)
        }
})