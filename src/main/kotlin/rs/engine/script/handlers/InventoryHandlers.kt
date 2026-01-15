package rs.engine.script.handlers

import rs.cache.config.InvType
import rs.engine.script.RuneScriptOpcode
import rs.engine.script.RuneScriptOpcodeHandler
import rs.engine.script.ScriptPointer

object InvGetObjHandler : RuneScriptOpcodeHandler(RuneScriptOpcode.INV_GETOBJ, ScriptPointer.ActivePlayer,
    {
        val ints = popInts(2)
        val inv = ints[0]
        val slot = ints[1]

        pushInt(activePlayer.invGetSlot(inv, slot))
    }
)

object InvTotalHandler : RuneScriptOpcodeHandler(RuneScriptOpcode.INV_TOTAL, ScriptPointer.ActivePlayer,
    {
        val ints = popInts(2)
        val inv = ints[0]
        val obj = ints[1]

        InvType.get(inv) ?: throw RuntimeException("Unknown invType $inv")

        pushInt(activePlayer.invTotal(inv, obj))
    }
)