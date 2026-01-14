package util

import de.mkammerer.argon2.Argon2Factory

object Password {
    private val argon2 = Argon2Factory.create()

    fun hash(password: String): String {
        // Parameters: iterations = 2, memory = 64MB, parallelism = 1
        return argon2.hash(2, 65536, 1, password.toCharArray())
    }

    fun verify(hash: String, password: String): Boolean {
        return argon2.verify(hash, password.toCharArray())
    }

    fun needsRehash(hash: String): Boolean {
        return argon2.needsRehash(hash, 2, 65536, 1)
    }
}
