package util

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rs.Environment
import java.sql.SQLException

/**
 * Utility object for executing .sql files and performing DB operations.
 * Supports multiple hosts/ports/databases and keeps connections cached.
 *
 * TODO: Clean this up into a class based object
 */
object SQLUtil {

    /** Cached connections keyed by Triple(host, port, database) */
    private val connections = mutableMapOf<Triple<String, Int, String?>, Connection>()

    /**
     * Get or create a persistent JDBC connection.
     *
     * @param host MySQL host
     * @param port MySQL port
     * @param database Optional database name; null = server root
     */
    @Synchronized
    private fun getConnection(host: String, port: Int, database: String? = null): Connection {
        val key = Triple(host, port, database)
        val existing = connections[key]
        if (existing?.isValid(2) == true) return existing

        val url = if (database.isNullOrEmpty()) {
            "jdbc:mysql://$host:$port"
        } else {
            "jdbc:mysql://$host:$port/$database"
        }

        try {
            val conn = DriverManager.getConnection(url, Environment.DB_USER, Environment.DB_PASS)
            conn.autoCommit = true

            connections[key] = conn

            Logger.messageColor = Logger.Color.YELLOW
            Logger.info("SQL", "Opened connection to ${database ?: "MySQL"} at $host:$port")
            return conn
        }catch (e: SQLException) {
            throw RuntimeException("Exception while opening MySQL connection to ${url}:$port", e)
        }
    }

    /** Executes all SQL statements from the given file (synchronous). */
    fun applySqlFile(
        sqlFilePath: String,
        host: String = Environment.DB_LOGIN_IP,
        port: Int = Environment.DB_LOGIN_PORT,
        database: String? = null
    ) {
        val file = File("./data/sql/$sqlFilePath")
        require(file.exists()) { "SQL file not found: ${file.path}" }

        val statements = splitSqlStatements(file.readText())
        val conn = getConnection(host, port, database)

        conn.createStatement().use { stmt ->
            conn.autoCommit = false
            try {
                for (statement in statements) {
                    if (statement.isNotBlank()) stmt.addBatch(statement)
                }
                stmt.executeBatch()
                conn.commit()
                Logger.messageColor = Logger.Color.GREEN
                Logger.info("SQL", "Applied $sqlFilePath" + if (database != null) " to $database" else "")
            } catch (e: Exception) {
                conn.rollback()
                Logger.messageColor = Logger.Color.RED
                Logger.info("SQL", "Error applying $sqlFilePath" + if (database != null) " to $database" else "")
                throw e
            } finally {
                conn.autoCommit = true
            }
        }
    }

    /** Async variant. */
    suspend fun applySqlFileAsync(
        sqlFilePath: String,
        host: String = Environment.DB_LOGIN_IP,
        port: Int = Environment.DB_LOGIN_PORT,
        database: String? = null
    ) = withContext(Dispatchers.IO) {
        applySqlFile(sqlFilePath, host, port, database)
    }

    /** Checks whether a table exists in the specified database. */
    fun tableExists(host: String, port: Int, database: String, tableName: String): Boolean {
        val conn = getConnection(host, port, database)
        conn.metaData.getTables(database, null, tableName, null).use { rs ->
            return rs.next()
        }
    }

    /** Async variant. */
    suspend fun tableExistsAsync(host: String, port: Int, database: String, tableName: String): Boolean =
        withContext(Dispatchers.IO) {
            tableExists(host, port, database, tableName)
        }

    /** Checks whether a database exists on the MySQL server. */
    fun databaseExists(database: String, host: String? = Environment.DB_LOGIN_IP, port: Int? = Environment.DB_LOGIN_PORT, ): Boolean {
        val conn = getConnection(host!!, port!!)
        conn.metaData.catalogs.use { rs ->
            while (rs.next()) {
                if (rs.getString(1).equals(database, ignoreCase = true)) return true
            }
        }
        return false
    }

    /** Async variant. */
    suspend fun databaseExistsAsync(host: String, port: Int, database: String): Boolean =
        withContext(Dispatchers.IO) {
            databaseExists(database, host, port)
        }

    /** Splits a raw SQL file into individual statements. */
    private fun splitSqlStatements(sql: String): List<String> =
        sql.splitToSequence(';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

    /** Closes all cached connections. */
    fun closeAllConnections() {
        for ((key, conn) in connections) {
            if (!conn.isClosed) conn.close()
            Logger.messageColor = Logger.Color.YELLOW
            val (host, port, db) = key
            Logger.info("SQL", "Closed connection to ${db ?: "server root"} at $host:$port")
        }
        connections.clear()
    }
}
