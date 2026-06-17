package com.example.ui.screens

import android.content.Context
import android.print.PrintManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import com.example.ui.NotebookViewModel
import com.example.ui.components.MathPrinter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookEditorScreen(
    viewModel: NotebookViewModel,
    onBackToList: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    val currentNotebook = viewModel.currentNotebook
    val pages = viewModel.currentNotebookPages.collectAsState()
    val currentPageIndex = viewModel.currentPageNumber

    var searchActive by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }

    if (currentNotebook == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No Notebook Selected")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searchActive) {
                        TextField(
                            value = viewModel.searchText,
                            onValueChange = { viewModel.searchText = it },
                            placeholder = { Text("Search on this page...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            trailingIcon = {
                                IconButton(onClick = {
                                    viewModel.searchText = ""
                                    searchActive = false
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Search")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_field")
                        )
                    } else {
                        Column(
                            modifier = Modifier.clickable {
                                renameInput = currentNotebook.name
                                showRenameDialog = true
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentNotebook.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Rename Notebook",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "Notebook: Math • Page $currentPageIndex of ${pages.value.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToList,
                        modifier = Modifier.testTag("editor_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to List")
                    }
                },
                actions = {
                    if (!searchActive) {
                        IconButton(onClick = { searchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search inside page")
                        }
                    }

                    // Print & PDF Export Button
                    IconButton(
                        onClick = {
                            val contentsList = pages.value.map { it.content }
                            if (contentsList.isEmpty()) {
                                Toast.makeText(context, "Nothing to print", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                            val jobName = "${context.packageName} - Math Notebook - ${currentNotebook.name}"
                            printManager.print(
                                jobName,
                                MathPrinter(context, currentNotebook.name, contentsList),
                                null
                            )
                        },
                        modifier = Modifier.testTag("action_print")
                    ) {
                        Text("⎙", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    // Theme toggle
                    IconButton(onClick = { viewModel.isDarkMode = !viewModel.isDarkMode }) {
                        Text(if (viewModel.isDarkMode) "☀️" else "🌙", fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        bottomBar = {
            MathSymbolToolbar(
                onSymbolClick = { symbol -> viewModel.insertSymbol(symbol) },
                onTabClick = { viewModel.insertSymbol("    ") },
                onUndoClick = { viewModel.undo() },
                onRedoClick = { viewModel.redo() },
                modifier = Modifier.testTag("math_symbol_toolbar")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (viewModel.isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF7F7F7))
        ) {
            // Zoom & Pagination Action Header Bar
            Surface(
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Zoom Controllers
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (viewModel.zoomPercent > 50) {
                                    viewModel.zoomPercent -= 10
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_zoom_out"),
                            colors = IconButtonDefaults.filledTonalIconButtonColors()
                        ) {
                            Text("−", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "${viewModel.zoomPercent}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        IconButton(
                            onClick = {
                                if (viewModel.zoomPercent < 200) {
                                    viewModel.zoomPercent += 10
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_zoom_in"),
                            colors = IconButtonDefaults.filledTonalIconButtonColors()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(16.dp))
                        }
                    }

                    // Pagination Control Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.prevPage() },
                            enabled = currentPageIndex > 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("◀", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "Page $currentPageIndex of ${pages.value.size}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        IconButton(
                            onClick = { viewModel.nextPage() },
                            enabled = currentPageIndex < pages.value.size,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("▶", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.width(8.dp))

                        // Add Page
                        IconButton(
                            onClick = { viewModel.addPage() },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_add_page"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text("+📄", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Delete page
                        IconButton(
                            onClick = { viewModel.deleteCurrentPage() },
                            enabled = pages.value.size > 1,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_delete_page"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text("✕📄", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Real physical Notebook Lined Paper Canvas Area
            val scrollState = rememberScrollState()
            val zoomFactor = viewModel.zoomPercent / 100f

            // Sizing metrics tied cleanly to the zoom scaling
            val baseLineSpacing = 36.dp
            val lineSpacingDp = baseLineSpacing * zoomFactor
            val marginPositionDp = 50.dp * zoomFactor

            val density = LocalDensity.current
            val lineSpacingPx = with(density) { lineSpacingDp.toPx() }
            val marginPositionPx = with(density) { marginPositionDp.toPx() }

            // Dynamic color adaptation to the eye-safe Dark vs Light modes
            val paperBgColor = if (viewModel.isDarkMode) Color(0xFF121212) else Color.White
            val horizontalLinesColor = if (viewModel.isDarkMode) Color(0xFF333333) else Color(0xFFD0E4F5)
            val redMarginColor = if (viewModel.isDarkMode) Color(0xFF552222) else Color(0xFFFFB2B2)
            val caretColor = if (viewModel.isDarkMode) Color(0xFFD0BCFF) else Color(0xFF6750A4)

            val baseFontSize = 21.sp
            val baseLineHeight = 36.sp
            val fontSizeScaled = baseFontSize * zoomFactor
            val lineHeightScaled = baseLineHeight * zoomFactor

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(if (viewModel.isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF0F0F0))
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(paperBgColor)
                        .verticalScroll(scrollState)
                        .drawBehind {
                            // 1. Draw horizontal notebook guidelines
                            var currentY = lineSpacingPx
                            while (currentY < size.height + 800f) { // draw ample empty space lines at bottom
                                drawLine(
                                    color = horizontalLinesColor,
                                    start = androidx.compose.ui.geometry.Offset(0f, currentY),
                                    end = androidx.compose.ui.geometry.Offset(size.width, currentY),
                                    strokeWidth = 1.dp.toPx()
                                )
                                currentY += lineSpacingPx
                            }

                            // 2. Draw traditional vertical red margin line
                            drawLine(
                                color = redMarginColor,
                                start = androidx.compose.ui.geometry.Offset(marginPositionPx, 0f),
                                end = androidx.compose.ui.geometry.Offset(marginPositionPx, size.height + 800f),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }
                ) {
                    // Custom search highlights filter
                    val visualTransformation = if (viewModel.searchText.isNotEmpty()) {
                        SearchHighlightTransformation(viewModel.searchText, viewModel.isDarkMode)
                    } else {
                        VisualTransformation.None
                    }

                    BasicTextField(
                        value = viewModel.currentText,
                        onValueChange = {
                            viewModel.onTextValueChange(it)
                        },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace, // Keep stable columns alignments
                            fontSize = fontSizeScaled,
                            lineHeight = lineHeightScaled,
                            color = if (viewModel.isDarkMode) Color.White else Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 600.dp) // guarantee tall scrolling area
                            .padding(
                                start = marginPositionDp + 12.dp,
                                end = 16.dp,
                                top = lineSpacingDp / 4,
                                bottom = 200.dp // extra scrolling padding
                            )
                            .testTag("math_notebook_editor"),
                        cursorBrush = SolidColor(caretColor),
                        visualTransformation = visualTransformation
                    )
                }
            }
        }
    }

    // Rename dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Notebook") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Notebook Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rename_input_field")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            viewModel.renameNotebook(currentNotebook.id, renameInput.trim())
                            showRenameDialog = false
                        }
                    },
                    modifier = Modifier.testTag("btn_rename_confirm")
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Global text generator helper
fun buildAnnotatedMathText(text: String, query: String, isDarkMode: Boolean): AnnotatedString {
    return buildAnnotatedString {
        if (query.isEmpty()) {
            append(text)
            return@buildAnnotatedString
        }
        var startIdx = 0
        while (startIdx < text.length) {
            val matchIdx = text.indexOf(query, startIdx, ignoreCase = true)
            if (matchIdx == -1) {
                append(text.substring(startIdx))
                break
            }
            append(text.substring(startIdx, matchIdx))
            withStyle(
                SpanStyle(
                    background = if (isDarkMode) Color(0xFF6E5E00) else Color(0xFFFFF59D),
                    color = if (isDarkMode) Color.White else Color.Black
                )
            ) {
                append(text.substring(matchIdx, matchIdx + query.length))
            }
            startIdx = matchIdx + query.length
        }
    }
}

class SearchHighlightTransformation(
    private val query: String,
    private val isDarkMode: Boolean
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = buildAnnotatedMathText(text.text, query, isDarkMode)
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}

@Composable
fun MathSymbolToolbar(
    onSymbolClick: (String) -> Unit,
    onTabClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val symbols = listOf(
        "²", "³", "⁴", "ⁿ", "ˣ", "ⁱ", "⁺", "⁻", "⁼",
        "₀", "₁", "₂", "₃", "₄", "ₓ",
        "+", "−", "×", "÷", "=", "≠", "≈", "±", "√", "∛", "∕",
        "½", "⅓", "⅔", "¼", "¾",
        "π", "θ", "α", "β", "γ", "Δ", "Σ", "∞", "°",
        "(", ")", "[", "]", "{", "}"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick utilities on the left (Undo, Redo, Tab)
                IconButton(
                    onClick = onUndoClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Text("⟲", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    onClick = onRedoClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Text("⟳", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .height(30.dp)
                        .clickable { onTabClick() }
                        .padding(horizontal = 10.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "⇥ Tab",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(Modifier.width(6.dp))
                VerticalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier
                        .height(20.dp)
                        .width(1.dp)
                )
                Spacer(Modifier.width(6.dp))

                // Scrollable symbols list
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("math_symbol_toolbar_row")
                ) {
                    items(symbols) { symbol ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .height(30.dp)
                                .clickable { onSymbolClick(symbol) }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .fillMaxHeight()
                            ) {
                                Text(
                                    text = symbol,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
