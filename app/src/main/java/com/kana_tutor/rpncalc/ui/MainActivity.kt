package com.kana_tutor.rpncalc.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.kana_tutor.rpncalc.R
import com.kana_tutor.rpncalc.model.Rpn

class MainActivity : AppCompatActivity() {
    // Member variables
    private var panelTextView: TextView? = null
    private var input: String? = null
    private var btnClicked: Button? = null
    private var lastIsZero = false
    private var operationPerformed = false
    private var historyStore: SharedPreferences? = null
    private val rpn = Rpn()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        panelTextView = findViewById(R.id.panelTextView)
        historyStore = getSharedPreferences(Rpn.KEY, MODE_PRIVATE)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.settings_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.btnHistory -> {
                val i = Intent(this@MainActivity, HistoryActivity::class.java)
                startActivity(i)
                true
            }
            R.id.btnAbout -> {
                startActivity(Intent(this@MainActivity, AboutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun btnOnClick(v: View) {
        // Split the content of the panelTextView into an string array
        val strSplitted = panelTextView!!.text.toString().split("\n".toRegex()).toTypedArray()
        val cls = v.javaClass.simpleName.toString()
        val buttonText : String
        when (cls) {
            "AppCompatImageButton" -> buttonText = v.tag.toString()
            "AppCompatButton" -> buttonText = (v as Button).text.toString()
            else -> buttonText = ""
        }

        when (buttonText) {
            "CLR" -> {
                operationPerformed = false
                panelTextView!!.text = "0"
            }
            "DEL" -> {

                // Remove the last number entered
                var newInput = rpn.delete(panelTextView!!.text.toString())
                if (newInput.length == 0) {
                    newInput = "0"
                }
                panelTextView!!.text = newInput
            }
            "ENTR" -> if (panelTextView!!.text.toString().length > 1 || panelTextView!!.text.toString().toDouble() != 0.0) {
                operationPerformed = false
                panelTextView!!.text = rpn.formatInput(strSplitted) //Format input
                panelTextView!!.append("\n0") // Append new line with a default zero in the text view
                lastIsZero = true
            }
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "." -> {
                // Handler numeric buttons

                /* A Button is also a View, so I'll cast the View parameter called v that onClick
                   method receive into a Button object, this way I can get properties of the button
                   clicked, for example the button's text */btnClicked = v as Button

                // Check if is the first input of the row
                if (panelTextView!!.text.toString().length == 1 && panelTextView!!.text.toString().toDouble() == 0.0 && buttonText != ".") {
                    panelTextView!!.text = btnClicked!!.text
                }
                else {
                    if (lastIsZero && buttonText != ".") {
                        panelTextView!!.text = rpn.delete(panelTextView!!.text.toString())
                    }
                    if (operationPerformed) {
                        panelTextView!!.append("\n")
                        operationPerformed = false
                    }
                    // Avoid insert more than one zero per input
                    if (buttonText == "." && !strSplitted[strSplitted.size - 1].contains(".")) {
                        panelTextView!!.append(".")
                    }
                    else if (buttonText != ".") {
                        panelTextView!!.append(btnClicked!!.text) // Append input to textview
                    }
                    lastIsZero = false
                }
            }
            // change sign
            "+-" -> {
                // Change symbol of last input
                val input = rpn.changeInputSymbol(strSplitted)
                if (input.length > 0) {
                    panelTextView!!.text = input
                }
            }
            "+", "-", "×", "÷" -> {
                // Handler operator buttons
                btnClicked = v as Button
                if (strSplitted.size > 1) {
                    operationPerformed = true
                    input = rpn.proccess(strSplitted, btnClicked!!.text.toString(), historyStore)
                    panelTextView!!.text = input
                }
            }
        }
    }
}