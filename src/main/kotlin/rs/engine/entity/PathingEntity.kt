package rs.engine.entity

open class PathingEntity(
    level: Int,
    x: Int,
    z: Int,
    width: Int,
    length: Int,
    lifecycle: EntityLifeCycle,
    val moveRestrict: MoveRestrict,
    val blockWalk: BlockWalk,
    val moveStrategy: MoveStrategy,
    val coordmask: Int,
    val entitymask: Int) : Entity(level, x, z, width, length, lifecycle) {

    init {
        TODO()
    }
}