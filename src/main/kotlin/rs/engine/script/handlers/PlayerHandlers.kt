package rs.engine.script.handlers

import rs.engine.World
import rs.engine.entity.Player.Companion.MessageGame
import rs.engine.script.RuneScriptOpcode
import rs.engine.script.RuneScriptOpcodeHandler
import rs.engine.script.ScriptPointer
import rs.engine.script.ScriptPointer.Companion.ActivePlayers
import rs.engine.script.ScriptPointer.Companion.ProtectedActivePlayers

object BasReadyAnimHandler : RuneScriptOpcodeHandler(RuneScriptOpcode.BAS_READYANIM, ScriptPointer.ActivePlayer, {
        val value = popInt()
        activePlayer.basReadyAnim = value
})

object BasTurnOnSpotHandler : RuneScriptOpcodeHandler(RuneScriptOpcode.BAS_TURNONSPOT, ScriptPointer.ActivePlayer, {
        val value = popInt()
        activePlayer.basTurnOnSpot = value
})

object BasWalkBHandler : RuneScriptOpcodeHandler(RuneScriptOpcode.BAS_WALK_B, ScriptPointer.ActivePlayer, {
        val value = popInt()
        activePlayer.basWalkBackward = value
})

object BasWalkFHandler : RuneScriptOpcodeHandler(RuneScriptOpcode.BAS_WALK_F, ScriptPointer.ActivePlayer,
    {
        val value = popInt()
        activePlayer.basWalkForward = value
    }
)

object MesHandler : RuneScriptOpcodeHandler(RuneScriptOpcode.MES, ScriptPointer.ActivePlayer,
    {
        val message = popString()
        activePlayer.MessageGame(message)
    }
)

object PAnimProtectHandler : RuneScriptOpcodeHandler(RuneScriptOpcode.P_ANIMPROTECT, ScriptPointer.ActivePlayer,
    {
        val value = popInt()
        check(value > -1)
        activePlayer.animProtect = value
    }
)

object PFindUidHandler : RuneScriptOpcodeHandler(RuneScriptOpcode.P_FINDUID, ScriptPointer.ActivePlayer,
    op@{
        val uid = popInt() shr 0
        val player = World.getPlayerByUid(uid)

        if (pointerGet(ScriptPointer.ProtectedActivePlayers[intOperand()].ordinal) && activePlayer.uid == uid) {
            // script is already running on this player with protected access, no-op
            pushInt(1)
            return@op
        }

        if (player == null || !player.canAccess()) {
            pushInt(0)
            return@op
        }

        activePlayer(player)
        pointerAdd(ActivePlayers[intOperand()]);
        pointerAdd(ProtectedActivePlayers[intOperand()]);
        pushInt(1)
    }
)

object StaffModLevelHandler : RuneScriptOpcodeHandler(RuneScriptOpcode.STAFFMODLEVEL, ScriptPointer.ActivePlayer,
    {
        pushInt(activePlayer.staffModLevel)
    }
)

object UidHandler : RuneScriptOpcodeHandler(RuneScriptOpcode.UID, ScriptPointer.ActivePlayer,
    {
        pushInt(activePlayer.uid)
    }
)