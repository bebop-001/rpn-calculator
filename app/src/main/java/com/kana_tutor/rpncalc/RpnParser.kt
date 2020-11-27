@file:Suppress("unused", "ObjectPropertyName")

package com.kana_tutor.rpncalc

import com.kana_tutor.rpncalc.RpnMap.Companion.rm
import com.kana_tutor.rpncalc.RpnStack.Companion.peekLast
import java.text.DecimalFormat
import kotlin.math.*

import com.kana_tutor.rpncalc.RpnStack.Companion.rmLast
import com.kana_tutor.rpncalc.RpnToken.Companion.asDouble
import com.kana_tutor.rpncalc.RpnToken.Companion.asIndex
import com.kana_tutor.rpncalc.RpnToken.Companion.fromStorable
import com.kana_tutor.rpncalc.RpnToken.Companion.isIndex
import com.kana_tutor.rpncalc.RpnToken.Companion.isNumber
import com.kana_tutor.rpncalc.RpnToken.Companion.toStorable
import java.lang.RuntimeException

class RpnParserException (message:String) : Exception(message)
const val RADIX = 36
val registerRange = 1..100
class RpnParser private constructor() {
    companion object {
        var registers = RpnMap()
        var printTrace = false
        var rpnError = ""
        private var _trace_  = false
        // debug trace statement.  Turn trace on/off
        // if the trace string is trace:on/trace:off
        private fun trace(trace : String) {
            if (trace == "trace:on") _trace_ = true
            else if (trace == "trace:off") _trace_ = false
            if(_trace_) println(trace)
        }
        private var digitsFormatIsEnabled = true
        var digitsAfterDecimal = 3
            private set
        var commasEnabled = true
            private set

        private var digitsFormat = ""
        private var formatString = ""
        // syntax: "on|off:digits:on|off digitsFormat"
        // for enable:digits after decimal:comma enable
        fun setDigitsFormatting(
            enable : Boolean, digits: Int = 4, commas : Boolean = true
        ) {
            digitsFormatIsEnabled = enable
            if (enable) {
                commasEnabled = commas
                digitsAfterDecimal = digits
                digitsFormat = if (commas) "#,##0" else "#0"
                digitsFormat += if (digits > 0) "." + "0".repeat(digits) else ""
            }
        }
        private fun RpnToken.setDigitsFormatting() {
            formatString = this.token
            formatString.apply {
                val split = split(":")
                when {
                    startsWith("format:off") -> setDigitsFormatting(false)
                    startsWith("format:fixed:on:") && split.size == 4 ->
                        setDigitsFormatting(true,
                                commas = true,
                                digits = split.last().toInt())
                    startsWith("format:fixed:off:") && split.size == 4 ->
                        setDigitsFormatting(true,
                                commas = false,
                                digits = split.last().toInt())
                    else -> throw RuntimeException("unrecognized digit format: $this")
                }
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
        private fun String.mathOp(lVal:RpnToken, rVal:RpnToken) : RpnToken {
            var result = Double.NaN
            val rv = rVal.value; val lv = lVal.value
            try {
                result = when (this) {
                    "+" -> lv + rv
                    "-" -> lv - rv
                    "×", "*" -> lv * rv
                    "÷", "/" -> lv / rv
                    "^" -> lv.pow(rv)
                    else -> {
                        rpnError = "mathOp: %this: unrecognized operator."
                        Double.NaN
                    }
                }
            }
            catch (e:Exception) {
                rpnError = "mathOp $this, $lv, $rv: Exception $e"
            }
            return RpnToken(result)
        }

        // from https://rosettacode.org/wiki/Parsing/RPN_calculator_algorithm
        fun rpnCalculate(
            inStack : RpnStack,
            @Suppress("UNUSED_PARAMETER") testId:Int = -1
        ): Pair<RpnStack, String> {
            val outStack = RpnStack()
            fun Double.degreesToRadians(): Double = this * PI / 180
            fun Double.radiansToDegrees(): Double  = this * 180 / PI
            rpnError = ""
            trace("Trace: For: ${inStack.map{"$it"}}")
            trace("Trace: after preprocess: ${inStack.map{"$it"}}")
            next@while (rpnError.isEmpty() && inStack.size > 0) {
                val current = inStack.removeAt(0)!!
                if (current.isNumber()!!) {
                    current.token = current.value.toFormattedString()
                    outStack.add(current)
                    continue@next
                }
                if (current.token.contains(":")) {
                    val tok = current.token
                    try {
                        when {
                            tok == "trace:on" || tok == "trace:off" -> trace(tok)
                            tok.startsWith("STORABLE:") -> {
                                val restored =  current.fromStorable()
                                if (restored != null) {
                                    val t = restored.token.split("=")
                                    if (t.size == 2 && t[0] == "REG" && t[1].toIntOrNull() != null) {
                                        when (t[1].toInt()) {
                                            in registerRange ->
                                                registers[t[1].toInt()] = restored
                                            -1 -> outStack.add(RpnToken(restored.value))
                                            else -> rpnError = "Bad storable token: $current"
                                        }
                                    }
                                    else outStack.add(restored)
                                }
                                else rpnError = "Bad storable token: $current"
                            }
                            tok.startsWith("format:") -> outStack.add(current)
                            else -> rpnError = "Unrecognized : operator: \"$tok\""
                        }
                    }
                    catch(e:Exception) {
                        rpnError = "Parsing \"$tok\"\nError: $e"
                    }
                    continue@next
                }

                trace("Trace: processing \"${current.token}\"\n\t" +
                        "in  = ${inStack.map{"$it"}}\n\t" +
                        "out = ${outStack.map{"$it"}}")
                when (current.token) {
                    "CHS" -> {
                        if (outStack.isNotEmpty() && outStack.peekLast().isNumber()!!) {
                            val d = outStack.rmLast()!!.value
                            outStack.add(RpnToken(d * -1))
                        }
                        else rpnError = "CHS:$rpnError"
                    }
                    "DROP" -> {
                        if (outStack.peekLast() != null)
                            outStack.rmLast()
                        else rpnError = "DROP:$rpnError"
                    }
                    "SWAP" -> {
                        if (outStack.size >= 2) {
                            outStack.add(outStack.rmLast(-1))
                        }
                        else rpnError = "SWAP FAILED: less than two objects on stack."
                    }
                    // op that expects two floats on stack.
                    "+", "-", "×", "*", "÷", "/", "^" -> {
                        if (outStack.size >= 2) {
                            var rVal = outStack.rmLast()!!
                            var lVal = outStack.rmLast()!!
                            if (rVal.token == "REG") {
                                if (outStack.isEmpty())
                                    rpnError = "${lVal.token} REG ${current.token}: Stack is empty."
                                else {
                                    val idx = lVal.asIndex()!!
                                    rVal = outStack.removeLast()
                                    lVal  =
                                            if (registers.containsKey(idx))
                                                registers[idx]!!
                                            else RpnToken(0.0)
                                    registers[idx] = current.token.mathOp(lVal, rVal)
                                }
                            }
                            else if (rVal.isNumber()!! && lVal.isNumber()!!) {
                                outStack.add(current.token.mathOp(lVal, rVal))
                            }
                            else {
                                println("${lVal.token}:${rVal.token}")
                            }
                        }
                        else rpnError = "\"${current.token}: need at least 2 values."
                    }
                    "STACK", "LIST", "ALL", "FORMAT" -> { outStack.add(current)}
                    "REG" -> {
                        if (outStack.isNotEmpty()) {
                            val lastElement = outStack.peekLast()!!
                            if (lastElement.token == "ALL" || lastElement.isIndex()!!)
                                outStack.add(current)
                            else rpnError = "${lastElement.token} REG: ${RpnToken.error}"
                        }
                        else rpnError = "REG: stack is empty"
                    }
                    "CLR" -> {
                        var op = outStack.rmLast()
                        if (op != null) {
                            if (op.token == "STACK") {
                                outStack.clear()
                            }
                            else if (op.token == "REG") {
                                // REG checked so we know this is all or an index.
                                op = outStack.rmLast()!!
                                if (op.token == "ALL") {
                                    registers.clear()
                                }
                                else if (op.isIndex()!!) {
                                    if (registers.rm(op.asIndex()!!) == null)
                                        rpnError = "${op.asIndex()} REG CLR: register is empty"
                                }
                                else rpnError = "${op.token} REG CLR: unexpected ${op.token}"
                            }
                            else rpnError = "CLR: unexpected token: ${op.token}"
                        }
                        else rpnError = "CLR: empty stack"
                    }
                    "STO", "RCL", "STORABLE" -> {
                        if (outStack.size >= 1) {
                            val v1 = outStack.rmLast()!!
                            if (current.token == "STORABLE" && v1.token == "ALL") {
                                for(i in outStack.indices)
                                    outStack[i] = outStack[i].toStorable()
                            }
                            else if (outStack.size >= 1) {
                                val v2 = outStack.rmLast()!!
                                when (current.token) {
                                    "STO" -> {
                                        when {
                                            v2.isIndex()!! -> registers.set(
                                                v2.asIndex()!!,
                                                outStack.rmLast()!!)
                                            v1.token == "FORMAT" ->
                                                v2.setDigitsFormatting()
                                            else -> rpnError =
                                                "${v2.token} STO: bad command."
                                                
                                        }
                                    }
                                    // Recall a register or "ALL"
                                    "RCL" -> {
                                        when {
                                            v2.token == "ALL" -> {
                                                registers.keys.map { outStack.add(registers[it]) }
                                            }
                                            v2.token == "FORMAT" -> outStack.add(RpnToken(formatString))
                                            v2.isIndex()!! -> {
                                                val idx = v2.asIndex()!!
                                                if (registers.containsKey(idx))
                                                    outStack.add(registers[idx])
                                                else rpnError = "$idx RCL: register[$idx] is empty."
                                            }
                                            else -> rpnError = "${v2.token} REG RCL: ${v2.token}: " +
                                                    "expected index or \"ALL\":" + rpnError
                                        }
                                    }
                                    "STORABLE" -> {
                                        registers.keys.map {
                                            outStack.add(registers[it]!!.toStorable(it))
                                        }
                                    }
                                }
                            }
                        }
                        else rpnError = "${current.token}: need at least 2 elements on stack. found ${outStack.size}"
                    }
                    "RAD", "DEG" -> {
                        outStack.add(current)
                    }
                        "SIN", "COS", "TAN" -> {
                            val degOrRad = outStack.peekLast()?.token
                            var angle = outStack.peekLast(-1)?.asDouble()
                            if (degOrRad != null && angle != null) {
                                angle = when (degOrRad) {
                                    "RAD" -> angle
                                    "DEG" -> angle.degreesToRadians()
                                    else -> null
                                }
                                if (angle == null)
                                    rpnError = "${current.token}: \"$degOrRad\": not DEG or RAD: $degOrRad"
                                else {
                                    val result = when (current.token) {
                                        "SIN" -> sin(angle)
                                        "COS" -> cos(angle)
                                        "TAN" -> tan(angle)
                                        else -> {
                                            0.0 /* can't happen. */
                                        }
                                    }
                                    outStack.rmLast()
                                    outStack.setLast(RpnToken(result))
                                }
                            }
                            else rpnError = "$current.token:$rpnError"
                        }
                        "ASIN", "ACOS", "ATAN" -> {
                            var degOrRad = outStack.peekLast()?.token
                            val value = outStack.peekLast(-1)?.asDouble()
                            if (degOrRad != null && value != null) {
                                degOrRad = when (degOrRad) {
                                    "RAD" -> degOrRad
                                    "DEG" -> degOrRad
                                    else -> null
                                }
                                if (degOrRad == null)
                                    rpnError = "${current.token}: \"$degOrRad\": not DEG or RAD: $degOrRad"
                                else {
                                    var angle = when (current.token) {
                                        "ASIN" -> asin(value)
                                        "ACOS" -> acos(value)
                                        "ATAN" -> atan(value)
                                        else -> {
                                            0.0 /* can't happen. */
                                        }
                                    }
                                    if (degOrRad == "DEG")
                                        angle = angle.radiansToDegrees()
                                    outStack.rmLast()
                                    outStack.setLast(RpnToken(angle))
                                }
                            }
                        }
                        "DUP" -> {
                            val last = outStack.peekLast()
                            if (last != null)
                                outStack.add(last)
                            else
                                rpnError = "DUP:empty stack"
                        }
                        // push an empty token on the stack. ENTR caused call
                        // to parser.  That's all that we need.
                        "ENTR" -> {
                        }
                        // push the value on the stack.  It's probably a number.
                        else -> {
                            outStack.add(current)
                            rpnError = "Unrecognized operation: ${current.token}"
                        }
                    }
            }
            trace("Trace: return: ${outStack.map{it}}")
            return Pair(outStack, rpnError)
        }
    }
}
