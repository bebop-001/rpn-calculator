@file:Suppress("unused", "unused", "unused")

package com.kana_tutor.rpncalc.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.kana_tutor.rpncalc.R
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.math.pow

// from https://rosettacode.org/wiki/Parsing/RPN_calculator_algorithm
fun rpnCalculate(expr: String) : String {
    fun Stack<String>.popD():Double {
        return this.pop().toDouble()
    }
    fun Stack<String>.pushD(d : Double) {
        this.push(d.toString())
    }
    fun List<String>.lastEquals(token:String) : Boolean {
        val lastIndex = this.lastIndex
        return lastIndex >= 0 && this[lastIndex] == token
    }
    fun String?.isNumber() : Boolean =
         (this != null && "^[-+]*\\d+(?:.\\d+)*$".toRegex().matches(this))
    fun Double.degreesToRadians() : Double = this * kotlin.math.PI / 180
    fun Double.radiansToDegrees() : Double = this * 180 / kotlin.math.PI

    if (expr.isEmpty()) return ""
    println("For expression = $expr\n")
    println("Token           Action             Stack")
    val tokens = Stack<String>()
    tokens.addAll(expr.split("\\s+".toRegex()).filter{it.isNotEmpty()})
    when {
        tokens.lastEquals("DEL") -> {
            tokens.pop()
            if (tokens.size > 0) {
                val t = tokens.pop().dropLast(1)
                if (t.isNotEmpty())
                    tokens.push(t)
                return tokens.joinToString("\n")
            }
        }
        tokens.lastEquals("CHS") -> {
            tokens.pop()
            if (tokens.size > 0) {
                var t = tokens.pop()
                if (t.isNumber()) {
                    t = when {
                        t.startsWith("+") -> t.replaceFirst("+", "-")
                        t.startsWith("-") -> t.replaceFirst("-", "+")
                        else -> "-$t"
                    }
                }
                tokens.push(t)
            }
        }
        tokens.lastEquals("SWAP") -> {
            tokens.pop()
            if (tokens.size >= 2) {
                val t1 = tokens.pop()
                val t2 = tokens.pop()
                tokens.push(t1)
                tokens.push(t2)
            }
            return tokens.joinToString("\n")
        }
        tokens.lastEquals("DROP") -> {
            tokens.pop()
            if (tokens.size >= 1) {
                tokens.pop()
            }
            return tokens.joinToString("\n")
        }
        tokens.lastEquals("CLR") -> tokens.clear()
    }
    val stack = Stack<String>()
    for (token in tokens) {
        when (token) {
            // op that expects two floats on stack.
            "+", "-", "×", "*", "÷", "/", "^" -> {
                val d1 = stack.popD()
                val d2 = stack.popD()
                when (token) {
                    "+"         -> stack.pushD(d2 + d1)
                    "-"         -> stack.pushD(d2 - d1)
                    "×", "*"    -> stack.pushD(d2 * d1)
                    "÷", "/"    -> stack.pushD(d2 / d1)
                    "^"         -> stack.pushD(d2.pow(d1))
                }
                println(" $token     Apply op to top of stack    $stack")
            }
            "SIN", "COS", "TAN",-> {
                val angle = stack.popD().degreesToRadians()
                stack.pushD(when (token) {
                    "SIN" -> kotlin.math.sin(angle)
                    "COS" -> kotlin.math.cos(angle)
                    "TAN" -> kotlin.math.tan(angle)
                    else  -> {
                        0.0 /* can't happen. */
                    }
                })
            }
            "ASIN", "ACOS", "ATAN" -> {
                val value = stack.popD()
                stack.pushD(when (token) {
                    "ASIN" -> kotlin.math.asin(value)
                    "ACOS" -> kotlin.math.acos(value)
                    "ATAN" -> kotlin.math.atan(value)
                    else -> {0.0 /* can't happen. */}
                }.radiansToDegrees())
            }
            "ENTR" -> stack.push("")
            else -> stack.push(token)
        }
    }
    return stack.joinToString("\n")
}

class MainActivity : AppCompatActivity() {
    // Member variables
    private lateinit var panelTextView: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        panelTextView = findViewById(R.id.panelTextView)
    }

    var shiftIsUp = true
    @SuppressLint("SetTextI18n")
    fun btnOnClick(v: View) {
        val cls = v.javaClass.simpleName.toString()
        val buttonText : String
        buttonText = when (cls) {
            "AppCompatImageButton" -> v.tag.toString()
            "AppCompatButton" -> if (v.tag != null) v.tag.toString()
                else (v as Button).text.toString()
            else -> ""
        }

        try {
            when (buttonText) {
                "⇳SHFT" -> {
                    if (shiftIsUp) {
                        val textColor = ContextCompat.getColor(
                                this, R.color.shift_down_text)
                        shiftIsUp = false
                        tangent_button.text = "ATAN"
                        sine_button.text = "ASIN"
                        cosine_button.text = "ACOS"
                        tangent_button.setTextColor(textColor)
                        sine_button.setTextColor(textColor)
                        cosine_button.setTextColor(textColor)
                        (v as Button).setTextColor(textColor)

                        v.setBackgroundColor(
                            ContextCompat.getColor(
                                this, R.color.shift_down_bg))
                    }
                    else {
                        shiftIsUp = true
                        val textColor = ContextCompat.getColor(
                                this, android.R.color.white)
                        tangent_button.setTextColor(textColor)
                        sine_button.setTextColor(textColor)
                        cosine_button.setTextColor(textColor)
                        (v as Button).setTextColor(textColor)

                        tangent_button.text = "TAN"
                        sine_button.text = "SIN"
                        cosine_button.text = "COS"
                        v.setBackgroundColor(
                                ContextCompat.getColor(
                                        this, R.color.operation_button))
                    }
                }
                "CLR", "DEL", "ENTR", "SWAP", "DROP" -> {
                    panelTextView.text = rpnCalculate(
                            panelTextView.text.toString() + " $buttonText")
                }
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "." -> {
                    panelTextView.text = panelTextView.text.toString() + buttonText
                }
                // change sign
                "+-" -> {
                    panelTextView.text = rpnCalculate(
                            panelTextView.text.toString() + " CHS ")
                }
                "+", "-", "×", "÷", "^", "SIN", "ASIN", "COS", "ACOS", "TAN", "ATAN" -> {
                    panelTextView.text = rpnCalculate(
                            panelTextView.text.toString() + " $buttonText ENTR")
                }
                else -> Log.d("btnOnClick", "$buttonText ignored")
            }
        }
        catch (e:Exception) {
            Toast.makeText(this, "Error: $e", Toast.LENGTH_LONG).show()
        }
    }
}
