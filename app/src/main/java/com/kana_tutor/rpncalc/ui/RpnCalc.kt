package com.kana_tutor.rpncalc.ui

import java.util.*
import kotlin.math.pow

// from https://rosettacode.org/wiki/Parsing/RPN_calculator_algorithm
fun rpnCalculate(expr: String) : String {
    fun Stack<String>.popD():Double {
        return this.pop().toDouble()
    }
    fun Stack<String>.pushD(d: Double) {
        this.push(d.toString())
    }
    fun List<String>.lastEquals(token: String) : Boolean {
        val lastIndex = this.lastIndex
        return lastIndex >= 0 && this[lastIndex] == token
    }
    fun String?.isNumber() : Boolean =
            (this != null && "^[-+]*\\d+(?:.\\d+)*$".toRegex().matches(this))
    fun Double.degreesToRadians() : Double = this * kotlin.math.PI / 180
    fun Double.radiansToDegrees() : Double = this * 180 / kotlin.math.PI
    var angleIsDegrees = true

    if (expr.isEmpty()) return ""
    println("For expression = $expr\n")
    println("Token           Action             Stack")
    val tokens = Stack<String>()
    tokens.addAll(expr.split("\\s+".toRegex()).filter { it.isNotEmpty() })
    when {
        tokens.lastEquals("CHS") -> {
            tokens.pop()
            if (tokens.size > 0) {
                var t = tokens.pop()
                if (t.isNumber()) {
                    t = when {
                        t.startsWith("+") -> t.replaceFirst("+", "-")
                        t.startsWith("-") -> t.replaceFirst("-", "+")
                        else -> "-$t"
                    }
                }
                tokens.push(t)
            }
        }
        tokens.lastEquals("SWAP") -> {
            tokens.pop()
            if (tokens.size >= 2) {
                val t1 = tokens.pop()
                val t2 = tokens.pop()
                tokens.push(t1)
                tokens.push(t2)
                return tokens.joinToString("\n") + "\n"
            }
            // swap without enough values, just return the input.
            var rv =  tokens.joinToString("\n")
            if (expr.endsWith("\n SWAP")) rv += "\n"
            return rv
        }
        tokens.lastEquals("DROP") -> {
            tokens.pop()
            if (tokens.size >= 1) {
                tokens.pop()
            }
            return tokens.joinToString("\n") + "\n"
        }
        tokens.lastEquals("CLR") -> tokens.clear()
    }
    val stack = Stack<String>()
    for (token in tokens) {
        when (token) {
            // op that expects two floats on stack.
            "+", "-", "×", "*", "÷", "/", "^" -> {
                val d1 = stack.popD()
                val d2 = stack.popD()
                when (token) {
                    "+"      -> stack.pushD(d2 + d1)
                    "-"      -> stack.pushD(d2 - d1)
                    "×", "*" -> stack.pushD(d2 * d1)
                    "÷", "/" -> stack.pushD(d2 / d1)
                    "^"      -> stack.pushD(d2.pow(d1))
                }
                println(" $token     Apply op to top of stack    $stack")
            }
            "RAD" -> {
                angleIsDegrees = false
            }
            "DEG"               -> {
                angleIsDegrees = true
            }
            "SIN", "COS", "TAN" -> {
                var angle = stack.popD()
                if (angleIsDegrees)
                    angle = angle.degreesToRadians()
                stack.pushD(when (token) {
                    "SIN" -> kotlin.math.sin(angle)
                    "COS" -> kotlin.math.cos(angle)
                    "TAN" -> kotlin.math.tan(angle)
                    else  -> {
                        0.0 /* can't happen. */
                    }
                })
            }
            "ASIN", "ACOS", "ATAN" -> {
                var value = stack.popD()
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
                stack.pushD(rv)
            }
            "ENTR" -> stack.push("")
            else -> stack.push(token)
        }
    }
    return stack.joinToString("\n")
}