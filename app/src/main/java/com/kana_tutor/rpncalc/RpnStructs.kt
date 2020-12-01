package com.kana_tutor.rpncalc

import com.kana_tutor.rpncalc.RpnParser.Companion.toFormattedString
import com.kana_tutor.rpncalc.kanautils.longBitsToDoubleOrNull
import com.kana_tutor.rpncalc.kanautils.toLongBitsString
import java.util.*
import java.util.Collections.addAll
import kotlin.math.abs


data class RpnToken(var token: String, var value : Double = java.lang.Double.NaN) {
    companion object {
        var error = ""
        fun RpnToken?.asDouble() : Double? {
            return (
                if (this != null && !this.value.isNaN()) value
                else null
            )
        }
        fun RpnToken?.isNumber():Boolean? {
            return (
                when {
                    this == null -> null
                    token == "PI" || token == "π" -> true
                    else -> {
                        val t = token.split(",").joinToString("")
                        val matchResult =
                                "^(?:-*\\d)*(?:.\\d+)*(?:E-*\\d+)*$".toRegex()
                                        .find(t)
                        matchResult != null
                    }
                }
            )
        }
        fun RpnToken?.isInt() :Boolean? {
            return (
                if (this != null) {
                    if (this.isNumber()!!)
                        abs(value - value.toInt()) < 1E-6
                    else false
                }
                else null
            )
        }
        fun RpnToken?.isIndex (range : IntRange = registerRange) : Boolean? {
            error = ""
            return (
                if (this != null) {
                    if (isInt()!!) {
                        if (value.toInt() in range) true
                        else {
                            error = "isIndex: $value out of range $range"
                            false
                        }
                    }
                    else {
                        error = "isIndex: value:$value not an int"
                        false
                    }
                }
                else null
            )
        }
        fun RpnToken?.asIndex(range : IntRange = registerRange) : Int? {
            return (
                when {
                    this == null -> null
                    isIndex(range)!! -> value.toInt()
                    else -> null
                }
            )
        }
        fun RpnToken?.isDoubleOrNull() : Boolean? {
            var rv : Boolean? = null
            if (this != null) {
                try {
                    this.value.toDouble()
                    rv = true
                }
                catch (e: Exception){
                    rv = false
                }
            }
            return rv
        }
        fun RpnToken.fromStorable():RpnToken? {
            var rv : RpnToken? = null
            val tok = this.token.split(":")
            val (_, longBits, regNum) = tok
            val idx = regNum.toIntOrNull()
            val double = longBits.longBitsToDoubleOrNull()
            if (idx != null && double != null) {
                if (idx in registerRange)
                    rv = RpnToken("REG=$idx", double)
                else if (idx == -1) {
                    rv = RpnToken(tok[3], double)
                }
            }
            return rv
        }
        fun RpnToken.toStorable(index:Int=-1) : RpnToken {
            val longBits = value.toLongBitsString()
            return RpnToken("STORABLE:$longBits:$index:$token")
        }

    }
    init {
        if (token == "PI" || token == "π")
            value = Math.PI
        else if (java.lang.Double.isNaN(value)) {
            try {
                value = token.toDouble()
            }
            catch (e:Exception) {
                // using exception to check string as double. */
            }
        }
    }
    constructor(d:Double) : this(d.toFormattedString(), d)
    fun toDouble(d : Double) : RpnToken {
        this.value = d
        this.token = d.toFormattedString()
        return this
    }
    override fun toString(): String {return "$token:$value"}
}
class RpnStack : java.util.Stack<RpnToken>() {
    companion object {
        fun String.toRpnStack(): RpnStack {
            val rv = RpnStack()
            val strTokens = split("\n")
                    .filter { "^\\s*#".toRegex().find(it) == null }
                    .joinToString("\n")
                    .split("\\s+".toRegex())
                    .filter { it.isNotEmpty() }
            strTokens.forEach { rv.add(RpnToken(it, Double.NaN)) }
            return rv
        }

        fun RpnStack?.rmLast(offset: Int = 0): RpnToken? {
            return (
                if (this == null) null
                else removeAt(lastIndex + offset)
            )
        }
        fun RpnStack?.peekLast(offset:Int = 0) : RpnToken? {
            return (
                if (this == null) null
                else if (lastIndex + offset >= 0) this.elementAt(lastIndex + offset)
                else null
            )
        }
    }
    fun setLast(token :RpnToken, offset:Int = 0) {
        val off = lastIndex + offset
        if (off < 0 || off > lastIndex) this.add(token)
        else this[off] = token
    }
}

class RpnMap {
    val map = mutableMapOf<Int, RpnToken>()
    val size:Int
        get() = map.size
    val keys:List<Int>
        get() = map.keys.sorted()
    companion object {
        var mapError = ""
        fun RpnMap?.rm(idx:Int) : RpnToken? {
            mapError = ""
            return  when {
                this == null -> {
                    mapError = "Emptty object"
                    null
                }
                map.isEmpty() || !map.containsKey(idx) -> {
                    mapError = "Map doesn't contain element $idx"
                    null
                }
                else -> map.remove(idx)
            }
        }
    }
    // map only contains tokens with valid double value.
    operator fun set(idx:Int, rpnToken:RpnToken) :Boolean {
        rpnToken.token = "REG=$idx"
        map[idx] = rpnToken
        return true
    }
    operator fun get(idx:Int)  : RpnToken? {
        return (
            if (map.containsKey(idx)) map[idx]
            else null
        )
    }
    fun remove(key : Int) : RpnToken? = map.remove(key)
    fun containsKey(key : Int) : Boolean = map.containsKey(key)
    fun clear() = map.clear()
    fun toStorable() : List<RpnToken> {
        val rv = mutableListOf<RpnToken>()
        return rv
    }
}
