package com.kana_tutor.rpncalc

import java.lang.RuntimeException

object Conversions {
    open class Unit(
            // grams, farenheit or celcius for exmple
        val name: String,
            // for completeness.
        val description: String,
            // m or ft for example.
        val tag: String,
            // gram for weight or cm for example
    )

    class UnitInstance(
        val type:String, // dst, time, etc.
        name: String,
        description: String,
        tag: String,
        val toBase: String,
        val fromBase: String,
    ) : Unit(name, description, tag)

    class UnitType(
        name: String,
        description: String,
        type: String, // like temp, dst, time, etc.
        base: String, // like m, mm, mile, etc
    ) : Unit(name, description, type)

    val conversions = mutableMapOf<String, Pair<UnitType, HashMap<String, UnitInstance>>>()
    fun listConversions() : List<String> {
        val rv = mutableListOf<String>()
        conversions.keys.forEach { typeKey ->
            val (unitType, instanceMap) = conversions[typeKey]!!
            val typeStr = "${unitType.name}:$typeKey"
            instanceMap.keys.forEach{instanceKey ->
                rv.add("$typeStr -> ${instanceMap[instanceKey]!!.name}:$instanceKey")
            }
        }
        return rv
    }
    fun getFromTo(
            type:String,
            fromUnits:String,
            toUnits:String
    ) :Pair<String, String>?{
        var rv : Pair<String, String>? = null
        if (conversions[type] != null) {
            val (_, cvtMap) = conversions[type]!!
            if (cvtMap[fromUnits] != null && cvtMap[toUnits] != null)
                rv = Pair(cvtMap[fromUnits]!!.toBase, cvtMap[toUnits]!!.fromBase)
        }
        return rv
    }

    init {
        arrayOf<Any>(
            UnitType("Temperature", "", "temp", "C"),
            UnitInstance("temp", "Centegrade",
                "°C", "C", "1 *", "1 *"),
            UnitInstance("temp", "Kelvin",
                "°K", "K", "273.15 -", "273.15 +"),
            UnitInstance("temp", "Fahrenheit",
                "°F", "F", "32 - 5 * 9 /", "5 / 9 * 32 +"),

            UnitType("Distance", "", "dst", "m"),
            UnitInstance("dst", "Foot",
				 "", "ft", "0.3048 *", "0.3048 /"),
            UnitInstance("dst", "Inch",
				 "", "in", "0.0254 *", "0.0254 /"),
            UnitInstance("dst", "Yard",
				 "", "yd", "0.9144 *", "0.9144 /"),
            UnitInstance("dst", "Mile",
				 "", "mi", "1609.344 *", "1609.344 /"),
            UnitInstance("dst", "Fathom",
                "", "fat", "1.8288 *", "1.8288 /"),
            UnitInstance("dst", "Nautical Mile",
                "", "Nmi", "1852 *", "1852 /"),
            UnitInstance("dst", "Astronomical Unit",
                    "", "AU", "149597871E3 *", "149597871E3 /"),
            UnitInstance("dst", "Light Years",
                    "", "LY", "9.4607305e15 *", "9.4607305e15 /"),
            UnitInstance("dst", "Furlong (US)",
				 "", "fur", "201.168 *", "201.168 /"),
            UnitInstance("dst", "Centimeter",
				 "", "cm", "1E2 /", "1E2 *"),
            UnitInstance("dst", "Kilometer",
				 "", "km", "1E3 *", "1E3 /"),
            UnitInstance("dst", "Millimeter",
				 "", "mm", "1E-3 *", "1E3 *"),
            UnitInstance("dst", "Meter",
				 "", "m", "1 *", "1 *"),

            UnitType("Time", "Time in seconds.",
                "time", "s"),
            UnitInstance("time", "Seconds",
                "", "s", "1 *", "1 *"),
            UnitInstance("time", "Milli-seconds",
                "", "milli-s", "1E-3 *", "1E3 *"),
            UnitInstance("time", "Micro-seconds",
                "", "micro-s", "1E-6 *", "1E6 *"),
            UnitInstance("time", "Minute",
                "", "min", "60 *", "60 /"),
            UnitInstance("time", "Hour",
                "", "hr", "${60*60} *", "${60*60} /"),
            UnitInstance("time", "Day",
                "", "day", "${60*60*24} *", "${60*60*24} /"),
            UnitInstance("time", "Week",
                "", "wk", "${60*60*24*7} *", "${60*60*24*7} /"),
            UnitInstance("time", "Fortnight",
                "", "fortn", "${60*60*24*14} *", "${60*60*24*14} /"),
            UnitInstance("time", "Year",
                "", "yr", "${60*60*24*365} *", "${60*60*24*365} /"),
        ).forEach{
            if(it is UnitType) {
                conversions[it.tag] = Pair(it, hashMapOf())
            }
            else if (it is UnitInstance) {
                val (obj,instanceMap) = conversions[it.type]!!
                instanceMap[it.tag] = it
            }
            else throw RuntimeException(
                    "Conversions:init:${it::class.java.simpleName}:unexpected class.")
        }
    }
}
