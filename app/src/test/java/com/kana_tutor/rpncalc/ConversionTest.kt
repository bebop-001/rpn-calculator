package com.kana_tutor.rpncalc

import com.kana_tutor.rpncalc.RpnParser.Companion.rpnCalculate
import com.kana_tutor.rpncalc.RpnStack.Companion.toRpnStack
import java.lang.RuntimeException
import java.lang.StringBuilder
import java.lang.System.exit

class ConversionTest {
    val conversions = Conversions.getInstance()
    var testId = 1
    fun printResult(
        rpnString: String, expectedStack: String, expectedErrors: String,
        verbose: Boolean = false
    ): Boolean {
        val (stack, errors) = rpnCalculate(
                rpnString.toRpnStack(), testId)
        val stackAsStr = stack.toString()
        val errorsFailed = errors != expectedErrors
        val stackFailed = stackAsStr != expectedStack
        val passed = !errorsFailed && !stackFailed
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
            if (stackFailed) print(
                    "    %s:\n\t > \"%s\"\n\t < \"%s\"\n".format(
                            "Sack differed", expectedStack, stackAsStr
                    ))
            if (errorsFailed) print(
                    "    %s:\n\t > \"%s\"\n\t < \"%s\"\n".format(
                            "Errors differed", expectedErrors, errors
                    ))
        }
        testId++
        return passed
    }


    val tests =
            listOf<Pair<String, () -> Pair<Int, Int>>>(
                    /*
                    Pair("test output formatting", ::testFormat),
                    Pair("test math operations", ::testMathOpps),
                    Pair("test stack operations", ::testStackOpps),
                    Pair("test register operations", ::testRegOps),
                    Pair("test save and restore registers", ::saveAndRestoreRegisters),

                     */
            )


}
fun main(args: Array<String>) {
    val conversionTest = ConversionTest()
    with (conversionTest) {
    // Build a usage string.
    val sb = StringBuilder()
            .append("Usage: RpnTestKt indexForTest\n")
            .append("Valid Tests:\n")
    tests.indices.map {
        sb.append("%2d) %s\n".format(it, tests[it].first))
    }

    var totalTests = 0
    var testsPassed = 0
    var testSets = 1
    if (args.size > 0 && args.size != 1) {
        System.err.println(sb.toString())
        exit(1)
    }
    var testNumber = 0
    if (args.size > 0) {
        try {
            testNumber = args[0].toInt()
        }
        catch (e: RuntimeException) {
            sb.append("${args[0]}: $e")
            System.err.println(sb.toString())
            exit(1)
        }
    }
    if (testNumber > 0) {
        val (testString, testFunction) = tests[testNumber]
        println("============= test set ${testNumber}: $testString")
        val (total, passed) = testFunction()
        println("\t$passed of $total PASSED")
        exit(0)
    }
    for ((testString, testFunction) in tests) {
        println("============= test set ${testSets++}: $testString")
        val (total, passed) = testFunction()
        println("\t$passed of $total PASSED")

        totalTests += total; testsPassed += passed
    }
    println("passed $testsPassed of $totalTests: " +
            "%.2f".format(testsPassed / totalTests.toDouble() * 100.0) +
            "%")
}
}
