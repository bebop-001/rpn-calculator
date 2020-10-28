package com.kana_tutor.rpncalc

import com.kana_tutor.rpncalc.ui.rpnCalculate
import java.util.*

// interactive command line interface to rpnCalc
fun main(@Suppress("UNUSED_PARAMETER") args: Array<String>) {
    var expr = "3 4 2 * 1 5 - 2 3 ^ ^ / + 6 8 + - 2 7 +"
    while (true) {
        try {
            expr = rpnCalculate(expr)
        }
        catch (e: Exception) {
            println("rpnCalculate Exception : $e")
        }
        expr += " "
        print(expr)
        expr += readLine()
    }
}
