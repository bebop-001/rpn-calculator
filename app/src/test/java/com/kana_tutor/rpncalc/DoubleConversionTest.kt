import com.kana_tutor.rpncalc.kanautils.longBitStringToDouble
import com.kana_tutor.rpncalc.kanautils.toLongBitsString

fun main(args: Array<String>) {
    val double = 1.5
	val longBits = java.lang.Double.doubleToLongBits(double)
    val longBitsS = longBits.toString(16)
    val d1: Double = java.lang.Double.longBitsToDouble(longBitsS.toLong(16))
    println("$double, $longBits, $longBitsS, $d1, ${double - d1}")
    val double2 = 1/3.0
    val s1 = double2.toLongBitsString()
    println("$double2, $s1, ${s1.longBitStringToDouble() - double2}")
    println("Empty string:${"".longBitStringToDouble()}")
    println("Nan:${(1/0.0).toLongBitsString()}")
}
