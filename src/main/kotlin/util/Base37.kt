package util

import java.math.BigInteger

object Base37 {

    private val BASE37_LOOKUP = arrayOf(
        '_', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
        'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
        't', 'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    )

    private val MAX_BASE37 = BigInteger("6582952005840035281") // 37^12

    fun toBase37(input: String): BigInteger {
        val string = input.trim()
        var result = BigInteger.ZERO

        for (i in string.indices.take(12)) {
            val c = string[i]
            result *= BigInteger.valueOf(37)

            when (c) {
                in 'A'..'Z' -> result += BigInteger.valueOf((c - 'A' + 1).toLong())
                in 'a'..'z' -> result += BigInteger.valueOf((c - 'a' + 1).toLong())
                in '0'..'9' -> result += BigInteger.valueOf((c - '0' + 27).toLong())
            }
        }

        return result
    }

    fun fromBase37(value: BigInteger): String {
        if (value < BigInteger.ZERO || value >= MAX_BASE37) return "invalid_name"
        if (value % BigInteger.valueOf(37) == BigInteger.ZERO) return "invalid_name"

        var v = value
        val chars = CharArray(12)
        var len = 0

        while (v != BigInteger.ZERO) {
            val div = v.divide(BigInteger.valueOf(37))
            val index = (v - div * BigInteger.valueOf(37)).toInt()
            chars[11 - len++] = BASE37_LOOKUP[index]
            v = div
        }

        return chars.copyOfRange(12 - len, 12).concatToString()
    }

    fun toTitleCase(str: String): String {
        return str.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
    }

    fun toSafeName(name: String): String {
        return fromBase37(toBase37(name))
    }

    fun toDisplayName(name: String): String {
        return toTitleCase(toSafeName(name).replace('_', ' '))
    }
}
