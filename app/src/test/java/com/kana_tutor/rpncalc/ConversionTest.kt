package com.kana_tutor.rpncalc

import com.kana_tutor.rpncalc.ConversionTest.testIdentity
import com.kana_tutor.rpncalc.ConversionTest.testMe
import com.kana_tutor.rpncalc.ConversionTest.testSelected
import com.kana_tutor.rpncalc.RpnParser.Companion.rpnCalculate
import com.kana_tutor.rpncalc.RpnStack.Companion.toRpnStack
import java.lang.Exception
import java.lang.Math.abs
import java.lang.StringBuilder
import java.lang.System.exit
import kotlin.system.exitProcess

object ConversionTest {
    val conversions = Conversions
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
    // test convert to/from same unit and difference should be 0.
    fun testIdentity() :Boolean {
        var totalTests = 0
        var testsPassed = 0
        val tests = Conversions.listConversions().sorted()
        for(test in tests) {
            totalTests++
            val matchResult =
                    "^([^:]+):(\\S+)\\s+.*([A-Z][^:]+):(.*)$".toRegex()
                            .find(test)
            val (matched, type, typeTag, instance, instanceTag)
                = matchResult!!.groupValues
            val cnvt = Conversions.getFromTo(typeTag, instanceTag, instanceTag)
            if (cnvt != null) {
                val (to, from) = cnvt
                val testStr = "format:fixed:on:2 FORMAT STO 100 DUP $from $to -"
                val (rpnStack, errors) = rpnCalculate(testStr.toRpnStack())
                if (rpnStack.size == 1 && rpnStack[0].value == 0.0)
                    testsPassed++;
                else
                    println("testIdenty:test $totalTests:$test FAILED")
            }
            else println("testIdenty:test $totalTests:$test: getToFrom FAILED.")
        }
        println("testIdenty:$testsPassed of $testsPassed passed")
        return totalTests == testsPassed
    }
    fun testSelected() : Boolean {
        var totalTests = 0
        var testsPassed = 0
        data class Test (val type:String, val from:String, val to:String, val testVal : String, val expected:String)
        val tests = listOf(
            Test("dst", "ft", "in", "1","12"),
            Test("dst", "AU", "mi", "1","92955807"),
            Test("dst", "LY", "mi", "1","5.8786254E12"),
            Test("dst", "ft", "mi", "5280","1"),
        )
        for (test in tests) {
            totalTests++
            val (type, to, from, testVal, expected) = test
            val cnvt = Conversions.getFromTo(type, from, to)
            if (cnvt != null) {
                val (fromCnvt, toCnvt) = cnvt
                var testStr = "$testVal $toCnvt $fromCnvt"
                // val testStr = "format:fixed:on:5 FORMAT STO $expected $testVal $toCnvt $fromCnvt"
                // println("test string:$testStr")
                var (rpnStack, errors) = rpnCalculate(testStr.toRpnStack())
                if (errors.isNotEmpty())
                    println("test $totalTests: $type:$from:$to Error: $errors")
                println("testSelected:test $totalTests:$test: PctDiff:${"%.5f".format(rpnStack[0].value)}%")
                val expt = expected.toDouble()
                val result = rpnStack[0].value
                val pctDiff = abs((result - expt) / expt) * 100.0
                if (pctDiff < 10E-7) {
                    if (pctDiff != 0.0)
                        println("pct diff:%1.2e%%".format(pctDiff))
                    println("test $totalTests $type:$from:$to PASSED")
                    testsPassed++
                }
                else println("test $totalTests $type:$from:$to FAILED.\n" +
                    "expecred $expected, received $result")
            }
        }
        println("testSelected: $testsPassed of $totalTests passed")
        return totalTests == testsPassed
    }


    val tests =
            listOf<Pair<String, () -> Pair<Int, Int>>>(
                    /*
                    Pair("test output formatting", ::testFormat),
                    Pair("test math operations", ::testMathOps),
                    Pair("test stack operations", ::testStackOps),
                    Pair("test register operations", ::testRegOps),
                    Pair("test save and restore registers", ::saveAndRestoreRegisters),

                     */
            )

    fun testMe(testNumber: Int = -1) {
            // Build a usage string.
            val sb = StringBuilder()
                    .append("Usage: RpnTestKt indexForTest\n")
                    .append("Valid Tests:\n")
            tests.indices.map {
                sb.append("%2d) %s\n".format(it, tests[it].first))

            var totalTests = 0
            var testsPassed = 0
            var testSets = 1
            var testNumber = 0
            if (testNumber > 0) {
                val (testString, testFunction) = tests[testNumber]
                println("============= test set ${testNumber}: $testString")
                val (total, passed) = testFunction()
                println("\t$passed of $total PASSED")
                exitProcess(0)
            }
            else {
                for ((testString, testFunction) in tests) {
                    println("============= test set ${testSets++}: $testString")
                    val (total, passed) = testFunction()
                    println("\t$passed of $total PASSED")

                    totalTests += total; testsPassed += passed
                }
            }
            println("passed $testsPassed of $totalTests: " +
                    "%.2f".format(testsPassed / totalTests.toDouble() * 100.0) +
                    "%")
        }
    }
}
fun interactive() {
    val conversions = Conversions
    fun quit(c:String) : Boolean {
        println("quiting...")
        return true
    }
    fun list(s:String) : Boolean {
        val conversionsList = conversions.listConversions()
        println(conversionsList.joinToString("\n"))
        return true
    }
    fun convert(s:String) : Boolean {
        val args = s.split("\\s+".toRegex())
        println("$s:$args")
        if (args.size != 5)
            println("bad cmd. Expected something like:" +
                "\n\t\"cnvt dst ft mi\" to convert length feet to miles." +
                "\n\tgot \"s\"")
        else {
            val (cmd, type, value, from, to) = args
            var goodNumber = false
            try { value.toDouble(); goodNumber = true }
            catch (e:Exception) {println("bad number:$e")}
            if (goodNumber) {
                val cvt = conversions.getFromTo(type, from, to)
                if (cvt == null)
                    println("can't convert $type:$from:$to")
                else {
                    val (from, to) = cvt
                    val cvtString = "$value format:fixed:on:2 FORMAT STO $from $to"
                    println("cvtString = \"$cvtString\"")

                    val stk = rpnCalculate(cvtString.toRpnStack())
                    println("$type:$from:$to = $stk")
                }
            }
        }
        return true
    }
    var cmd = ""
    val cmdTable : Map<String, Pair<String, (String) -> Boolean>> = mapOf(
            "l" to Pair("List conversions" ,::list),
            "q" to Pair("Quit, ", ::quit),
            "cnvt" to Pair("Convert", ::convert),

    )
    while (cmd != "q") {
        print(" > ")
        cmd = readLine().toString().trim()
        val rv = when  {
            cmd == "q" -> cmdTable.getValue(cmd).second.invoke(cmd)
            cmd == "l" -> cmdTable.getValue(cmd).second.invoke(cmd)
            cmd.startsWith("cnvt") -> cmdTable.getValue("cnvt").second.invoke(cmd)
            else -> {
                println (
                    "bad cmd: \"$cmd\".  " +
                    "valid commands are:\"${
                        cmdTable.keys.sorted()
                            .joinToString(", ") {
                                "\"$it\":${cmdTable.getValue(it).first}" 
                            }
                    }\""
                )
            }
        }
    }
}
fun main(args: Array<String>) {
    testIdentity()
    testSelected()
    exitProcess(0)
    if (args.size == 1) {
        if (args[0] == "-i") interactive()
        else if (args[0] == "-t") ConversionTest.testMe(-1)
        else if ("^\\d+$".toRegex().matches(args[0])) testMe(args[0].toInt())
    }
}
