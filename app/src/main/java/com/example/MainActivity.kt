package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent // Allow full bleed of gradient
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(ObsidianBgStart, ObsidianBgEnd)
                                )
                            )
                            .padding(innerPadding)
                    ) {
                        CalculatorAppContent(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorAppContent(viewModel: CalculatorViewModel) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    if (isTablet) {
        TabletCalculatorLayout(viewModel)
    } else {
        MobileCalculatorLayout(viewModel)
    }
}

@Composable
fun MobileCalculatorLayout(viewModel: CalculatorViewModel) {
    val input by viewModel.input.collectAsStateWithLifecycle()
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    val useDegrees by viewModel.useDegrees.collectAsStateWithLifecycle()
    val historyList by viewModel.history.collectAsStateWithLifecycle()
    
    var showHistory by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // History Toggle Button
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showHistory = !showHistory
                },
                modifier = Modifier
                    .background(DisplayCardColor, CircleShape)
                    .testTag("btn_history_toggle")
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "Show history",
                    tint = if (showHistory) GlowTeal else Color.White
                )
            }

            // Brand Header / Title
            Text(
                text = "CALCULATOR",
                color = ThemeTextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                fontFamily = FontFamily.SansSerif
            )

            // DEG / RAD Toggle Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DisplayCardColor)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleDegrees()
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("btn_deg_rad_toggle"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.getDotSize(useDegrees))
                        .clip(CircleShape)
                        .background(if (useDegrees) GlowTeal else ThemeTextSecondary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (useDegrees) "DEG" else "RAD",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Display area or History Drawer
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            if (showHistory) {
                HistoryDrawer(
                    historyList = historyList,
                    onSelect = { item ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setInput(item.expression)
                        showHistory = false
                    },
                    onDelete = { item ->
                        viewModel.deleteHistoryItem(item.id)
                    },
                    onClearAll = {
                        viewModel.clearHistory()
                    },
                    onClose = {
                        showHistory = false
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                DisplayPanel(
                    input = input,
                    preview = preview,
                    onClearInput = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.clear()
                    },
                    onBackspace = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.delete()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Keyboard Controls Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            // Horizontal Scientific Scroll Bar
            ScientificButtonRow(
                onAppend = { char ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.append(char)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Standard Keyboard Grid
            StandardKeyboardGrid(
                onAppend = { char ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.append(char)
                },
                onClear = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.clear()
                },
                onToggleSign = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleSign()
                },
                onEvaluate = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.evaluateResult()
                }
            )
        }
    }
}

@Composable
fun TabletCalculatorLayout(viewModel: CalculatorViewModel) {
    val input by viewModel.input.collectAsStateWithLifecycle()
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    val useDegrees by viewModel.useDegrees.collectAsStateWithLifecycle()
    val historyList by viewModel.history.collectAsStateWithLifecycle()
    
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Left Side: History & Config Sheet (Canonal layout: Supporting Pane)
        Card(
            modifier = Modifier
                .weight(0.35f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = DisplayCardColor),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HISTORY LOG",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )

                    if (historyList.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.clearHistory()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear all history",
                                tint = Color.LightGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (historyList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "No calculations",
                                tint = ThemeTextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No calculations yet",
                                color = ThemeTextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(historyList, key = { it.id }) { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ObsidianBgStart)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.setInput(item.expression)
                                    }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.expression,
                                        color = ThemeTextSecondary,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteHistoryItem(item.id)
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete calculation item",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "= ${item.result}",
                                    color = GlowTeal,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right Side: Standard and Scientific Deck (Canonical panel)
        Column(
            modifier = Modifier
                .weight(0.65f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Main Display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f),
                colors = CardDefaults.cardColors(containerColor = DisplayCardColor),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CALCULATOR DECK",
                        color = ThemeTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    // DEG/RAD Toggle
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(ObsidianBgStart)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.toggleDegrees()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (useDegrees) GlowTeal else ThemeTextSecondary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (useDegrees) "DEG" else "RAD",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    DisplayPanel(
                        input = input,
                        preview = preview,
                        onClearInput = { viewModel.clear() },
                        onBackspace = { viewModel.delete() },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Expanded Desktop/Tablet Grid combining inputs and scientific math
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Scientific Subgrid Column (3 columns wide)
                    Column(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val sciButtons = listOf(
                            listOf("sin(", "cos(", "tan("),
                            listOf("log(", "ln(", "^"),
                            listOf("√(", "π", "e"),
                            listOf("(", ")", "%")
                        )

                        for (row in sciButtons) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (char in row) {
                                    val formattedLabel = char.removeSuffix("(")
                                    CalculatorButton(
                                        text = formattedLabel,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.append(char)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        containerColor = KeyScientificColor,
                                        contentColor = GlowTeal,
                                        testTag = "btn_tablet_${formattedLabel}"
                                    )
                                }
                            }
                        }
                    }

                    // Main Numeric & Operative Keyboard Panel
                    Column(
                        modifier = Modifier
                            .weight(0.6f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val gridButtons = listOf(
                            listOf("C", "±", "%", "÷"),
                            listOf("7", "8", "9", "×"),
                            listOf("4", "5", "6", "-"),
                            listOf("1", "2", "3", "+"),
                            listOf("0", ".", "=")
                        )

                        for (rowData in gridButtons) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (char in rowData) {
                                    val isClear = char == "C"
                                    val isEqual = char == "="
                                    val isOperator = char in listOf("÷", "×", "-", "+", "±")
                                    val isZero = char == "0"

                                    val container = when {
                                        isClear -> Color(0xFFE04F5F)
                                        isEqual -> KeyAccentColor
                                        isOperator -> KeyScientificColor
                                        else -> KeyStandardColor
                                    }

                                    val content = when {
                                        isClear -> Color.White
                                        isEqual -> Color.White
                                        isOperator -> AccentOrange
                                        else -> Color.White
                                    }

                                    val tag = when (char) {
                                        "C" -> "btn_tablet_clear"
                                        "=" -> "btn_tablet_equals"
                                        "÷" -> "btn_tablet_divide"
                                        "×" -> "btn_tablet_multiply"
                                        "-" -> "btn_tablet_subtract"
                                        "+" -> "btn_tablet_add"
                                        "±" -> "btn_tablet_sign"
                                        "." -> "btn_tablet_dot"
                                        else -> "btn_tablet_$char"
                                    }

                                    CalculatorButton(
                                        text = char,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            when (char) {
                                                "C" -> viewModel.clear()
                                                "±" -> viewModel.toggleSign()
                                                "=" -> viewModel.evaluateResult()
                                                "÷" -> viewModel.append("÷")
                                                "×" -> viewModel.append("×")
                                                else -> viewModel.append(char)
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(if (isZero) 2f else 1f)
                                            .fillMaxHeight(),
                                        containerColor = container,
                                        contentColor = content,
                                        testTag = tag
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DisplayPanel(
    input: String,
    preview: String?,
    onClearInput: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DisplayCardColor),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            // Spacer/alignment spacer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.TopStart
            ) {
                // Clear Quick Trigger if text is present
                if (input.isNotEmpty()) {
                    Text(
                        text = "CLEAR ALL",
                        color = Color(0xFFFF4D4D),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onClearInput() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Running Input Display (Auto-scaling or wrapping scroll text)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.BottomEnd
            ) {
                val scrollState = rememberScrollState()
                
                // Keep cursor at the end by scrolling on changes
                LaunchedEffect(input) {
                    if (scrollState.maxValue > 0) {
                        scrollState.animateScrollTo(scrollState.maxValue, tween(300))
                    }
                }

                Text(
                    text = input.ifEmpty { "0" },
                    color = Color.White,
                    fontSize = if (input.length > 10) 32.sp else 44.sp,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                    maxLines = 1,
                    fontFamily = FontFamily.SansSerif
                )
            }

            // Real-time Preview / Computed Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Erase Backspace indicator inside screen
                if (input.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(ObsidianBgStart, CircleShape)
                            .clickable { onBackspace() }
                            .testTag("btn_backspace"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⌫",
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Text(
                    text = preview ?: "",
                    color = GlowTeal,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ScientificButtonRow(
    onAppend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sciButtons = listOf(
        Pair("sin", "sin("),
        Pair("cos", "cos("),
        Pair("tan", "tan("),
        Pair("xʸ", "^"),
        Pair("√x", "√("),
        Pair("ln", "ln("),
        Pair("log", "log("),
        Pair("π", "π"),
        Pair("e", "e"),
        Pair("(", "("),
        Pair(")", ")")
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for ((label, char) in sciButtons) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(KeyScientificColor)
                    .clickable { onAppend(char) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("btn_$label"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = GlowTeal,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StandardKeyboardGrid(
    onAppend: (String) -> Unit,
    onClear: () -> Unit,
    onToggleSign: () -> Unit,
    onEvaluate: () -> Unit
) {
    val keys = listOf(
        listOf("C", "()", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("±", "0", ".", "=")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (row in keys) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (char in row) {
                    val isClear = char == "C"
                    val isEqual = char == "="
                    val isOperator = char in listOf("÷", "×", "-", "+")
                    val isSpecial = char in listOf("()", "%", "±")

                    val containerColor = when {
                        isClear -> Color(0xFFE04F5F)
                        isEqual -> KeyAccentColor
                        isOperator -> KeyScientificColor
                        isSpecial -> KeyScientificColor
                        else -> KeyStandardColor
                    }

                    val contentColor = when {
                        isClear || isEqual -> Color.White
                        isOperator -> AccentOrange
                        isSpecial -> GlowTeal
                        else -> Color.White
                    }

                    val tag = when (char) {
                        "C" -> "btn_clear"
                        "()" -> "btn_paren"
                        "%" -> "btn_percent"
                        "÷" -> "btn_divide"
                        "×" -> "btn_multiply"
                        "-" -> "btn_subtract"
                        "+" -> "btn_add"
                        "±" -> "btn_sign"
                        "." -> "btn_dot"
                        "=" -> "btn_equals"
                        else -> "btn_$char"
                    }

                    CalculatorButton(
                        text = char,
                        onClick = {
                            when (char) {
                                "C" -> onClear()
                                "±" -> onToggleSign()
                                "=" -> onEvaluate()
                                "()" -> {
                                    // Smart parenthesis insertion: if it's already got open parenthesise unmatched, insert a closing paren, else opening index
                                    onAppend("(")
                                }
                                else -> onAppend(char)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(if (char == "=") 1f else 1.1f), // Elegant square key layout
                        containerColor = containerColor,
                        contentColor = contentColor,
                        testTag = tag
                    )
                }
            }
        }
    }
}

@Composable
fun CalculatorButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = KeyStandardColor,
    contentColor: Color = Color.White,
    testTag: String
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = if (text.length > 2) 18.sp else 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
fun HistoryDrawer(
    historyList: List<CalculationHistory>,
    onSelect: (CalculationHistory) -> Unit,
    onDelete: (CalculationHistory) -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DisplayCardColor),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to display",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "CALCULATION HISTORY",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                if (historyList.isNotEmpty()) {
                    IconButton(onClick = onClearAll) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete all history items",
                            tint = Color(0xFFFF5252)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "No calculations",
                            tint = ThemeTextSecondary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Your calculation ledger is empty.",
                            color = ThemeTextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "New formulas will appear here after evaluation.",
                            color = ThemeTextSecondary.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    state = rememberLazyListState()
                ) {
                    items(historyList, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(ObsidianBgStart)
                                .clickable { onSelect(item) }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.expression,
                                    color = ThemeTextSecondary,
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "= ${item.result}",
                                    color = GlowTeal,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onDelete(item) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete item",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Inline helper for dots of deg/rad
private fun Int.getDotSize(active: Boolean): androidx.compose.ui.unit.Dp {
    return if (active) 8.dp else 4.dp
}
