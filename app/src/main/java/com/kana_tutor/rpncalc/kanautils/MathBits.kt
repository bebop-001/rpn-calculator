package com.kana_tutor.rpncalc.kanautils

import java.lang.Exception

// for saving/restoring doubles using a long-bits string.
val NAN_asString = java.lang.Double
        .doubleToLongBits(Double.NaN).toString(16)
fun Double.toLongBitsString() : String {
    var rv = NAN_asString
    try {
        rv = java.lang.Double
                .doubleToLongBits(this).toString(16)
    }
    catch (e: java.lang.NumberFormatException) {
        println("toLongBitsString \"$this\" FAILED:$e")
    }
    return rv
}
fun String.longBitStringToDouble() : Double {
    var rv = Double.NaN
    try {
        rv = java.lang.Double.longBitsToDouble(this.toLong(16))
    }
    catch (e: Exception) {
        println("longBitsToDouble \"$this\" FAILED:$e")
    }
    return rv
}
