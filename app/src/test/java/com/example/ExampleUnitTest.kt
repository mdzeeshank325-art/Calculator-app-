package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    
    private val parser = CalculatorParser(useDegrees = false)
    private val parserDegrees = CalculatorParser(useDegrees = true)

    @Test
    fun addition_isCorrect() {
        assertEquals(4.0, parser.evaluate("2 + 2"), 0.0)
    }

    @Test
    fun precedence_isCorrect() {
        // Order of operations: multiplication before addition
        assertEquals(14.0, parser.evaluate("2 + 3 * 4"), 0.0)
        assertEquals(14.0, parser.evaluate("2 + 3 × 4"), 0.0)
    }

    @Test
    fun parentheses_areCorrect() {
        assertEquals(20.0, parser.evaluate("(2 + 3) * 4"), 0.0)
    }

    @Test
    fun implicitMultiplication_isCorrect() {
        // implicit multiplication: 2(3+4) = 2*7 = 14
        assertEquals(14.0, parser.evaluate("2(3+4)"), 0.0)
        // implicit multiplication of pi
        assertEquals(Math.PI * 2.0, parser.evaluate("2π"), 0.0001)
        // continuous implicit multiplication
        assertEquals(40.0, parser.evaluate("(2+3)(4+4)"), 0.0)
    }

    @Test
    fun powersAndRoots_areCorrect() {
        assertEquals(8.0, parser.evaluate("2^3"), 0.0)
        assertEquals(3.0, parser.evaluate("√9"), 0.0)
    }

    @Test
    fun trig_isCorrect() {
        // sin(0) = 0
        assertEquals(0.0, parser.evaluate("sin(0)"), 0.0001)
        // cos(0) = 1
        assertEquals(1.0, parser.evaluate("cos(0)"), 0.0001)
    }

    @Test
    fun degrees_areCorrect() {
        // sin(90 degrees) = 1
        assertEquals(1.0, parserDegrees.evaluate("sin(90)"), 0.0001)
        // cos(60 degrees) = 0.5
        assertEquals(0.5, parserDegrees.evaluate("cos(60)"), 0.0001)
    }
}
