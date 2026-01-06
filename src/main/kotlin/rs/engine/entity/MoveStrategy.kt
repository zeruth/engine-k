package rs.engine.entity

enum class MoveStrategy {
    SMART,
    NAIVE,
    FLY;

    companion object {
        fun of(id: Int) = values()
            .getOrNull(id) ?: throw IndexOutOfBoundsException("Invalid HuntVis id: $id")
    }
}