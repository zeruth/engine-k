package rs.engine.entity


class Loc(level: Int, x: Int, z: Int, width: Int, length: Int, lifecycle: Int,
    val type: Int,
    val shape: Int,
    val angle: Int) : Entity(level, x, z, width, length, lifecycle) {
}