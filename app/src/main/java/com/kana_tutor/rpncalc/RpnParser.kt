package com.kana_tutor.rpncalc

import android.util.Log
import java.util.*
import java.text.DecimalFormat
import kotlin.math.cos

import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

class RpnParser private constructor() {
    data class RpnToken(var token: String, var value : Double = Double.NaN) {
        override fun toString(): String {
            return "$token:$value"
        }
    }
    companion object {
        private var digitsFormatIsEnabled = true
        private var digitsFormat = "#,##0.000"

        // from https://rosettacode.org/wiki/Parsing/RPN_calculator_algorithm
        fun rpnCalculate(inStack : Stack<RpnToken>): Stack<RpnToken> {
            fun Stack<RpnToken>.popD(): Double {
                var (token, value) = this.pop()
                // filter out any commas in the string.
                if (token == "π" || token == "PI")
                    value = Math.PI
                return value
            }

            fun Double.toFormatedString(): String {
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
                val newToken = RpnToken(d.toFormatedString(), d)
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
                return RpnToken(value.toFormatedString(), value)
            }
            fun Stack<RpnToken>.shift() : RpnToken = this.removeAt(0)
            fun Stack<RpnToken>.unshift(tok :RpnToken)  = this.add(0, tok)
            fun Double.degreesToRadians(): Double = this * kotlin.math.PI / 180
            fun Double.radiansToDegrees(): Double  = this * 180 / kotlin.math.PI
            if (inStack.isEmpty()) return Stack<RpnToken>()
            println("For: ${inStack.map{"$it"}}")
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
            println("after preprocess: ${inStack.map{"$it"}}")
            val outStack = Stack<RpnToken>()
            var angleIsDegrees = false
            while (inStack.size > 0) {
                var next = inStack.shift()
                var (token, value) = next
                println("next:$next inStack:${inStack.map{"$it"}} outStack:${outStack.map{"$it"}}")
                when (token) {
                    // op that expects two floats on stack.
                    "+", "-", "×", "*", "÷", "/", "^" -> {
                        val d1 = outStack.popD()
                        val d2 = outStack.popD()
                        when (token) {
                            "+"      -> outStack.pushD(d2 + d1)
                            "-"      -> outStack.pushD(d2 - d1)
                            "×", "*" -> outStack.pushD(d2 * d1)
                            "÷", "/" -> outStack.pushD(d2 / d1)
                            "^"      -> outStack.pushD(d2.pow(d1))
                        }
                        println(" $token     Apply op to top of stack    $outStack")
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
                        val result = when (token) {
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
                        var value = inStack.popD()
                        if (angleIsDegrees)
                            value = value.degreesToRadians()
                        var rv = when (token) {
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
                        // try pushD which will succeed if this is a number.
                        try {
                            outStack.pushD(fromFormattedString(token))
                        }
                        catch (e:Exception) {
                            outStack.push(next)
                        }
                    }
                }
            }
            println("return: ${outStack.map{it}}")
            return outStack
        }
    }
}