package rs.engine.entity

enum class EntityLifeCycle {
    FOREVER, // never respawns or despawns, is always in the world.
    RESPAWN, // entity added from engine that respawns later.
    DESPAWN; // entity added from script that despawns later.

    companion object {
        fun of(id: Int) = MoveRestrict.values()
            .getOrNull(id) ?: throw IndexOutOfBoundsException("Invalid HuntVis id: $id")
    }
}