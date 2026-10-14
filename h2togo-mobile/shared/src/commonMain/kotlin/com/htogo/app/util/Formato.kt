package com.htogo.app.util

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * `String.format` común a Android e iOS (en Kotlin/Native no existe). Soporta lo que usa la app:
 * `%s`, `%d`, `%0Nd`, `%.Nf`, `%,.Nf` (miles con coma) y `%%`. Los decimales usan punto.
 * Al importarlo explícitamente tiene prioridad sobre el de la biblioteca estándar de JVM.
 */
fun String.format(vararg args: Any?): String {
    val sb = StringBuilder()
    var i = 0
    var arg = 0
    while (i < length) {
        val c = this[i]
        if (c != '%') { sb.append(c); i++; continue }
        if (i + 1 < length && this[i + 1] == '%') { sb.append('%'); i += 2; continue }
        // %[,][0][ancho][.precision](d|f|s)
        var j = i + 1
        var miles = false
        var ceros = false
        if (j < length && this[j] == ',') { miles = true; j++ }
        if (j < length && this[j] == '0') { ceros = true; j++ }
        var ancho = 0
        while (j < length && this[j].isDigit()) { ancho = ancho * 10 + (this[j] - '0'); j++ }
        var precision = -1
        if (j < length && this[j] == '.') {
            j++; precision = 0
            while (j < length && this[j].isDigit()) { precision = precision * 10 + (this[j] - '0'); j++ }
        }
        val tipo = if (j < length) this[j] else 's'
        val valor = args.getOrNull(arg++)
        var texto = when (tipo) {
            'd' -> (valor as? Number)?.toLong()?.let { if (miles) agruparMiles(it.toString()) else it.toString() } ?: valor.toString()
            'f' -> (valor as? Number)?.toDouble()?.let { conDecimales(it, if (precision < 0) 6 else precision, miles) } ?: valor.toString()
            else -> valor.toString()
        }
        if (texto.length < ancho) texto = (if (ceros) "0" else " ").repeat(ancho - texto.length) + texto
        sb.append(texto)
        i = j + 1
    }
    return sb.toString()
}

/** 1234.567 con 2 decimales → "1234.57" (con [miles], "1,234.57"). */
fun conDecimales(valor: Double, decimales: Int, miles: Boolean = false): String {
    val factor = 10.0.pow(decimales)
    val redondeado = (abs(valor) * factor).roundToLong()
    val entera = (redondeado / factor.toLong()).toString()
    val fraccion = (redondeado % factor.toLong()).toString().padStart(decimales, '0')
    val signo = if (valor < 0 && redondeado != 0L) "-" else ""
    val parteEntera = if (miles) agruparMiles(entera) else entera
    return signo + parteEntera + if (decimales > 0) ".$fraccion" else ""
}

private fun agruparMiles(digitos: String): String {
    val neg = digitos.startsWith("-")
    val d = digitos.removePrefix("-")
    val agrupado = d.reversed().chunked(3).joinToString(",").reversed()
    return if (neg) "-$agrupado" else agrupado
}
