package com.kana_tutor.rpncalc

class Conversions private constructor(){
    open class Unit (        // grams, farenheit or celcius for exmple
        val name:String,
            // for completeness.
        val description:String,
            // mtr or ft for example.
        val tag:String,
            // gram for weight or cmtr for example
    )
    class UnitInstance(
        name:String,
        description:String,
        tag:String,
        // gram for weight or cmtr for example
        val baseTag:String,
        // this contains string RPN code to convert to/from this instance.
        // conversion is accomplished by "Chaining" conversions, so
        // the conversion from feet to instances will be
        // ft -> cmtr, cmtr -> in
        val convertTo:String,
        val convertFrom:String,
        // list link.
        var nextInstance : UnitInstance? = null
    ) : Unit(name, description, tag)
    class TypeOfUnit (
        name:String,
        description:String,
        tag:String,
        var instances : UnitInstance? = null,
        var nextUnit : TypeOfUnit? = null,
    ) : Unit(name, description, tag)
    val root: TypeOfUnit

    init{
        root = TypeOfUnit("Temperature", "", "temp")
        root.instances = UnitInstance("Centegrade", "", "°C",
                "°C", "1 *", "1 *")
        var nextInstance = root.instances!!.nextInstance
        nextInstance = UnitInstance("Kelvin", "", "°K",
                "°C", "273.15 +", "253.15 -")
        nextInstance = nextInstance.nextInstance
        nextInstance = UnitInstance("Fahrenheit", "", "°F",
                "°C", "32 - 5 * 9 /", "5 / 9 * 32 +")
        root.nextUnit = TypeOfUnit("Distance", "", "dst")
        nextInstance = root.nextUnit!!.instances
        nextInstance = UnitInstance("Foot", "", "ft",
                "mtr", "0.348 /", "0.348 *")
        nextInstance = nextInstance.nextInstance
        nextInstance = UnitInstance("Inch", "", "in",
                "mtr", "0.0254 /", "0.0254 *")
        nextInstance = nextInstance.nextInstance
        nextInstance = UnitInstance("Yard", "", "yd",
                "mtr", "0.9144 /", "0.9144 *")
        nextInstance = nextInstance.nextInstance
        nextInstance = UnitInstance("Mile", "", "mi",
                "mtr", "1609.34 /", "1609.34 *")
        nextInstance = nextInstance.nextInstance
        nextInstance = UnitInstance("Nautical Mile", "", "Nmi",
                "mtr", "1852 /", "1852 *")
        nextInstance = nextInstance.nextInstance
        nextInstance = UnitInstance("Astronomical Unit", "", "AU",
                "mtr", "1.496E11 /", "1.496E11 *")
        nextInstance = nextInstance.nextInstance
        nextInstance = UnitInstance("Furlong (US)", "", "fur",
                "mtr", "201.168 /", "201.168 *")
        nextInstance = nextInstance.nextInstance
        nextInstance = UnitInstance("Centimeter", "", "cmtr",
                "mtr", "1E2 /", "1E2 *")
        nextInstance = nextInstance.nextInstance
        nextInstance = UnitInstance("Kilometer", "", "kmtr",
                "mtr", "1E3 *", "1E3 /")
        nextInstance = nextInstance.nextInstance
        nextInstance = UnitInstance("Millimeter", "", "mmtr",
                "mtr", "1E-3 *", "1E3 *")
        nextInstance = nextInstance.nextInstance
        nextInstance = UnitInstance("Meter", "", "mtr",
                "mtr", "1 *", "1 *")
    }

    companion object {
        private val myInstance = Conversions()
        fun getInstance(): Conversions {
            return myInstance
        }
    }
}