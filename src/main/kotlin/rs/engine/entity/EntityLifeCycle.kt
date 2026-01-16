package rs.engine.entity

object EntityLifeCycle {
    const val FOREVER = 0 // never respawns or despawns, is always in the world.
    const val RESPAWN = 1 // entity added from engine that respawns later.
    const val DESPAWN = 2 // entity added from script that despawns later.
}