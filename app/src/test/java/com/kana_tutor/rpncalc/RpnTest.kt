package com.kana_tutor.rpncalc

import com.kana_tutor.rpncalc.RpnParser.Companion.rpnCalculate
import com.kana_tutor.rpncalc.RpnStack.Companion.toRpnStack


var testId = 1
fun printResult(
    rpnString: String, expectedStack: String, expectedErrors:String,
        verbose:Boolean = false
    ) : Boolean{
    val (stack, errors) = rpnCalculate(
            rpnString.toRpnStack(), testId)
    val stackAsStr = stack.toString()
    val errorsFailed = errors != expectedErrors
    val stackFailed = stackAsStr != expectedStack
    val passed  = !errorsFailed && !stackFailed
    if (verbose) println(arrayOf(
            "RECEIVED",
            "\tRPN:\"$rpnString\"",
            "\tStack: \"$stackAsStr\"",
            "\tErrors:\"$errors\"")
            .joinToString("\n")
    )
    if (passed)
        println("test $testId \"$rpnString\" PASSED")
    else {
        println("test $testId \"$rpnString\" FAILED")
        if (stackFailed) println (
            "\tSack differed:\n" +
            "\t\tExpected \"$expectedStack\" vs \"$stackAsStr\"")
        if (errorsFailed) println (
            "\tErrors differed:\n" +
            "\t\tExpected \"$expectedErrors\" vs \"$errors\"")
    }
    testId++
    return passed
}
fun testFormat() :Pair<Int,Int> {
    var totalTests = 0
    var testsPassed = 0
    totalTests++
    if (printResult("format:off FORMAT STO 1e4 3 /",
                    "[3333.3333333333335:3333.3333333333335]",
                    "")
    ) testsPassed++
    totalTests++
    if (printResult("format:fixed:off:2 FORMAT STO 1e4 3 /",
                    "[3333.33:3333.3333333333335]",
                    "")
    ) testsPassed++
    totalTests++
    if (printResult("format:fixed:on:0 FORMAT STO 1e4 3 /",
                    "[3,333:3333.3333333333335]",
                    "")
    ) testsPassed++
    totalTests++
    if (printResult("format:fixed:on:2 FORMAT STO 1.564",
                    "[1.56:1.564]",
                    "")
    ) testsPassed++
    totalTests++
    if (printResult("format:fixed:on:2 FORMAT STO 1.565",
                    "[1.56:1.565]",
                    "")
    ) testsPassed++
    totalTests++
    if (printResult("format:fixed:on:2 FORMAT STO  1.566",
                    "[1.57:1.566]",
                    "")
    ) testsPassed++
    totalTests++
    if (printResult("format:fixed:on:2 FORMAT STO FORMAT RCL 1.566",
                    "[1.57:1.566]",
                    "")
    ) testsPassed++


    // set format for remainder of tests.
    rpnCalculate("format:fixed:on:2 FORMAT STO".toRpnStack())
    totalTests++
    if (printResult(" 1 2 3 3.5 -2 -2.5 2E3 2E-5 -2E-5",
                    "[1.00:1.0, 2.00:2.0, 3.00:3.0, "
                            + "3.50:3.5, -2.00:-2.0, -2.50:-2.5, "
                            + "2,000.00:2000.0, 0.00:2.0E-5, -0.00:-2.0E-5]",
                    "")
    ) testsPassed++
    return Pair(totalTests, testsPassed)
}

fun testMathOpps() :Pair<Int,Int>{
    var totalTests = 0
    var testsPassed = 0

    totalTests++
    if (printResult("1 CHS 999",
                    "[-1.00:-1.0, 999.00:999.0]",
                    "")
    ) testsPassed++
    totalTests++
    if (printResult("1 3 - 999",
                    "[-2.00:-2.0, 999.00:999.0]",
                    "")
    ) testsPassed++
    totalTests++
    if (printResult("1 3 4 + + 999",
                    "[8.00:8.0, 999.00:999.0]",
                    "")
    ) testsPassed++
    totalTests++
    if (printResult("90 DEG SIN 999",
                    "[1.00:1.0, 999.00:999.0]",
                    "")
    ) testsPassed++
    totalTests++
    if (printResult("90 DEG SIN RAD ASIN 999",
                    "[1.57:1.5707963267948966, 999.00:999.0]",
                    "")
    ) testsPassed++
    totalTests++
    if (printResult("π 2 / RAD SIN DEG ASIN 999",
                    "[90.00:90.0, 999.00:999.0]",
                    "")
    ) testsPassed++
    return Pair(totalTests, testsPassed)
}
fun testStackOpps() :Pair<Int,Int>{
    var totalTests = 0
    var testsPassed = 0

    totalTests++
    if (printResult("1 2 SWAP 999",
                    "[2.00:2.0, 1.00:1.0, 999.00:999.0]",
                    "")
    ) testsPassed++
    totalTests++
    if (printResult("1 2 DROP 999",
                    "[1.00:1.0, 999.00:999.0]",
                    "")
    ) testsPassed++
    totalTests++
    if (printResult("27 DUP 999",
                    "[27.00:27.0, 27.00:27.0, 999.00:999.0]",
                    "")
    ) testsPassed++
    totalTests++
    if (printResult("1 2 3 4 5 STACK CLR 999",
                    "[999.00:999.0]",
                    "")
    ) testsPassed++
    totalTests++
    if (printResult("100 9 * 5 / 32 +",
                    "[212.00:212.0]",
                    "")
    ) testsPassed++
    return Pair(totalTests, testsPassed)
}
fun testRegOps() :Pair<Int,Int>{
    var totalTests = 0
    var testsPassed = 0
    RpnParser.registers.clear()

    totalTests++
    if (printResult("777 ALL REG CLR 999",
                    "[777.00:777.0, 999.00:999.0]",
                    "")
    ) testsPassed++
    totalTests++
    if (printResult("15 2 REG STO 2 REG RCL 999",
                    "[REG=2:15.0, 999.00:999.0]",
                    "")
    ) testsPassed++
    totalTests++
    if (printResult("777 2 REG CLR 2 REG RCL 999",
                    "[777.00:777.0]",
                    "2 RCL: register[2] is empty.")
    ) testsPassed++
    totalTests++
    if (printResult("777 888 2 REG STO 123 99 REG STO ALL REG RCL 999",
                    "[777.00:777.0, REG=2:888.0, REG=99:123.0, 999.00:999.0]",
                    "")
    ) testsPassed++
    totalTests++
    if (printResult("777 100 REG CLR 999",
                    "[777.00:777.0]",
                    "100 REG CLR: register is empty")
    ) testsPassed++
    totalTests++
    if (printResult("777 101 REG CLR 999",
                    "[777.00:777.0, 101.00:101.0]",
                    "101.00 REG: isIndex: 101.0 out of range 1..100")
    ) testsPassed++
    totalTests++
    if (printResult("ALL REG CLR 20 5 REG STO 10 5 REG / 999 5 REG RCL",
                    "[999.00:999.0, REG=5:2.0]",
                    "")
    ) testsPassed++
    totalTests++
    if (printResult("ALL REG CLR 20 6 REG / 6 REG RCL 999",
                    "[REG=6:0.0, 999.00:999.0]",
                    "")
    ) testsPassed++

    return Pair(totalTests, testsPassed)
}
fun saveAndRestoreRegisters() :Pair<Int,Int>{
    var totalTests = 0
    var testsPassed = 0
    RpnParser.registers.clear()

    totalTests++
    if (printResult("777 888 2 REG STO 123 99 REG STO 27 27 REG STO ALL REG STORABLE 999",
    "[777.00:777.0, STORABLE:zc3tr19uvojk:2:REG=2:NaN, STORABLE:z5w0yevhfk00:27:REG=27:NaN, STORABLE:z8n3w9eryebk:99:REG=99:NaN, 999.00:999.0]",
    "")
    ) testsPassed++
    totalTests++
    if (printResult("8888 ALL REG CLR STORABLE:z5w0yevhfk00:27 " +
                    "STORABLE:z8n3w9eryebk:99 STORABLE:zc3tr19uvojk:2 ALL REG RCL 9999",
                    "[8,888.00:8888.0, REG=2:888.0, REG=27:27.0, REG=99:123.0, 9,999.00:9999.0]",
                    "")
    ) testsPassed++
    totalTests++
    if (printResult("8888 ALL REG CLR STORABLE:z5w0yevhfk00:27 " +
                    "STORABLE:z8n3w9eryebk:99 STORABLE:zc3tr19uvojk:2 ALL REG RCL 9999",
                    "[8,888.00:8888.0, REG=2:888.0, REG=27:27.0, REG=99:123.0, 9,999.00:9999.0]",
                    "")
    ) testsPassed++
    totalTests++
    if (printResult("1 2 3 4 5 6 ALL STORABLE 999 ",
                    "[STORABLE:z045v4fok2yo:-1:1.00:NaN, STORABLE:z1ci99jj7474:-1:2.00:NaN, STORABLE:z1yogc3gimtc:-1:3.00:NaN, STORABLE:z2kunendu5fk:-1:4.00:NaN, STORABLE:z2vxqxxchwqo:-1:5.00:NaN, STORABLE:z370uh7b5o1s:-1:6.00:NaN, 999.00:999.0]",
                    "")
    ) testsPassed++
    totalTests++
    if (printResult("STORABLE:z045v4fok2yo:-1:1.00:NaN, STORABLE:z1ci99jj7474:-1:2.00:NaN, STORABLE:z1yogc3gimtc:-1:3.00:NaN, STORABLE:z2kunendu5fk:-1:4.00:NaN, STORABLE:z2vxqxxchwqo:-1:5.00:NaN, STORABLE:z370uh7b5o1s:-1:6.00:NaN, 999",
                    "[1.00:1.0, 2.00:2.0, 3.00:3.0, 4.00:4.0, 5.00:5.0, 6.00:6.0, 999.00:999.0]",
            "")
    ) testsPassed++

    return Pair(totalTests, testsPassed)
}

val tests =
    listOf<Pair<String, () -> Pair<Int, Int>>>(
            Pair("test output formatting", ::testFormat),
            Pair("test math operations", ::testMathOpps),
            Pair("test stack operations", ::testStackOpps),
            Pair("test register operations", ::testRegOps),
            Pair("test save and restore registers", ::saveAndRestoreRegisters),
)

fun main(@Suppress("UNUSED_PARAMETER") args: Array<String>) {
    var totalTests = 0
    var testsPassed = 0
    var testSets = 1

    for ((testString, testFunction) in tests) {
        println("============= test set ${testSets++}: $testString")
        val (total, passed) = testFunction()
        println("\t$total of $passed PASSED")

        totalTests += total; testsPassed += passed
    }
    println ("passed $testsPassed of $totalTests: " +
            "%.2f".format(testsPassed / totalTests.toDouble() * 100.0) +
             "%")
}

