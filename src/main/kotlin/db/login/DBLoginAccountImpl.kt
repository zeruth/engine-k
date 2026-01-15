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

    companion object {
        suspend fun DBAccount.update(
            username: String? = null,
            password: String? = null,
            passwordUpdated: LocalDateTime? = null,
            email: String? = null,
            oauthProvider: String? = null,
            mutedUntil: LocalDateTime? = null,
            bannedUntil: LocalDateTime? = null,
            staffModLevel: Int? = null,
            notes: String? = null,
            notesUpdated: LocalDateTime? = null,
            members: Boolean? = null,
            tfaEnabled: Boolean? = null,
            tfaLastCode: Int? = null,
            tfaSecretBase32: String? = null,
            tfaIncorrectAttempts: Int? = null
        ): Boolean {

            val updates = mutableListOf<String>()
            val params = mutableListOf<Any?>()

            fun add(column: String, value: Any?) {
                if (value != null) {
                    updates += "$column = ?"
                    params += value
                }
            }

            add("username", username)
            add("password", password)
            add("password_updated", passwordUpdated)
            add("email", email)
            add("oauth_provider", oauthProvider)
            add("muted_until", mutedUntil)
            add("banned_until", bannedUntil)
            add("staffmodlevel", staffModLevel)
            add("notes", notes)
            add("notes_updated", notesUpdated)
            add("members", members)
            add("tfa_enabled", tfaEnabled)
            add("tfa_last_code", tfaLastCode)
            add("tfa_secret_base32", tfaSecretBase32)
            add("tfa_incorrect_attempts", tfaIncorrectAttempts)

            if (updates.isEmpty()) {
                return false
            }

            val sql =
                """
                    UPDATE account
                    SET ${updates.joinToString(", ")}
                    WHERE id = ?
                """.trimIndent()

            val rows = DBLogin.query { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    params.forEachIndexed { i, value ->
                        stmt.setObject(i + 1, value)
                    }
                    stmt.setInt(params.size + 1, id)
                    stmt.executeUpdate()
                }
            }

            return rows > 0
        }
    }
}