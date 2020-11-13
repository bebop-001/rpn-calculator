package com.kana_tutor.rpncalc

import com.kana_tutor.rpncalc.RpnParser.Companion.rpnCalculate
import com.kana_tutor.rpncalc.RpnParser.RpnStack.Companion.toRpnStack



fun printResult(testName:String, expected:String, found:String) : Boolean{
    val rv = expected == found
    print("\t\"$testName\": ")
    if (rv) {
        println("PASSED")
    }
    else {
        println("FAILED\n" +
                "Expected\n\t\t\"$expected\"\n" +
                "Received\n\t\t\"$found\"")
    }
    return rv
}
fun testCalculate(testName:String) :Pair<Int,Int>{
    var totalTests = 0
    var testsPassed = 0
    var rpnStack = rpnCalculate("1 2 3".toRpnStack())
    totalTests++
    if (printResult("check stack order",
                    rpnStack.toString(),
                    rpnCalculate(rpnStack).toString())
    ) testsPassed++
    totalTests++
    if (printResult("test CHS",
                    "[-1.000:-1.0:-1.000]",
                    rpnCalculate("1 CHS".toRpnStack()).toString())
    ) testsPassed++
    totalTests++
    if (printResult("test 1 + 3",
                    "[4.000:4.0:4.000]",
                    rpnCalculate("1 3 +".toRpnStack()).toString())
    ) testsPassed++
    totalTests++
    if (printResult("test 1 3 4 + +",
                    "[8.000:8.0:8.000]",
                    rpnCalculate("1 3 4 + +".toRpnStack()).toString())
    ) testsPassed++
    totalTests++
    if (printResult("test 90 DEG SIN",
                    "[1.000:1.0:1.000]",
                    rpnCalculate("90 DEG SIN".toRpnStack()).toString())
    ) testsPassed++
    totalTests++
    if (printResult("test π 2 / RAD SIN",
                    "[1.000:1.0:1.000]",
                    rpnCalculate("π 2 / RAD SIN".toRpnStack()).toString())
    ) testsPassed++
    totalTests++
    if (printResult("test 15 2 STO 2 RCL",
                    "[15.000:15.0:15.000]",
                    rpnCalculate("15 2 STO 2 RCL".toRpnStack()).toString())
    ) testsPassed++
    totalTests++
    if (printResult("test 1 2 SWAP",
                    "[2.000:2.0:2, 1.000:1.0:1]",
                    rpnCalculate("1 2 SWAP".toRpnStack()).toString())
    ) testsPassed++
    totalTests++
    if (printResult("test 1 2 DROP",
                    "[1.000:1.0:1]",
                    rpnCalculate("1 2 DROP".toRpnStack()).toString())
    ) testsPassed++
    totalTests++
    if (printResult("test 27 DUP",
                    "[27.000:27.0:27, 27.000:27.0:27]",
                    rpnCalculate("27 DUP".toRpnStack()).toString())
    ) testsPassed++
    totalTests++
    if (printResult("test 1 2 3 4 5 STACK CLR 15",
                    "[15.000:15.0:15]",
                    rpnCalculate("1 2 3 4 5 STACK CLR 15".toRpnStack()).toString())
    ) testsPassed++
    totalTests++
    if (printResult("test 100 9 * 5 / 32 i.e. C -> F",
                    "[212.000:212.0:212.000]",
                    rpnCalculate("100 9 * 5 / 32 +".toRpnStack()).toString())
    ) testsPassed++


    return Pair(totalTests, testsPassed)
}
val x : (String) -> Pair<Int, Int> = ::testCalculate
val tests =
    listOf<Pair<String, (String) -> Pair<Int, Int>>>(
        Pair("testStackOrder", ::testCalculate),
)

fun main(@Suppress("UNUSED_PARAMETER") args: Array<String>) {
    var totalTests = 0;
    var testsPassed = 0
    var testSets = 1

    for ((testString, testFunction) in tests) {
        println("============= test set ${testSets++}")
        val (total, passed) = testFunction(testString)
        println("\t$total of $passed PASSED")

        totalTests += total; testsPassed += passed
    }
    println ("passed $testsPassed of $totalTests: " +
            "%.2f".format(testsPassed / totalTests.toDouble() * 100.0) +
             "%")
}

