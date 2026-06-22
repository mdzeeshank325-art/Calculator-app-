package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {
    private val database = CalculationDatabase.getDatabase(application)
    private val repository = CalculationRepository(database.calculationDao())

    val history: StateFlow<List<CalculationHistory>> = repository.allCalculations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _useDegrees = MutableStateFlow(false)
    val useDegrees: StateFlow<Boolean> = _useDegrees.asStateFlow()

    private val _preview = MutableStateFlow<String?>(null)
    val preview: StateFlow<String?> = _preview.asStateFlow()

    init {
        // Evaluate expression in real-time
        combine(_input, _useDegrees) { currentInput, degrees ->
            Pair(currentInput, degrees)
        }.onEach { (currentInput, degrees) ->
            _preview.value = calculatePreview(currentInput, degrees)
        }.launchIn(viewModelScope)
    }

    fun setInput(newInput: String) {
        _input.value = newInput
    }

    fun toggleDegrees() {
        _useDegrees.value = !_useDegrees.value
    }

    fun append(char: String) {
        // Prevent multiple trailing decimals or consecutive invalid operators
        val current = _input.value
        
        // If clicking on 'Error', starting a new calculations overrides it
        if (current == "Error") {
            _input.value = if (char in listOf("+", "-", "×", "÷", "*", "/", "^", "%")) "0$char" else char
            return
        }

        // Clean double operator entry (replace original)
        val operators = listOf("+", "-", "×", "÷", "*", "/", "^", "%")
        if (operators.contains(char) && current.isNotEmpty()) {
            val lastChar = current.last().toString()
            if (operators.contains(lastChar)) {
                _input.value = current.dropLast(1) + char
                return
            }
        }
        
        _input.value = current + char
    }

    fun delete() {
        val current = _input.value
        if (current.isEmpty() || current == "Error") {
            _input.value = ""
            return
        }
        
        // Smart deletion for multi-character functions
        val functions = listOf("sin(", "cos(", "tan(", "log(", "ln(", "sqrt(")
        val matchedFunc = functions.firstOrNull { current.endsWith(it) }
        if (matchedFunc != null) {
            _input.value = current.substring(0, current.length - matchedFunc.length)
        } else {
            _input.value = current.dropLast(1)
        }
    }

    fun clear() {
        _input.value = ""
    }

    fun toggleSign() {
        val current = _input.value
        if (current.isEmpty() || current == "Error") {
            _input.value = "-"
            return
        }
        
        // If last character is close parenthesis or starts back, handle negation
        val lastNumberRegex = Regex("""\d+(\.\d+)?$""")
        val match = lastNumberRegex.find(current)
        if (match != null) {
            val lastNum = match.value
            val preNumberIdx = match.range.first
            if (preNumberIdx >= 2 && current.substring(preNumberIdx - 2, preNumberIdx) == "(-") {
                // Negate "(-5" back to "5"
                _input.value = current.substring(0, preNumberIdx - 2) + lastNum
                // Also clean trailing closed parenthesis if we wrapped it
                if (_input.value.endsWith(")")) {
                    // This is just a visual wrap: if input was "(-5)", then preNumberIdx is index of 5.
                    // Instead of full string parsing, let's look at a simpler wrap toggle.
                }
            } else if (preNumberIdx >= 1 && current[preNumberIdx - 1] == '-') {
                _input.value = current.substring(0, preNumberIdx - 1) + lastNum
            } else {
                _input.value = current.substring(0, preNumberIdx) + "(-" + lastNum
            }
        } else {
            if (current.endsWith("-")) {
                _input.value = current.dropLast(1)
            } else {
                _input.value = current + "-"
            }
        }
    }

    fun evaluateResult() {
        val currentInput = _input.value
        if (currentInput.isEmpty() || currentInput == "Error") return
        
        viewModelScope.launch {
            try {
                val parser = CalculatorParser(useDegrees.value)
                val rawResult = parser.evaluate(currentInput)
                val formattedResult = formatDouble(rawResult)
                
                repository.insert(
                    CalculationHistory(
                        expression = currentInput,
                        result = formattedResult
                    )
                )
                
                _input.value = formattedResult
            } catch (e: Exception) {
                _input.value = "Error"
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    private fun calculatePreview(currentInput: String, useDegrees: Boolean): String? {
        if (currentInput.isEmpty() || currentInput == "Error") return null
        
        // Don't show preview if input is just simple numbers with no operational keys
        if (!currentInput.any { it in "+-*/^%()×÷√" } && 
            !currentInput.contains("sin") && 
            !currentInput.contains("cos") && 
            !currentInput.contains("tan") && 
            !currentInput.contains("log") && 
            !currentInput.contains("ln")) {
            return null
        }
        
        return try {
            val parser = CalculatorParser(useDegrees)
            val raw = parser.evaluate(currentInput)
            val formatted = formatDouble(raw)
            if (formatted == currentInput || formatted == "Error") null else "= $formatted"
        } catch (e: Exception) {
            null // Silent return
        }
    }

    private fun formatDouble(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "Error"
        
        return try {
            val bd = java.math.BigDecimal(value)
                .setScale(10, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros()
            val plain = bd.toPlainString()
            if (plain == "-0") "0" else plain
        } catch (e: Exception) {
            val rawString = value.toString()
            if (rawString.endsWith(".0")) rawString.substring(0, rawString.length - 2) else rawString
        }
    }
}
