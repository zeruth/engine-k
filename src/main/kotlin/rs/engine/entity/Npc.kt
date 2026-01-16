package rs.engine.entity

import rs.net.prot.NpcInfoProt

class Npc(
    level: Int,
    x: Int,
    z: Int,
    width: Int,
    height: Int,
    lifeCycle: Int,
    val nid: Int,
    var type: Int,
    moveRestrict: Int,
    blockWalk: Int,
    ) : PathingEntity(level, x, z, width, height, lifeCycle, moveRestrict, blockWalk, MoveStrategy.NAIVE, NpcInfoProt.FACE_COORD, NpcInfoProt.FACE_ENTITY) {
}