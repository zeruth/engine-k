package ext

import java.sql.ResultSet
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor

object SqlExt {
    private val constructors = ConcurrentHashMap<KClass<*>, KFunction<*>>()

    fun <T : Any> ResultSet.toDataClass(type: KClass<T>): T {
        val ctor = constructorFor(type)

        val params = ctor.parameters.associateWith { param ->
            this.getObject(param.name)
        }

        return ctor.callBy(params)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T: Any> constructorFor(type: KClass<T>): KFunction<T> =
        constructors.getOrPut(type) {
            type.primaryConstructor!!
        } as KFunction<T>
}
