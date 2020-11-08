package com.kana_tutor.rpncalc

import com.kana_tutor.rpncalc.RpnParser.Companion.rpnCalculate
import java.util.*

var rpnStack = Stack<RpnParser.RpnToken>()
// interactive command line interface to rpnCalc
fun main(@Suppress("UNUSED_PARAMETER") args: Array<String>) {
    var expr = "3 4 2 * 1 5 - 2 3 ^ ^ / + 6 8 + - 2 7 +"
    while (true) {
        try {
            rpnStack = rpnCalculate(rpnStack)
        }
        catch (e: Exception) {
            println("rpnCalculate Exception : $e")
        }
        expr += " "
        print(expr)
        expr += readLine()
    }
}
