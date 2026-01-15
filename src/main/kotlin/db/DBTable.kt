package db

import ext.SqlExt.toDataClass
import util.Logger
import kotlin.reflect.KClass

open class DBTable<T : Any>(
    val db: DB,
    val name: String,
    val type: KClass<T>
) {
    suspend fun reset(): Int {
        db.execute("DELETE FROM $name;")
        db.execute("ALTER TABLE $name AUTO_INCREMENT = 1;")
        Logger.messageColor = Logger.Color.CYAN
        Logger.info("DB[${db.name}]", "TABLE[$name] reset")
        return 0
    }

    suspend fun list(): List<T> =
        db.selectList("SELECT * FROM $name") { rs ->
            rs.toDataClass(type)
        }

    suspend fun getUnique(field: String, arg: Int): T? =
        db.selectOne("SELECT * FROM $name WHERE $field = ?", listOf(arg)) { rs ->
            rs.toDataClass(type)
        }

    suspend fun getUnique(field: String, arg: String): T? =
        db.selectOne("SELECT * FROM $name WHERE $field = ?", listOf(arg)) { rs ->
            rs.toDataClass(type)
        }

    open suspend fun insert(args: Array<Any>): Any {
       TODO()
    }
}


