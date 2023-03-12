package org.openredstone.chad.commands

import org.openredstone.chad.FractalConfig
import kotlin.math.*
import kotlin.random.Random
import java.awt.image.BufferedImage
import java.security.MessageDigest

fun fractal(seed: String, fractalConfig: FractalConfig): BufferedImage {
    val width = fractalConfig.size
    val height = fractalConfig.size
    val maxIterations = fractalConfig.maxIterations  // don't worry it will average like 5 iterations per pixel
    val messiness = fractalConfig.messiness          // unless you change this to something like 99
    val zoom = fractalConfig.zoom

    val aspectRatio = width.toDouble() / height.toDouble()
    val rng = Random(seed.sha256lowerLong())

    val angle = rng.nextDouble(-3.14, 3.14)
    val seedCoordinate = findGoodJulia(angle, messiness)

    val a = rng.nextDouble(0.0, 0.2)
    val b = rng.nextDouble(0.0, 0.2)
    val c = rng.nextDouble(0.0, 0.2)

    val img = BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR)
    for (y in 0 until height) {
        for (x in 0 until ceil(width.toDouble() / 2.0).toInt()) {
            val coX = aspectRatio * zoom * (x.toDouble() / width.toDouble() - 0.5)
            val coY = zoom * (y.toDouble() / height.toDouble() - 0.5)
            val coordinate = Complex(coX, coY)
            val iterations = juliaPixel(coordinate, maxIterations, seedCoordinate)
            val color = getColor(iterations, a, b, c)
            img.setRGB(x, y, color)
            img.setRGB(width - x - 1, height - y - 1, color)
        }
    }
    return img
}

// Helper functions
private fun getColor(i: Double, a: Double, b: Double, c: Double): Int {
    val red = max(sin(i * a) * 255.0, 0.0).toInt()
    val green = max(sin(i * b) * 255.0, 0.0).toInt()
    val blue = max(sin(i * c) * 255.0, 0.0).toInt()
    return red + green.shl(8) + blue.shl(16)
}

private fun juliaPixel(coordinate: Complex, maxIterations: Int, c: Complex): Double {
    var z = coordinate
    var i = 0
    while (i < maxIterations && z.mag2() < 4.0) {
        z = z.square() + c
        i += 1
    }
    if (i >= maxIterations) {
        return i.toDouble()
    }
    repeat(3) {
        z = z.square() + c
        i += 1
    }
    // actual magic
    return i.toDouble() + 1.0 - ln(ln(sqrt(z.mag2()))) * 1.44269504089
}

// room for improvement but works 99.9% of times (never crashes just gives up and returns a bad one)
private fun findGoodJulia(angle: Double, messiness: Int): Complex {
    val x = cos(angle) * 0.4 - 0.3
    val y = sin(angle) * 0.4
    var coord = Complex(x, y)
    val step = coord * 0.005
    repeat(1000) {
        coord += step
        if (mandelPixel(coord, messiness + 1) <= messiness) {
            return coord
        }
    }
    // bad luck, will get an almost blank image
    return Complex(16.0, 0.0)
}

private fun mandelPixel(coord: Complex, max_iterations: Int): Int {
    var z = Complex(0.0, 0.0)
    var i = 0
    while (i < max_iterations && z.mag2() <= 4.0) {
        z = z.square() + coord
        i += 1
    }
    return i
}

// Hashing
private fun String.sha256lowerLong(): Long = MessageDigest
    .getInstance("SHA-256")
    .digest(this.toByteArray())
    .fold(0) { acc, byte -> acc.shl(8).or(byte.toLong().and(255)) }

// Complex numbers
private data class Complex(val re: Double, val im: Double)

private operator fun Complex.plus(c: Complex): Complex =
    Complex(this.re + c.re, this.im + c.im)

private operator fun Complex.times(d: Double): Complex =
    Complex(this.re * d, this.im * d)

private fun Complex.square() = Complex(re * re - im * im, 2 * re * im)

private fun Complex.mag2(): Double =
    this.re * this.re + this.im * this.im

