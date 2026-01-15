package rs.engine.entity;

enum class PlayerStat {
    ATTACK,
    DEFENCE,
    STRENGTH,
    HITPOINTS,
    RANGED,
    PRAYER,
    MAGIC,
    COOKING,
    WOODCUTTING,
    FLETCHING,
    FISHING,
    FIREMAKING,
    CRAFTING,
    SMITHING,
    MINING,
    HERBLORE,
    AGILITY,
    THIEVING,
    SLAYER,
    FARMING,
    RUNECRAFT;

    companion object {
        // Map<String, PlayerStat>
        val nameMap: Map<String, PlayerStat> = values().associateBy { it.name }

        // Map<PlayerStat, String>
        val statNameMap: Map<PlayerStat, String> = values().associateWith { it.name }

        // Enabled flags for each stat (by ordinal)
        val enabled: BooleanArray = booleanArrayOf(
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, false, false, true
        )

        // Free-to-play flags for each stat (by ordinal)
        val free: BooleanArray = booleanArrayOf(
            true, true, true, true, true, true, true, true, true, false,
            true, true, true, true, true, false, false, false, false, false, true
        )
    }
}
