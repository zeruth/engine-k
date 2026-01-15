package rs.engine.script

open class RuneScriptOpcodeHandler(
    val opcode: Int,
    val pointers: Any? = null,
    val op: ScriptState.() -> Unit
)