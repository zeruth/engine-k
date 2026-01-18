package rs.net.msg.out

import rs.net.msg.out.game.ServerGameMessage
import rs.net.msg.out.game.ServerGameProt.Companion.CHAT_FILTER_SETTINGS
import rs.net.msg.out.game.ServerGameProt.Companion.REBUILD_NORMAL
import rs.net.msg.out.game.ServerGameProtPriority.IMMEDIATE

class ChatFilterSettings(val publicChat: Int, val privateChat: Int, val tradeDuel: Int) :
    ServerGameMessage(IMMEDIATE, CHAT_FILTER_SETTINGS, encode = {
        p1(publicChat)
        p1(privateChat)
        p1(tradeDuel)
})