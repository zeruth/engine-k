package rs.engine.script

enum class ScriptPointer {
    ActivePlayer,
    ActivePlayer2,
    ProtectedActivePlayer,
    ProtectedActivePlayer2,
    ActiveNpc,
    ActiveNpc2,
    ActiveLoc,
    ActiveLoc2,
    ActiveObj,
    ActiveObj2,
    _LAST;

    companion object {
        val ActivePlayers =  arrayOf(ActivePlayer, ActivePlayer2)
        val ProtectedActivePlayers =  arrayOf(ProtectedActivePlayer, ProtectedActivePlayer2)
        val ActiveNpcs =  arrayOf(ActiveNpc, ActiveNpc2)
        val ActiveLocs =  arrayOf(ActiveLoc, ActiveLoc2)
        val ActiveObjs =  arrayOf(ActiveObj, ActiveObj2)
    }
}