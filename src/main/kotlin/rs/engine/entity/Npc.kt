package rs.engine.entity

import rs.NpcInfoProt

class Npc(
    level: Int,
    x: Int,
    z: Int,
    width: Int,
    height: Int,
    lifeCycle: EntityLifeCycle,
    moveRestrict: MoveRestrict,
    blockWalk: BlockWalk,
    var nid: Int,
    var type: Int,
    ) : PathingEntity(level, x, z, width, height, lifeCycle, moveRestrict, blockWalk, MoveStrategy.NAIVE, NpcInfoProt.FACE_COORD, NpcInfoProt.FACE_ENTITY) {
}