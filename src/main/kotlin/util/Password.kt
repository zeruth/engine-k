package util

import de.mkammerer.argon2.Argon2Factory
import rs.Environment.ARGON2_ITERATIONS
import rs.Environment.ARGON2_MEMORY
import rs.Environment.ARGON2_PARALLELISM

object Password {
    private val argon2 = Argon2Factory.create()

    fun hash(password: String): String {
        return argon2.hash(ARGON2_ITERATIONS, ARGON2_MEMORY, ARGON2_PARALLELISM, password.toCharArray())
    }

    fun verify(hash: String, password: String): Boolean {
        return argon2.verify(hash, password.toCharArray())
    }

    fun needsRehash(hash: String): Boolean {
        return argon2.needsRehash(hash, ARGON2_ITERATIONS, ARGON2_MEMORY, ARGON2_PARALLELISM)
    }
}
