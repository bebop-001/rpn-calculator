@file:Suppress("unused")

package com.kana_tutor.rpncalc

import java.lang.Double.NaN
import java.lang.Double.isNaN
import java.text.DecimalFormat
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

class RpnParserException (message:String) : Exception(message)
const val RADIX = 36
class RpnParser private constructor() {
    data class RpnToken(var token: String, var value : Double = NaN) {
        val original = token
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
        override fun toString(): String {return "$token:$value:$original"}
    }
    class RpnStack() : java.util.Stack<RpnToken>() {
        companion object {
            fun String.toRpnStack() : RpnStack {
                val rpnStack = RpnStack()
                val strTokens  = split("\n")
                        .filter{"^\\s*#".toRegex().find(it) == null}
                        .joinToString("\n")
                        .split("\\s+".toRegex())
                        .filter{it.isNotEmpty()}
                strTokens.forEach{rpnStack.add(RpnToken(it))}
                return rpnStack
            }
        }
        enum class StkError (val errorType:String) {
            empty("Stack is empty"),
            conversion("Conversion error"),
        }

        fun peek(offset:Int = 0) : RpnToken? {
            return (
                if (this.lastIndex + offset >= 0)
                    this[this.lastIndex + offset]
                else
                    null
            )
        }
        fun shift()  = this.removeAt(0)
        fun unshift(tok :RpnToken)  = this.add(0, tok)
        fun popD(): Double {
            var (token, value) = this.pop()
            // filter out any commas in the string.
            if (token == "π" || token == "PI")
                value = Math.PI
            return value
        }
        fun pop(offset : Int = 0) :RpnToken? {
            return (
                    if ((this.lastIndex + offset) >= 0) this[this.lastIndex + offset]
                    else null)
        }


    }
    companion object {
        var registers = mutableMapOf<Int, Double>()
        var printTrace = false
        fun printTrace (trace : String) {
            if(printTrace) println(trace)
        }
        fun clearRegisters () {
            registers.clear()
        }
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
        fun Double.toFormattedString(): String {
            return if (digitsFormatIsEnabled) {
                DecimalFormat(digitsFormat)
                        .format(this)
                        .toString()
            }
            else this.toString()
        }

        // from https://rosettacode.org/wiki/Parsing/RPN_calculator_algorithm
        fun rpnCalculate(inStack : RpnStack): RpnStack {
            fun fromFormattedString(str:String) : Double =
                str.split(",").joinToString().toDouble()
            fun RpnStack.pushD(d: Double) : RpnToken {
                val newToken =  if (digitsFormatIsEnabled)
                        RpnToken(d.toFormattedString(), d)
                    else
                        RpnToken(d.toString(), d)
                this.push(newToken)
                return newToken
            }

            fun RpnStack.lastEquals(token: String): Boolean {
                val lastIndex = this.lastIndex
                return lastIndex >= 0 && this[lastIndex].token == token
            }

            fun RpnToken.chs() : RpnToken {
                var(_, value) = this
                value *= -1
                return RpnToken(value.toFormattedString(), value)
            }
            fun Double.degreesToRadians(): Double = this * kotlin.math.PI / 180
            fun Double.radiansToDegrees(): Double  = this * 180 / kotlin.math.PI
            fun String.isValidIndex(range: IntRange):Boolean{
                var rv = false
                try {
                    val idx = this.toInt()
                    rv = idx in range
                }
                catch (e:Exception) {/* ignore, using toInt to test for valid int. */}
                return rv
            }
            if (inStack.isEmpty()) return RpnStack()
            printTrace("Trace: For: ${inStack.map{"$it"}}")
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
            }
            printTrace("Trace: after preprocess: ${inStack.map{"$it"}}")
            val outStack = RpnStack()
            var angleIsDegrees = false
            while (inStack.size > 0) {
                val next = inStack.shift()
                printTrace("Trace: next:$next inStack:${inStack.map{"$it"}} " +
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
                    "REG" -> {
                        // if the previous value is a valid index into the registers
                        // hash, set the next value on the out stack to the index.
                        val previous = outStack.peek()
                        if (previous == null)
                            throw RpnParserException("RpnParser:REG:Stack is empth.")
                        if (!previous.original.isValidIndex(1..100))
                            throw RpnParserException("RpnParser:REG:${previous.original}:" +
                                "Not a valid register index.")
                        next.value = previous.value
                        outStack.pop()
                        outStack.push(next)
                    }
                    "STO" -> {
                        val stoExceptionString =
                            "Please select a register between 1 and 100.\nbad index:%s"
                        val idxToken = outStack.pop()
                        val newRegValue = outStack.pop().value
                        val idx : Int
                        try { idx = idxToken.original.toInt()}
                        catch (e:Exception) {
                            throw RpnParserException(
                                    stoExceptionString.format(idxToken.original)
                            )
                        }
                        if (idx < 1 || idx > 100)
                            throw RpnParserException(
                                    stoExceptionString.format(idxToken.original)
                            )
                        registers[idx] = newRegValue
                    }
                    "RCL" -> {
                        val idxToken = outStack.pop() ?:
                            throw RpnParserException("${next.token} index not found.")
                        val idx : Int = try {idxToken.original.toInt()}
                        catch (e:Exception) {
                            throw RpnParserException("${next.token}: "
                                    + "${idxToken.original}: not an integer.")
                        }
                        if (idx !in  1..100) {
                            throw RpnParserException("${next.token}: "
                                    + "index not between 1 and 100:$idx")
                        }
                        outStack.pushD(registers[idx]!!)
                    }
                    "CLR" -> {
                        val reg = outStack.peek()
                        if (reg == null) {
                            throw RpnParserException("RpnParser: no register found.")
                        }
                        else if (reg.token == "REG") {
                            if (registers.containsKey(reg.value.toInt()))
                                registers.remove(reg.value.toInt())
                            else println("RpnParser.register.clear: " +
                                    " Register ${reg.value.toInt()} not in use.")
                            outStack.pop() // remove the index.
                        }
                        else if (reg.token == "STACK") {
                            outStack.clear()
                        }
                        else {
                            throw RpnParserException("CLR: unexpected predicate:\"${reg.token}\"")
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
                    "DUP"                             -> {
                        val t = outStack.pop()
                        outStack.push(t)
                        outStack.push(t)
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
            printTrace("Trace: return: ${outStack.map{it}}")
            return outStack
        }
    }
}
