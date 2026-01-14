package db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import rs.Environment.DB_PASS
import rs.Environment.DB_USER
import util.Logger
import java.sql.Connection
import java.sql.ResultSet

open class DB(val id: String,val  port: Int, val name: String) {
    var ds: HikariDataSource
    val maxPool: Int = 5

    init {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://$id:$port/$name"
            username = DB_USER
            this.password = DB_PASS
            maximumPoolSize = maxPool
            driverClassName = "com.mysql.cj.jdbc.Driver"
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }

        ds = HikariDataSource(config)

        runBlocking {
            // Test connection
            val result = selectOne("SELECT NOW()") { rs ->
                rs.getTimestamp(1)
            }

            Logger.messageColor = Logger.Color.YELLOW
            Logger.info("DB[$name]", "connected @ $result")
        }
    }

    suspend fun <T> query(block: (Connection) -> T): T =
        withContext(Dispatchers.IO) {
            ds.connection.use { block(it) }
        }

    suspend fun <T> selectOne(sql: String, params: List<Any> = emptyList(), mapper: (ResultSet) -> T?): T? =
        query { conn ->
            conn.prepareStatement(sql).use { stmt ->
                params.forEachIndexed { i, v -> stmt.setObject(i + 1, v) }
                stmt.executeQuery().use { rs -> if (rs.next()) mapper(rs) else null }
            }
        }

    suspend fun <T> selectList(sql: String, params: List<Any> = emptyList(), mapper: (ResultSet) -> T): List<T> =
        query { conn ->
            conn.prepareStatement(sql).use { stmt ->
                params.forEachIndexed { i, v -> stmt.setObject(i + 1, v) }
                stmt.executeQuery().use { rs ->
                    mutableListOf<T>().apply {
                        while (rs.next()) add(mapper(rs))
                    }
                }
            }
        }

    suspend fun execute(sql: String, params: List<Any?> = emptyList()): Int =
        query { conn ->
            conn.prepareStatement(sql).use { stmt ->
                params.forEachIndexed { i, v -> stmt.setObject(i + 1, v) }
                stmt.executeUpdate()
            }
        }
}