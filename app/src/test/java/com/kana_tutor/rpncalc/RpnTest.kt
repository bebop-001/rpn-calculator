package com.kana_tutor.rpncalc

import java.util.*

// from https://rosettacode.org/wiki/Parsing/RPN_calculator_algorithm
fun rpnCalculate(expr: String) {
    if (expr.isEmpty()) throw IllegalArgumentException("Expresssion cannot be empty")
    println("For expression = $expr\n")
    println("Token           Action             Stack")
    val tokens = expr.split("\\s+".toRegex())
    val stack = Stack<Double>()
    for (token in tokens) {
        val d = token.toDoubleOrNull()
        if (d != null) {
            stack.push(d)
            println(" $d   Push num onto top of stack  $stack")
        }
        else if ((token.length > 1) || (token !in "+-*/^")) {
            throw IllegalArgumentException("$token is not a valid token")
        }
        else if (stack.size < 2) {
            throw IllegalArgumentException("Stack contains too few operands")
        }
        else {
            val d1 = stack.pop()
            val d2 = stack.pop()
            stack.push(when (token) {
                "+"  -> d2 + d1
                "-"  -> d2 - d1
                "*"  -> d2 * d1
                "/"  -> d2 / d1
                "^" -> Math.pow(d2, d1)
                else -> throw Exception("Unexpected op: $token")
            })
            println(" $token     Apply op to top of stack    $stack")
        }
    }
    println("\nThe final value is ${stack[0]}")
}

fun main(@Suppress("UNUSED_PARAMETER") args: Array<String>) {
    val expr = "3 4 2 * 1 5 - 2 3 ^ ^ / + 6 8 + -"
    rpnCalculate(expr)
}