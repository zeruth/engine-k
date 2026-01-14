package db.login

import db.DB
import rs.Environment
import util.Logger
import util.Password

object DBLogin : DB(Environment.DB_LOGIN_IP, Environment.DB_LOGIN_PORT, "login") {

    val account = DBLoginAccountImpl(this)

    suspend fun get(id: Int): DBLoginAccountImpl.DBAccount? =
        account.getUnique("id", id)

    suspend fun get(username: String): DBLoginAccountImpl.DBAccount? =
        account.getUnique("username", username)

    suspend fun getOrInsert(username: String, password: String): DBLoginAccountImpl.DBAccount? {
        get(username)?.let {
            return if (Password.verify(it.password, password)) {
                Logger.messageColor = Logger.Color.CYAN
                Logger.info("LOGIN", "LOGGED_IN: ${it.username}")
                it
            } else {
                null
            }
        }
        val new = account.insert(arrayOf(username, Password.hash(password))) as DBLoginAccountImpl.DBAccount
        Logger.messageColor = Logger.Color.CYAN
        Logger.info("LOGIN", "CREATED: ${new.username}")
        return new
    }
}