package rs.net.msg.out.game

open class ServerGameZoneMessage(val coord: Int) : ServerGameMessage(ServerGameProtPriority.IMMEDIATE, encode = null)