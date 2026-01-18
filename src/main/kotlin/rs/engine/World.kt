package rs.engine

import ServerWorld
import ServerWorld.computeUid
import db.login.DBLogin
import rs.Environment
import rs.engine.entity.Npc
import rs.engine.entity.Player
import rs.engine.entity.PlayerLoading
import rs.engine.entity.PlayerStat
import rs.engine.game.Inventory
import rs.engine.script.RuneScriptRunner
import rs.engine.script.ScriptState
import rs.engine.zone.ZoneMap
import rs.net.Client
import rs.util.EntityPool
import util.Logger
import java.math.BigInteger

object World {
    val PLAYERS = Environment.NODE_MAX_PLAYERS
    val NPCS = Environment.NODE_MAX_NPCS
    val TICKRATE = Environment.TICK_RATE

    var invs: HashMap<Int, Inventory> = HashMap()
    var shutdown: Boolean = false

    var currentTick: Int = 0 // the current tick of the game world.

    val players = EntityPool<Player>(PLAYERS)
    val npcs = EntityPool<Npc>(NPCS)
    var nextTick = 0L

    val gameMap = GameMap

    val newPlayers = ArrayList<Player>(PLAYERS)

    fun getInventory(inv: Int) : Inventory? {
        if (inv == -1) return null;

        for (inventory in invs.values) {
            if (inventory.type == inv)
                return inventory;
        }

        val invetory = Inventory.fromType(inv)
        invs[inv] = invetory

        return invetory
    }

    fun getPlayerByUid(uid: Int) = players.values.firstOrNull { it.uid == uid }

    fun cycle() {
        // world processing
        // - world queue
        // - npc hunt
        //processWorld();

        // client input
        // - calculate afk event readiness
        // - process packets
        // - process pathfinding/following request
        // - client input tracking
        //processClientsIn();

        // Spawn triggers, despawn triggers
        //processNpcEventQueue();

        // npc processing (if npc is not busy)
        // - resume suspended script
        // - stat regen
        // - timer
        // - queue
        // - movement
        // - modes
        //processNpcs();

        // player processing
        // - primary queue
        // - weak queue
        // - timers
        // - soft timers
        // - engine queue
        // - interactions
        // - movement
        // - close interface if attempting to logout
        //processPlayers();

        // player logout
        //processLogouts();

        // player login, good spot for it (before packets so they immediately load but after processing so nothing hits them)
        processLogins();

        // process zones
        // - build list of active zones around players
        // - loc/obj despawn/respawn
        // - compute shared buffer
        //processZones();

        // process player & npc update info
        // - convert player movements
        // - compute player info
        // - convert npc movements
        // - compute npc info
        //processInfo();

        // client output
        // - map update
        // - player info
        // - npc info
        // - zone updates
        // - inv changes
        // - stat changes
        // - afk zones changes
        // - flush packets
        processClientsOut();

        // cleanup
        // - reset zones
        // - reset players
        // - reset npcs
        // - reset invs
        //processCleanup();

        // ----
    }

    fun processLogins() {
        for (player in newPlayers) {

        }
    }

    fun processClientsOut() {

    }

    fun newPlayer(safeName: String, name37: BigInteger, hash64: BigInteger) : Player {
        return Player(safeName, name37, hash64).apply {
            for (i in 0 until 21) {
                stats[i] = 0;
                baseLevels[i] = 1;
                levels[i] = 1;
            }

            // hitpoints starts at level 10
            stats[PlayerStat.HITPOINTS.ordinal] = getExpByLevel(10);
            baseLevels[PlayerStat.HITPOINTS.ordinal] = 10;
            levels[PlayerStat.HITPOINTS.ordinal] = 10;
        }
    }

    suspend fun login(client: Client, username: String, password: String) {
        val account = DBLogin.getOrInsert(username, password)

        account?.let {
            val player = PlayerLoading.load(account)
            val pid = players.nextFreeId() ?: return
            players.add(pid, player)

            player.pid = pid
            player.uid = computeUid(player.name37, player.pid)
            player.tele = true
            player.moveClickRequest = false

            gameMap.getZone(player.x, player.z, player.level).enter(player)
            player.onLogin()
            Logger.messageColor = Logger.Color.CYAN
            Logger.info("LOGIN", "[${account.username}-${client.uuid}] (Passed - CRCs / RSA / Password)")
        }
    }

    fun getNextNid() : Int {
        return npcs.nextFreeId() ?: throw RuntimeException("No npc nextFreeId")
    }

    fun addNpc(npc: Npc, duration: Int, firstSpawn: Boolean = true) {
        if (firstSpawn) {
            npcs.add(npc.nid, npc)
        }
    }
}