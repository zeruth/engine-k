package rs.net.msg.out

import rs.net.msg.out.game.ServerGameMessage
import rs.net.msg.out.game.ServerGameProt.Companion.FRIENDLIST_LOADED
import rs.net.msg.out.game.ServerGameProtPriority.BUFFERED

class FriendsListLoaded(val status: Int) :
    ServerGameMessage(BUFFERED, FRIENDLIST_LOADED, encode = {
        // 0 loading friend list
        // 1 connecting to friendserver
        // 2 online
        // else Please wait...
        p1(status)
})