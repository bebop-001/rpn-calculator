@file:Suppress("unused")

package com.kana_tutor.rpncalc

import java.lang.Double.NaN
import java.lang.Double.isNaN
import java.text.DecimalFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

class RpnParser private constructor() {
    data class RpnToken(var token: String, var value : Double = NaN) {
        init {
            if (isNaN(value)) {
                try {
                    val d = token.toDouble()
                        value = d
                }
                catch (e:Exception) {
                    // using exception to check string as double. */
                }
            }
        }
        override fun toString(): String {return "$token:$value"}
    }
    companion object {
        private var digitsFormatIsEnabled = true
        var digitsAfterDecimal = 3
            private set
        var commasEnabled = true
            private set

        private var digitsFormat = "#,##0.000"
        // syntax: "on|off:digits:on|off digitsFormat" for enable:digits after decimal:comma enable
        fun setDigitsFormatting(enable : Boolean, digits: Int = 4, commas : Boolean = true) {
            digitsFormatIsEnabled = enable
            if (enable) {
                commasEnabled = commas
                digitsAfterDecimal = digits
                digitsFormat = if (commas) "#,##0" else "0"
                digitsFormat += if (digits > 0) "." + "0".repeat(digits) else ""
            }
        }

        // from https://rosettacode.org/wiki/Parsing/RPN_calculator_algorithm
        fun rpnCalculate(inStack : Stack<RpnToken>): Stack<RpnToken> {
            fun Stack<RpnToken>.popD(): Double {
                var (token, value) = this.pop()
                // filter out any commas in the string.
                if (token == "π" || token == "PI")
                    value = Math.PI
                return value
            }

            fun Double.toFormattedString(): String {
                return if (digitsFormatIsEnabled) {
                    DecimalFormat(digitsFormat)
                            .format(this)
                            .toString()
                }
                else this.toString()
            }
            fun fromFormattedString(str:String) : Double =
                str.split(",").joinToString().toDouble()
            fun Stack<RpnToken>.pushD(d: Double) : RpnToken {
                val newToken =  if (digitsFormatIsEnabled)
                        RpnToken(d.toFormattedString(), d)
                    else
                        RpnToken(d.toString(), d)
                this.push(newToken)
                return newToken
            }

            fun Stack<RpnToken>.lastEquals(token: String): Boolean {
                val lastIndex = this.lastIndex
                return lastIndex >= 0 && this[lastIndex].token == token
            }

            fun RpnToken.chs() : RpnToken {
                var(_, value) = this
                value *= -1
                return RpnToken(value.toFormattedString(), value)
            }
            fun Stack<RpnToken>.shift() : RpnToken = this.removeAt(0)
            fun Stack<RpnToken>.unshift(tok :RpnToken)  = this.add(0, tok)
            fun Double.degreesToRadians(): Double = this * kotlin.math.PI / 180
            fun Double.radiansToDegrees(): Double  = this * 180 / kotlin.math.PI
            if (inStack.isEmpty()) return Stack<RpnToken>()
            println("Trace: For: ${inStack.map{"$it"}}")
            // format token for any token that has a value.
            inStack.filter{!isNaN(it.value)}
                    .map{it.token = it.value.toFormattedString()}
            when {
                inStack.lastEquals("CHS")  -> {
                    inStack.pop()
                    inStack.push(inStack.pop().chs())
                }
                inStack.lastEquals("SWAP") -> {
                    inStack.pop()
                    val t1 = inStack.pop()
                    val t2 = inStack.pop()
                    inStack.push(t1)
                    inStack.push(t2)
                }
                inStack.lastEquals("DROP") -> {
                    inStack.pop()
                    inStack.pop()
                }
                inStack.lastEquals("CLR")  -> inStack.clear()
            }
            println("Trace: after preprocess: ${inStack.map{"$it"}}")
            val outStack = Stack<RpnToken>()
            var angleIsDegrees = false
            while (inStack.size > 0) {
                val next = inStack.shift()
                println("Trace: next:$next inStack:${inStack.map{"$it"}} " +
                        "outStack:${outStack.map{"$it"}}")
                when (next.token) {
                    // op that expects two floats on stack.
                    "+", "-", "×", "*", "÷", "/", "^" -> {
                        val d1 = outStack.popD()
                        val d2 = outStack.popD()
                        when (next.token) {
                            "+"      -> outStack.pushD(d2 + d1)
                            "-"      -> outStack.pushD(d2 - d1)
                            "×", "*" -> outStack.pushD(d2 * d1)
                            "÷", "/" -> outStack.pushD(d2 / d1)
                            "^"      -> outStack.pushD(d2.pow(d1))
                        }
                    }
                    "RAD"                             -> {
                        angleIsDegrees = false
                    }
                    "DEG"                             -> {
                        angleIsDegrees = true
                    }
                    "SIN", "COS", "TAN"               -> {
                        var angle = outStack.popD()
                        if (angleIsDegrees)
                            angle = angle.degreesToRadians()
                        val result = when (next.token) {
                            "SIN" -> sin(angle)
                            "COS" -> cos(angle)
                            "TAN" -> tan(angle)
                            else  -> {
                                0.0 /* can't happen. */
                            }
                        }
                        outStack.pushD(result)
                    }
                    "ASIN", "ACOS", "ATAN"            -> {
                        var value = outStack.popD()
                        if (angleIsDegrees)
                            value = value.degreesToRadians()
                        var rv = when (next.token) {
                            "ASIN" -> kotlin.math.asin(value)
                            "ACOS" -> kotlin.math.acos(value)
                            "ATAN" -> kotlin.math.atan(value)
                            else   -> {
                                0.0 /* can't happen. */
                            }
                        }
                        if (angleIsDegrees)
                            rv = rv.radiansToDegrees()
                        outStack.pushD(rv)
                    }
                    // push an empty token on the stack. ENTR caused call
                    // to parser.  That's all that we need.
                    "ENTR" -> {
                    }
                    // push the value on the stack.  It's probably a number.
                    else                              -> {
                        outStack.push(next)
                    }
                }
            }
            println("Trace: return: ${outStack.map{it}}")
            return outStack
        }
    }
}
