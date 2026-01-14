package db.login

import kotlinx.coroutines.runBlocking
import util.Logger
import util.SQLUtil.applySqlFile
import util.SQLUtil.databaseExists

object DBLoginBootstrap {

    val login = "login"

    /**
     * !!!DANGER!!! DB RESET
     * Test account create/get
     */
    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
        resetDBAuth()
        val shouldInsert = DBLogin.getOrInsert("tester", "tester!")
        val shouldBeNull = DBLogin.getOrInsert("tester", "tester@")
        val shouldGet = DBLogin.getOrInsert("tester", "tester!")
        println(shouldInsert != null)
        println(shouldBeNull == null)
        println(shouldGet != null)
    }

    /** Completely delete and recreate the database (for testing purposes) */
    fun resetDBAuth() {
        DROP()
        init()
    }

    fun init() {
        if (!databaseExists(login)) {
            Logger.messageColor = Logger.Color.CYAN
            Logger.info("SQL", "Database '$login' does not exist: Initialize now? (y)es (n)o:")
            when (val input = readlnOrNull()?.trim()?.lowercase()) {
                "", "y", "yes", -> {
                    CREATE()
                }
                "n", "no" -> {
                    Logger.messageColor = Logger.Color.YELLOW
                    Logger.info("SQL", "Skipping auth initialization")
                }
                else -> {
                    Logger.messageColor = Logger.Color.YELLOW
                    Logger.info("SQL", "Unknown input '$input'. Defaulting to initialize.")
                    CREATE()
                }
            }
        } else {
            Logger.messageColor = Logger.Color.YELLOW
            Logger.info("SQL", "Database '$login' already exists.")
        }
    }

    fun DROP() {
        applySqlFile("login/drop.sql")
    }

    fun CREATE() {
        applySqlFile("login/create.sql")
        applySqlFile("login/account/create.sql")
    }
}
