@file:Suppress("unused", "UNUSED_VARIABLE", "UNUSED_PARAMETER")

package com.kana_tutor.rpncalc
import com.kana_tutor.rpncalc.ConversionTest.testIdentity
import com.kana_tutor.rpncalc.ConversionTest.testSelected
import com.kana_tutor.rpncalc.RpnParser.Companion.rpnCalculate
import com.kana_tutor.rpncalc.RpnStack.Companion.toRpnStack

object ConversionTest {
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
                    testsPassed++
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
        data class Test(val type: String, val from: String, val to: String, val testVal: String, val expected: String)
        val tests = listOf(
            Test("dst", "ft", "in", "1", "12"),
            Test("dst", "ft", "mi", "5280", "1"),
            Test("dst", "km", "mm", "1", "1.0E6"),
            Test("dst", "yd", "km", "1000", "0.9144"),
            Test("dst", "fur", "mi", "1000", "125"),
            Test("dst", "fat", "ft", "1000", "6000"),
            Test("dst", "AU", "mi", "1", "92955807"),
            Test("dst", "LY", "mi", "1", "5.8786254E12"),
            Test("temp", "C", "F", "100", "212"),
            Test("temp", "F", "C", "32", "0"),
            Test("temp", "F", "K", "1000", "810.92778"),
        )
        for (test in tests) {
            totalTests++
            val (type, from, to, testVal, expected) = test
            val cnvt = Conversions.getFromTo(type, from, to)
            if (cnvt != null) {
                val (fromCnvt, toCnvt) = cnvt
                val testStr = "$testVal $fromCnvt $toCnvt"
                // val testStr = "format:fixed:on:5 FORMAT STO $expected $testVal $toCnvt $fromCnvt"
                // println("test string:$testStr")
                val (rpnStack, errors) = rpnCalculate(testStr.toRpnStack())
                if (errors.isNotEmpty())
                    println("test $totalTests: $type:$from:$to Error: $errors")
                val expt = expected.toDouble()
                val result = rpnStack[0].value
                val pctDiff = kotlin.math.abs(
                    if (result == expt) 0.0
                    else ((result - expt) / expt) * 100.0
                )
                if (pctDiff < 10E-7) {
                    if (pctDiff != 0.0)
                        println("pct diff:%1.2e%%".format(pctDiff))
                    println("test $totalTests $type:from $testVal $from to $expected $to PASSED")
                    testsPassed++
                }
                else {
                    println("test $totalTests $type:from $testVal $from to $expected $to FAILED" +
                        "\texpected $expected, received $result\n" +
                        "\t${"pct diff:%5.2f%%".format(pctDiff)}")
                }
            }
        }
        println("testSelected: $testsPassed of $totalTests passed")
        return totalTests == testsPassed
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
                    val (cvtFrom, cvtTo) = cvt
                    val cvtString = "$value format:fixed:on:2 FORMAT STO $cvtFrom $cvtTo"
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
    if (args.size == 1 && args[0] == "-i") interactive()
}
