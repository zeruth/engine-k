package rs.engine

import rs.Environment
import rs.engine.entity.Player
import rs.engine.game.Inventory

object World {
    val PLAYERS = Environment.NODE_MAX_PLAYERS
    val NPCS = Environment.NODE_MAX_NPCS

    var invs: HashMap<Int, Inventory> = HashMap()
    var shutdown: Boolean = false

    var currentTick: Int = 0 // the current tick of the game world.

    val players = arrayOfNulls<Player>(PLAYERS)

    fun getInventory(inv: Int) : Inventory? {
        if (inv == -1) return null;

        for (inventory in invs.values) {
            if (inventory.type == inv)
                return inventory;
        }

        val invetory = Inventory.fromType(inv)
        invs[inv] = invetory

        return invetory
    }

    fun getPlayerByUid(uid: Int) = players.firstOrNull { it?.uid == uid }
}