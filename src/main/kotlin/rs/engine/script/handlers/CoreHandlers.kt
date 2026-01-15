package rs.engine.script.handlers

import rs.cache.config.ScriptVarType
import rs.cache.config.VarPlayerType
import rs.engine.script.RuneScriptOpcode
import rs.engine.script.RuneScriptOpcodeHandler
import rs.engine.script.RuneScriptProvider
import rs.engine.script.ScriptState

object BranchEqualsHandler : RuneScriptOpcodeHandler(RuneScriptOpcode.BRANCH_EQUALS,
    op = {
        val b = popInt()
        val a = popInt()

        if (a == b) {
            pc += intOperand()
        }
    }
)

object BranchGreaterThanHandler : RuneScriptOpcodeHandler(RuneScriptOpcode.BRANCH_GREATER_THAN,
    op = {
        val b = popInt()
        val a = popInt()
        if (a > b) {
            pc += intOperand()
        }
    }
)

object BranchGreaterThanOrEqualsHandler : RuneScriptOpcodeHandler(RuneScriptOpcode.BRANCH_GREATER_THAN_OR_EQUALS,
    op = {
        val b = popInt()
        val a = popInt()
        if (a >= b) {
            pc += intOperand()
        }
    }
)

object BranchHandler : RuneScriptOpcodeHandler(RuneScriptOpcode.BRANCH,
    op = {
        pc += intOperand()
    }
)

object GoSubWithParamsHandler : RuneScriptOpcodeHandler(RuneScriptOpcode.GOSUB_WITH_PARAMS,
    op = {
        if (fp >= 50) {
            throw RuntimeException("stack overflow")
        }
        val id = intOperand()
        val proc = RuneScriptProvider.get(id) ?: throw RuntimeException("unable to find proc with id: $id")

        gosubFrame(proc)
    }
)

object PopIntLocalHandler : RuneScriptOpcodeHandler(RuneScriptOpcode.POP_INT_LOCAL,
    op = {
        intLocals[intOperand()] = popInt()
    }
)

object PopVarpHandler : RuneScriptOpcodeHandler(RuneScriptOpcode.POP_VARP,
    op = {
        val secondary = (intOperand() shr 16) and 0x1
        val player = if (secondary != 0) _activePlayer2 else _activePlayer

        if (player == null)
            throw RuntimeException("No Active Player")

        val varpType = VarPlayerType.get(intOperand() and 0xffff) ?: throw RuntimeException("No VarPlayerType")

        if (varpType.type == ScriptVarType.STRING) {
            player.setVar(varpType.id, popString());
        } else {
            player.setVar(varpType.id, popInt());
        }
    }
)

object PushConstIntHandler : RuneScriptOpcodeHandler(RuneScriptOpcode.PUSH_CONSTANT_INT,
    op = {
        pushInt(intOperand())
    }
)

object PushConstStringHandler : RuneScriptOpcodeHandler(RuneScriptOpcode.PUSH_CONSTANT_STRING,
    op = {
        pushString(stringOperand())
    }
)

object PushIntLocalHandler : RuneScriptOpcodeHandler(RuneScriptOpcode.PUSH_INT_LOCAL,
    op = {
        pushInt(intLocals[intOperand()])
    }
)

object ReturnHandler : RuneScriptOpcodeHandler(RuneScriptOpcode.RETURN,
    op = op@{
        if (fp == 0) {
            execution = ScriptState.FINISHED
            return@op
        }

        popFrame()
    }
)