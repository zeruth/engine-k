package rs.engine.entity

import db.login.DBLoginAccountImpl
import rs.engine.World
import util.Base37.fromBase37
import util.Base37.toBase37

object PlayerLoading {
    fun load(account: DBLoginAccountImpl.DBAccount): Player {
        val hash64 = toBase37(account.username); // username or email.
        val name37 = toBase37(account.username); // always username.
        val safeName = fromBase37(name37); // always safe username.

        val player = World.newPlayer(safeName, name37, hash64)
        player.account_id = account.id
        player.staffModLevel = account.staffmodlevel
        player.members = account.members
        return player
    }
}