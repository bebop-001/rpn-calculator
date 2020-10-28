package com.kana_tutor.rpncalc.model

import android.content.SharedPreferences
import java.text.DecimalFormat
import java.util.*

/**
 * This class includes some methods to support the logic of the RPN calc.
 * @author Algenis Eduardo Volquez <evolquez></evolquez>@gmail.com>
 * https://github.com/kana_tutor/
 * @version 1.0
 */
class Rpn {
    private var lastOperation: String? = null
    private val symbols = arrayOf("/", "x", "-", "+")
    private var symbolPosition = -1

    /**
     * Format the input on calculator
     * @param input
     * @return String
     */
    fun formatInput(input: Array<String>): String {
        var inputFormatted = ""
        var cont = 0
        var number: Double
        for (value in input) {
            number = value.toDouble()
            cont += 1
            inputFormatted += number
            if (cont < input.size) {
                inputFormatted += "\n"
            }
        }
        return inputFormatted
    }

    /**
     * Changes the symbol of the last input (- +)
     * @param values
     * @return String
     */
    fun changeInputSymbol(values: Array<String>): String {
        var input = ""
        var value = values[values.size - 1].toDouble()
        if (value > 0 || value < 0) {
            value = value * -1
            values[values.size - 1] = value.toString()
            input = formatInput(values)
        }
        return input
    }

    /**
     * Remove the last number input by the user
     * @param input
     * @return String
     */
    fun delete(input: String): String {
        var input = input
        input = input.substring(0, input.length - 1)
        return input
    }

    /**
     * Proccess the operation taken, then return the formatted text for the input text view
     * @param input
     * @param operatorSymbol
     * @param sharedPreferences
     * @return String
     */
    fun proccess(input: Array<String>, operatorSymbol: String?, sharedPreferences: SharedPreferences): String {

        // First, format the input
        var input = input
        input = formatInput(input).split("\n".toRegex()).toTypedArray()
        if (input.size > 1) {
            val num1 = input[input.size - 2].toDouble()
            val num2 = input[input.size - 1].toDouble()
            var rs = 0.0
            when (operatorSymbol) {
                "\u00F7" -> {
                    rs = num1 / num2
                    symbolPosition = 0
                }
                "\u00D7" -> {
                    rs = num1 * num2
                    symbolPosition = 1
                }
                "-" -> {
                    rs = num1 - num2
                    symbolPosition = 2
                }
                "+" -> {
                    rs = num1 + num2
                    symbolPosition = 3
                }
            }

            // Prepare string to history format
            lastOperation = applyFormat(num1) + symbols[symbolPosition] + applyFormat(num2) + ":" + applyFormat(rs) + ";"
            saveHistory(sharedPreferences)

            // Add the result
            input = Arrays.copyOf(input, input.size - 1)
            input[input.size - 1] = rs.toString()
        }
        return formatInput(input)
    }

    /**
     * Save the last operation to SharedPreferences
     * @param sharedPreferences
     * @return boolean
     */
    private fun saveHistory(sharedPreferences: SharedPreferences): Boolean {

        // First read the data on sharedPreferences
        var history = sharedPreferences.getString(KEY, "")
        history += lastOperation

        // Now add new data to shared preferences
        val editor = sharedPreferences.edit()
        editor.putString(KEY, history)
        return editor.commit() // Commit changes and return
    }

    /**
     * Format numbers to show on history
     * @param number
     * @return String
     */
    private fun applyFormat(number: Double): String {
        val nFormat = DecimalFormat("#,###.#")
        return nFormat.format(number)
    }

    companion object {
        const val KEY = "RPN_HISTORY"

        /**
         * Prepare the history to show in items on history actity
         */
        @JvmStatic
        fun loadHistoryInArray(history: String): ArrayList<HistoryHolder> {
            val arrayHistory = ArrayList<HistoryHolder>()
            if (!history.contains("NONE")) {
                val items = history.split(";".toRegex()).toTypedArray()
                var operation: Array<String>
                for (item in items) {
                    if (item.contains(":")) {
                        operation = item.split(":".toRegex()).toTypedArray()
                        if (operation[0].contains("/")) {
                            operation[0] = operation[0].replace("/", "\u00F7")
                        }
                        else if (operation[0].contains("x")) {
                            operation[0] = operation[0].replace("x", "\u00D7")
                        }
                        arrayHistory.add(HistoryHolder(operation[0], operation[1]))
                    }
                }
            }
            return arrayHistory
        }
    }
}