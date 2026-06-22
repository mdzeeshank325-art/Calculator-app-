package com.example

import kotlin.math.*

class CalculatorParser(private val useDegrees: Boolean = false) {

    fun evaluate(expressionStr: String): Double {
        val tokens = tokenize(expressionStr)
        if (tokens.isEmpty()) return 0.0
        val parser = ParserInstance(tokens, useDegrees)
        val result = parser.parseExpression()
        if (parser.hasNext()) {
            throw IllegalArgumentException("Unexpected token: " + parser.peek())
        }
        if (result.isNaN()) {
            throw IllegalArgumentException("Not a Number")
        }
        if (result.isInfinite()) {
            throw ArithmeticException("Value is infinite")
        }
        return result
    }

    private fun tokenize(expr: String): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        val normalized = expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("π", "p") // Represent as 'p' for character tracking
        
        while (i < normalized.length) {
            val c = normalized[i]
            when {
                c.isWhitespace() -> {
                    i++
                }
                c in "+-*/^%()" -> {
                    result.add(c.toString())
                    i++
                }
                c == '√' -> {
                    result.add("sqrt")
                    i++
                }
                c.isDigit() || c == '.' -> {
                    val sb = StringBuilder()
                    while (i < normalized.length && (normalized[i].isDigit() || normalized[i] == '.')) {
                        sb.append(normalized[i])
                        i++
                    }
                    result.add(sb.toString())
                }
                c.isLetter() -> {
                    val sb = StringBuilder()
                    while (i < normalized.length && (normalized[i].isLetter())) {
                        sb.append(normalized[i])
                        i++
                    }
                    val word = sb.toString()
                    if (word == "p") {
                        result.add("pi")
                    } else {
                        result.add(word)
                    }
                }
                else -> {
                    throw IllegalArgumentException("Unknown character: $c")
                }
            }
        }

        // Insert implicit multiplication:
        val processedTokens = mutableListOf<String>()
        for (idx in result.indices) {
            val current = result[idx]
            if (idx > 0) {
                val prev = processedTokens.last()
                val isPrevValue = isValueToken(prev)
                val isCurrentValueOrFuncOrParen = isStartingValueToken(current) || isFunctionToken(current) || current == "("
                if (isPrevValue && isCurrentValueOrFuncOrParen) {
                    processedTokens.add("*")
                }
            }
            if (current == "p") {
                processedTokens.add("pi")
            } else {
                processedTokens.add(current)
            }
        }
        
        return processedTokens
    }

    private fun isStartingValueToken(token: String): Boolean {
        if (token == "pi" || token == "e") return true
        val firstChar = token.firstOrNull() ?: return false
        return firstChar.isDigit() || firstChar == '.'
    }

    private fun isValueToken(token: String): Boolean {
        if (token == "pi" || token == "e" || token == ")") return true
        val firstChar = token.firstOrNull() ?: return false
        return firstChar.isDigit() || firstChar == '.'
    }

    private fun isFunctionToken(token: String): Boolean {
        return token in listOf("sin", "cos", "tan", "log", "ln", "sqrt")
    }

    private class ParserInstance(private val tokens: List<String>, private val useDegrees: Boolean) {
        private var index = 0

        fun hasNext() = index < tokens.size

        fun peek(): String = if (hasNext()) tokens[index] else ""

        fun consume(): String {
            val token = peek()
            index++
            return token
        }

        fun parseExpression(): Double {
            var value = parseTerm()
            while (hasNext()) {
                val op = peek()
                if (op == "+" || op == "-") {
                    consume()
                    val nextTerm = parseTerm()
                    if (op == "+") value += nextTerm else value -= nextTerm
                } else {
                    break
                }
            }
            return value
        }

        private fun parseTerm(): Double {
            var value = parseFactor()
            while (hasNext()) {
                val op = peek()
                if (op == "*" || op == "/" || op == "%") {
                    consume()
                    val nextFactor = parseFactor()
                    if (op == "*") {
                        value *= nextFactor
                    } else if (op == "/") {
                        if (nextFactor == 0.0) throw ArithmeticException("Division by zero")
                        value /= nextFactor
                    } else {
                        if (nextFactor == 0.0) throw ArithmeticException("Modulo by zero")
                        value %= nextFactor
                    }
                } else {
                    break
                }
            }
            return value
        }

        private fun parseFactor(): Double {
            var value = parseBase()
            if (peek() == "^") {
                consume()
                val exponent = parseFactor()
                value = value.pow(exponent)
            }
            return value
        }

        private fun parseBase(): Double {
            val token = peek()
            if (token.isEmpty()) {
                throw IllegalArgumentException("Unexpected end of expression")
            }
            if (token == "+") {
                consume()
                return parseBase()
            }
            if (token == "-") {
                consume()
                return -parseBase()
            }
            if (token == "(") {
                consume() // '('
                val value = parseExpression()
                if (peek() == ")") {
                    consume() // ')'
                } else {
                    throw IllegalArgumentException("Missing )")
                }
                return value
            }
            if (token in listOf("sin", "cos", "tan", "log", "ln", "sqrt")) {
                val func = consume()
                val argument = parseBase()
                return when (func) {
                    "sin" -> {
                        val radians = if (useDegrees) Math.toRadians(argument) else argument
                        sin(radians)
                    }
                    "cos" -> {
                        val radians = if (useDegrees) Math.toRadians(argument) else argument
                        cos(radians)
                    }
                    "tan" -> {
                        val radians = if (useDegrees) Math.toRadians(argument) else argument
                        tan(radians)
                    }
                    "log" -> log10(argument)
                    "ln" -> ln(argument)
                    "sqrt" -> {
                        if (argument < 0) throw IllegalArgumentException("Square root of negative number")
                        sqrt(argument)
                    }
                    else -> throw IllegalArgumentException("Unknown function: $func")
                }
            }
            if (token == "pi") {
                consume()
                return Math.PI
            }
            if (token == "e") {
                consume()
                return Math.E
            }
            
            val numStr = consume()
            return numStr.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid token: $numStr")
        }
    }
}
