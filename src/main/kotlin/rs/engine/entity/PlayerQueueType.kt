package rs.engine.entity

import rs.engine.script.ScriptFile
import rs.engine.script.ScriptState
import rs.util.Linkable

object PlayerQueueType {
    const val NORMAL = 0
    const val LONG = 1   // like normal with dev-controlled logout behavior
    const val ENGINE = 2
    const val WEAK = 3   // sept 2004
    const val STRONG = 4 // late-2004
    const val SOFT = 5   // OSRS
}

typealias QueueType = PlayerQueueType

class PlayerQueueRequest(
    val type: QueueType,
    val script: ScriptFile,
    val args: Array<Any>,
    var delay: Int
) : Linkable() {

    var lastInt: Int = 0
}

class EntityQueueState(
    val script: ScriptState,
    var delay: Int
) : Linkable()
