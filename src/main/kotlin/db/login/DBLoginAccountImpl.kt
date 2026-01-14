package db.login

import db.DBTable
import db.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.sql.Statement
import java.time.LocalDateTime

class DBLoginAccountImpl(val login: DBLogin) : DBTable<DBLoginAccountImpl.DBAccount>(login, "account", DBAccount::class){
    @Serializable
    data class DBAccount(
        val id: Int,
        var username: String, //req
        var password: String, //req
        @Serializable(with = LocalDateTimeSerializer::class)
        var password_updated: LocalDateTime?,
        var email: String?,
        var oauth_provider: String?,
        var registration_ip: String?,
        @Serializable(with = LocalDateTimeSerializer::class)
        var registration_date: LocalDateTime,
        @Serializable(with = LocalDateTimeSerializer::class)
        var muted_until: LocalDateTime?,
        @Serializable(with = LocalDateTimeSerializer::class)
        var banned_until: LocalDateTime?,
        var staffmodlevel: Int,
        var notes: String?,
        @Serializable(with = LocalDateTimeSerializer::class)
        var notes_updated: LocalDateTime?,
        var members: Boolean,
        var tfa_enabled: Boolean,
        var tfa_last_code: Int,
        var tfa_secret_base32: String?,
        var tfa_incorrect_attempts: Int
    )

    override suspend fun insert(args: Array<Any>) : Any {
        val username = args[0] as String
        val password = args[1] as String

        val index = DBLogin.query { conn ->
            conn.prepareStatement(
                "INSERT INTO account (username, password) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS
            ).use { stmt ->
                stmt.setString(1, username)
                stmt.setString(2, password)

                stmt.executeUpdate()

                stmt.generatedKeys.use { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        }

        return login.get(index)!!
    }
}