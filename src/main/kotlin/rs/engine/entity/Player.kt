package rs.engine.entity

import rs.net.Client
import rs.net.prot.PlayerInfoProt
import rs.cache.config.InvType
import rs.cache.config.ScriptVarType
import rs.cache.config.VarPlayerType
import rs.engine.World
import rs.engine.game.Inventory
import rs.engine.game.ModalState
import rs.net.msg.out.MessageGame
import rs.net.msg.out.VarpLarge
import rs.net.msg.out.VarpSmall
import rs.net.msg.out.game.ServerGameMessage
import rs.net.msg.out.game.ServerGameProtPriority

class Player : PathingEntity(0, 3094, 3106, 1, 1, EntityLifeCycle.FOREVER, MoveRestrict.NORMAL, BlockWalk.NPC, MoveStrategy.SMART, PlayerInfoProt.FACE_COORD, PlayerInfoProt.FACE_ENTITY){
    var invs: HashMap<Int, Inventory> = HashMap()
    var modalState: ModalState = ModalState.NONE
    var delayed: Boolean = false
    var protect: Boolean = false
    var animProtect: Int = 0
    var basReadyAnim: Int = -1
    var basTurnOnSpot: Int = -1
    var basWalkForward: Int = -1
    var basWalkBackward: Int = -1
    var varsString: Array<String?> = arrayOfNulls(VarPlayerType.count())
    var vars: IntArray = IntArray(VarPlayerType.count())
    var buffer = ArrayList<ServerGameMessage?>()
    var pid = -1
    var uid = -1
    var staffModLevel = 0

    var client: Client? = null

    fun invTotal(inv: Int, obj: Int) : Int {
        val container = getInventory(inv) ?: throw RuntimeException("invGetSlot: Invalid inventory type: $inv")
        return container.getItemCount(obj)
    }

    fun invGetSlot(inv: Int, slot: Int) : Int {
        val container = getInventory(inv) ?: throw RuntimeException("invGetSlot: Invalid inventory type: $inv")

        if (!container.validSlot(slot)) {
            throw RuntimeException("invGetSlot: Invalid slot: $slot of max ${container.capacity}");
        }

        return container.get(slot)?.id ?: -1
    }

    fun getInventory(inv: Int) : Inventory? {
        if (inv == -1) return null;

        val invType = InvType.get(inv)
        var container: Inventory?

        if (invType == null) return null;

        if (invType.scope == InvType.SCOPE_SHARED) {
            container = World.getInventory(inv)
        } else {
            container = invs[inv]

            if (container == null) {
                container = Inventory.fromType(inv)
            }
        }

        return container
    }

    fun canAccess(): Boolean {
        // once the world has gone past shutting down, no protection rules apply
        if (World.shutdown)
            return true

        return !protect && !busy();
    }

    fun busy(): Boolean {
        return delayed || containsModalInterface()
    }

    fun containsModalInterface(): Boolean {
        // main or chat is open
        return (this.modalState.mask and (ModalState.MAIN.mask or ModalState.CHAT.mask)) != ModalState.NONE.mask
    }

    fun setVar(id: Int, value: Any) {
        val varp = VarPlayerType.get(id) ?: throw RuntimeException("VarPlayerType not found: $id")

        if (varp.type == ScriptVarType.STRING && value is String) {
            varsString[varp.id] = value
        } else if (value is Int) {
            vars[varp.id] = value;

            if (varp.transmit) {
                this.writeVarp(id, value);
            }
        } else {
            throw RuntimeException("VarPlayerType set value not valid: $value")
        }
    }

    fun isConnected() : Boolean {
        return false
        //return client.channel.isActive
    }

    fun writeInner(message: ServerGameMessage) {
        //client.channel.pipeline().write(message)
        //TODO: Track metrics
    }

    fun write(message: ServerGameMessage) {
        if (!isConnected()) {
            return
        }

        if (message.priority == ServerGameProtPriority.IMMEDIATE) {
            writeInner(message)
        } else {
            buffer.add(message)
        }
    }

    fun writeVarp(id: Int, value: Int) {
        if (value in -128..127) {
            write(VarpSmall(id, value))
        } else {
            write(VarpLarge(id, value))
        }
    }

    fun messageGame(message: String) {
        write(MessageGame(message))
    }

    companion object {
        fun Player.VarpLarge(id: Int, value: Int) : VarpLarge {
            return VarpLarge(this, id, value)
        }

        fun Player.VarpSmall(id: Int, value: Int) : VarpSmall {
            return VarpSmall(this, id, value)
        }

        fun Player.MessageGame(message: String) : MessageGame {
            return MessageGame(this, message)
        }
    }
}