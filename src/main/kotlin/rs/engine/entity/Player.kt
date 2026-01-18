package rs.engine.entity

import rs.net.Client
import rs.net.prot.PlayerInfoProt
import rs.cache.config.InvType
import rs.cache.config.ScriptVarType
import rs.cache.config.VarPlayerType
import rs.engine.World
import rs.engine.game.Inventory
import rs.engine.game.InventoryListener
import rs.engine.game.ModalState
import rs.engine.script.RuneScriptProvider
import rs.engine.script.RuneScriptRunner
import rs.engine.script.ScriptPointer
import rs.engine.script.ScriptState
import rs.engine.script.ServerTriggerType
import rs.net.msg.out.ChatFilterSettings
import rs.net.msg.out.FriendsListLoaded
import rs.net.msg.out.IfClose
import rs.net.msg.out.ResetAnims
import rs.net.msg.out.ResetClientVarCache
import rs.net.msg.out.UpdateIgnoreList
import rs.net.msg.out.UpdatePid
import rs.net.msg.out.VarpLarge
import rs.net.msg.out.VarpSmall
import rs.net.msg.out.game.ServerGameMessage
import rs.net.msg.out.game.ServerGameMessage.Companion.write
import rs.net.msg.out.game.ServerGameProtPriority
import rs.util.LinkList
import java.math.BigInteger
import kotlin.math.pow

class Player(val safeName: String, val name37: BigInteger, val hash64: BigInteger) : PathingEntity(0, 3094, 3106, 1, 1, EntityLifeCycle.FOREVER, MoveRestrict.NORMAL, BlockWalk.NPC, MoveStrategy.SMART, PlayerInfoProt.FACE_COORD, PlayerInfoProt.FACE_ENTITY){
    var moveClickRequest = false
    var tele = false
    var members = false
    var account_id = -1
    var invs: HashMap<Int, Inventory> = HashMap()
    var modalState: ModalState = ModalState.NONE
    var modalMain = -1
    var lastModalMain = -1
    var modalChat = -1
    var lastModalChat = -1
    var modalSide = -1
    var lastModalSide = -1
    var modalTutorial = -1
    var overlay = -1
    var lastOverlay = -1
    var refreshModal = false
    var refreshModalClose = false
    var requestModalClose = false
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
    var stats = IntArray(21)
    var levels = ByteArray(21)
    var baseLevels = ByteArray(21)

    var client: Client? = null

    var originX = -1
    var originZ = -1

    val buildArea = BuildArea(this)

    var publicChat = ChatModes.ChatModePublic.ON
    var privateChat = ChatModes.ChatModePrivate.ON
    var tradeDuel = ChatModes.ChatModeTradeDuel.ON

    var activeScript: ScriptState? = null

    val weakQueue: LinkList<PlayerQueueRequest> = LinkList()

    var invListeners = emptyArray<InventoryListener>()

    var lastStepX = -1
    var lastStepZ = -1

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
                writeVarp(id, value);
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

    fun onLogin() {
        // confirmed order:
        // - rebuild_normal
        // - chat_filter_settings
        // - varp_reset
        // - varps
        // - invs
        // - interfaces
        // - stats
        // - runweight
        // - runenergy
        // - reset anims
        // - social

        buildArea.rebuildNormal()
        ChatFilterSettings(publicChat, privateChat, tradeDuel).write(this)
        // todo: exact order
/*        if (Environment.FRIEND_SERVER) {
            this.write(new FriendlistLoaded(1));
        } else {
            this.write(new FriendlistLoaded(2));
            this.write(new UpdateIgnoreList([]));
        }*/
        FriendsListLoaded(2).write(this)
        UpdateIgnoreList(emptyArray()).write(this)
        //TODO
        //IfClose.write(this)
        UpdatePid(uid, members).write(this)
        //TODO
        //ResetClientVarCache.write(this)
        for (varp in 0 until vars.size) {
            val type = VarPlayerType.get(varp) ?: throw RuntimeException("VarPlayerType not found: $varp")
            val value = vars[varp]

            if (type.transmit)
                writeVarp(varp, value)
        }
        //TODO
        //ResetAnims.write(this)
        val loginTrigger = RuneScriptProvider.getByTriggerSpecific(ServerTriggerType.LOGIN)
        loginTrigger?.let {
            executeScript(RuneScriptRunner.init(it, this), true)
        }
        lastStepX = this.x - 1
        lastStepZ = this.z
        isActive = true
    }

    fun executeScript(script: ScriptState, protect: Boolean = false, force: Boolean = false) {
        val state = runScript(script, protect, force)
        if (state == -1) return

        if (state != ScriptState.FINISHED) {
            if (state == ScriptState.WORLD_SUSPENDED) {
                //TODO
                //World.enqueueScript(script, script.popInt());
            } else if (state == ScriptState.NPC_SUSPENDED) {
                //TODO
                //script.activeNpc.activeScript = script
            } else {
                script.activePlayer.activeScript = script;
                script.activePlayer.protect = protect // preserve protected access when delayed
            }
        } else if (script == activeScript) {
            activeScript = null

            if (modalState.mask and ModalState.MAIN.mask == ModalState.NONE.mask) {
                // close chat dialogues automatically and leave main modals alone
                this.closeModal(false)
            }
        }
    }

    fun runScript(script: ScriptState, protect: Boolean = false, force: Boolean = false): Int {
        //TODO
/*        if (!force && (protect || delayed)) {
            return -1
        }*/

        if (protect) {
            script.pointerAdd(ScriptPointer.ProtectedActivePlayer)
            this.protect = true
        }

        val state = RuneScriptRunner.execute(script)

        if (protect) {
            this.protect = false
        }

        if (script.pointerGet(ScriptPointer.ProtectedActivePlayer.ordinal) && script._activePlayer != null) {
            script.pointerRemove(ScriptPointer.ProtectedActivePlayer.ordinal)
            script._activePlayer!!.protect = false
        }

        if (script.pointerGet(ScriptPointer.ProtectedActivePlayer2.ordinal) && script._activePlayer2 != null) {
            script.pointerRemove(ScriptPointer.ProtectedActivePlayer2.ordinal)
            script._activePlayer2!!.protect = false
        }

        return state
    }

    fun closeModal(clearWeakQueue: Boolean = true) {
        if (clearWeakQueue) {
            weakQueue.clear()
        }
        if (delayed) {
            this.protect = false
        }

        if (modalState == ModalState.NONE) {
            return
        }

        modalState = ModalState.NONE

        // close any input dialogue suspended scripts.
        if (activeScript?.execution == ScriptState.COUNTDIALOG || activeScript?.execution == ScriptState.PAUSEBUTTON) {
            activeScript = null
        }

        // close any main viewport interface
        if (this.modalMain != -1) {
            val closeTrigger = RuneScriptProvider.getByTrigger(ServerTriggerType.IF_CLOSE, this.modalMain);
            if (closeTrigger != null) {
                this.executeScript(RuneScriptRunner.init(closeTrigger, this), false);
            }
            //TODO
            //this.clearComListeners(this.modalMain);
            this.modalMain = -1;
        }

        // close any chatbox interface
        if (this.modalChat != -1) {
            val closeTrigger = RuneScriptProvider.getByTrigger(ServerTriggerType.IF_CLOSE, this.modalChat);
            if (closeTrigger != null) {
                this.executeScript(RuneScriptRunner.init(closeTrigger, this), false);
            }

            //TODO
            //this.clearComListeners(this.modalChat);
            this.modalChat = -1;
        }

        // close any sidebar tabs interface
        if (this.modalSide != -1) {
            val closeTrigger = RuneScriptProvider.getByTrigger(ServerTriggerType.IF_CLOSE, this.modalSide);
            if (closeTrigger != null) {
                this.executeScript(RuneScriptRunner.init(closeTrigger, this), false);
            }

            //TODO
            //this.clearComListeners(this.modalSide);
            this.modalSide = -1;
        }

        this.refreshModalClose = true;
    }

    //TODO
/*    fun clearComListeners(root: Int) {
        if (root == -1) return

        for (i in 0 until invListeners.size) {
            val com = invListeners[i]
            if (Component)
        }
    }*/

    companion object {
        val levelExperience = IntArray(99)

        init {
            var acc = 0.0
            for (i in 0 until 99) {
                val level = i + 1
                val delta = (level + 2.0.pow(level / 7.0) * 300.0).toInt()
                acc += delta
                levelExperience[i] = ((acc / 4).toInt()) * 10
            }
        }

        fun Player.writeVarp(id: Int, value: Int) {
            if (value in -128..127) {
                write(VarpSmall(id, value))
            } else {
                write(VarpLarge(id, value))
            }
        }

        fun dummy() : Player {
            return Player("", BigInteger("0"), BigInteger("0"))
        }
    }

    fun getLevelByExp(exp: Int): Int {
        for (i in 98 downTo 0) {
            if (exp >= levelExperience[i]) {
                return minOf(i + 2, 99)
            }
        }
        return 1
    }

    fun getExpByLevel(level: Int): Int {
        return if (level <= 1) 0 else levelExperience[level - 2]
    }
}