package com.kana_tutor.rpncalc.kanautils

import java.lang.Exception
import kotlin.random.Random.Default.nextDouble

// Convert long-bits values to string using max allowable
// radix to save on file size when in string form.
const val RADIX = 36
// for saving/restoring doubles using a long-bits string.
val NAN_asString = java.lang.Double
        .doubleToLongBits(Double.NaN).toString(RADIX)
fun Double.toLongBitsString() : String {
    var rv = NAN_asString
    try {
        rv = java.lang.Double
                .doubleToLongBits(this).toString(RADIX)
    }
    catch (e: java.lang.NumberFormatException) {
        println("toLongBitsString \"$this\" FAILED:$e")
    }
    return rv
}
fun String.longBitStringToDouble() : Double {
    var rv = Double.NaN
    try {
        rv = java.lang.Double.longBitsToDouble(this.toLong(RADIX))
    }
    catch (e: Exception) {
        println("longBitsToDouble \"$this\" FAILED:$e")
    }
    return rv
}

fun HashMap<Int, Double>.toJson() : String{
    val x = nextDouble()
    val rv = "{" +
    this.map{(key, value) -> "\"$key\" : \"${value.toLongBitsString()}\""}
            .joinToString(",") + "}\n"
    return rv
}
