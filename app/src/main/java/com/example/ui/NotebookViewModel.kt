package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Notebook
import com.example.data.Page
import com.example.data.MathRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotebookViewModel(private val repository: MathRepository) : ViewModel() {

    // All available notebooks
    val allNotebooks: StateFlow<List<Notebook>> = repository.allNotebooks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Editing State
    var currentNotebook by mutableStateOf<Notebook?>(null)
        private set

    private val _currentNotebookPages = MutableStateFlow<List<Page>>(emptyList())
    val currentNotebookPages: StateFlow<List<Page>> = _currentNotebookPages.asStateFlow()

    // Current page being edited (1-indexed)
    var currentPageNumber by mutableStateOf(1)
        private set

    // Text editor state (custom input cursor tracker)
    var currentText by mutableStateOf(TextFieldValue(""))
        private set

    // Configuration states
    var zoomPercent by mutableStateOf(100)
    var isDarkMode by mutableStateOf(false)
    var searchText by mutableStateOf("")

    // Undo & Redo Histories
    private val undoStack = mutableListOf<TextFieldValue>()
    private val redoStack = mutableListOf<TextFieldValue>()
    private var lastUndoTrackTime = 0L

    private var autoSaveJob: Job? = null

    // Track state of page collection
    private var pageObserveJob: Job? = null

    fun selectNotebook(notebook: Notebook?) {
        // Save current page immediately before transitioning
        val prevNotebook = currentNotebook
        if (prevNotebook != null) {
            viewModelScope.launch {
                savePageImmediately()
            }
        }

        currentNotebook = notebook
        currentPageNumber = 1
        undoStack.clear()
        redoStack.clear()

        pageObserveJob?.cancel()
        if (notebook != null) {
            pageObserveJob = viewModelScope.launch {
                repository.getPagesForNotebook(notebook.id).collect { pages ->
                    _currentNotebookPages.value = pages
                    // Load the content of the current selected page
                    val curPage = pages.find { it.pageNumber == currentPageNumber }
                    if (curPage != null) {
                        // Avoid resetting the TextFieldValue if the content is the same (keeps cursor position)
                        if (currentText.text != curPage.content) {
                            currentText = TextFieldValue(
                                text = curPage.content,
                                selection = TextRange(curPage.content.length)
                            )
                        }
                    } else {
                        // If pages list isn't empty but current page is missing, load the first one
                        if (pages.isNotEmpty()) {
                            currentPageNumber = pages.first().pageNumber
                            currentText = TextFieldValue(
                                text = pages.first().content,
                                selection = TextRange(pages.first().content.length)
                            )
                        }
                    }
                }
            }
        } else {
            _currentNotebookPages.value = emptyList()
            currentText = TextFieldValue("")
        }
    }

    fun createNotebook(name: String) {
        viewModelScope.launch {
            val newId = repository.createNotebook(name)
            val created = repository.getNotebookById(newId)
            if (created != null) {
                selectNotebook(created)
            }
        }
    }

    fun renameNotebook(notebookId: Long, newName: String) {
        viewModelScope.launch {
            repository.renameNotebook(notebookId, newName)
            if (currentNotebook?.id == notebookId) {
                currentNotebook = currentNotebook?.copy(name = newName)
            }
        }
    }

    fun deleteNotebook(notebook: Notebook) {
        viewModelScope.launch {
            if (currentNotebook?.id == notebook.id) {
                selectNotebook(null)
            }
            repository.deleteNotebook(notebook)
        }
    }

    // Page Management
    fun addPage() {
        val notebook = currentNotebook ?: return
        viewModelScope.launch {
            savePageImmediately()
            val newPage = repository.addPage(notebook.id)
            currentPageNumber = newPage.pageNumber
            currentText = TextFieldValue("")
            undoStack.clear()
            redoStack.clear()
        }
    }

    fun deleteCurrentPage() {
        val notebook = currentNotebook ?: return
        val pages = _currentNotebookPages.value
        val toDelete = pages.find { it.pageNumber == currentPageNumber } ?: return

        viewModelScope.launch {
            repository.deletePageAndReindex(toDelete)
            // Navigate safely to a neighboring page
            val remainingCount = pages.size - 1
            if (currentPageNumber > remainingCount && currentPageNumber > 1) {
                currentPageNumber = remainingCount
            } else if (currentPageNumber < 1) {
                currentPageNumber = 1
            }
            // Trigger selection reload
            val actual = repository.getPageByNumber(notebook.id, currentPageNumber)
            currentText = TextFieldValue(actual?.content ?: "", TextRange(actual?.content?.length ?: 0))
            undoStack.clear()
            redoStack.clear()
        }
    }

    fun setPage(pageNo: Int) {
        val notebook = currentNotebook ?: return
        val pages = _currentNotebookPages.value
        val target = pages.find { it.pageNumber == pageNo } ?: return
        
        viewModelScope.launch {
            savePageImmediately()
            currentPageNumber = pageNo
            currentText = TextFieldValue(target.content, TextRange(target.content.length))
            undoStack.clear()
            redoStack.clear()
        }
    }

    fun nextPage() {
        val pages = _currentNotebookPages.value
        if (currentPageNumber < pages.size) {
            setPage(currentPageNumber + 1)
        }
    }

    fun prevPage() {
        if (currentPageNumber > 1) {
            setPage(currentPageNumber - 1)
        }
    }

    // Custom text change tracking
    fun onTextValueChange(newValue: TextFieldValue) {
        val oldText = currentText.text
        if (oldText != newValue.text) {
            val currentTime = System.currentTimeMillis()
            val textLengthDiff = Math.abs(newValue.text.length - oldText.length)
            val isWordBoundary = newValue.text.endsWith(" ") || newValue.text.endsWith("\n") || (newValue.text.contains('\n') && !oldText.contains('\n'))
            
            if (currentTime - lastUndoTrackTime > 1500L || isWordBoundary || textLengthDiff > 5) {
                // Save current state to undo stack before updating
                addToUndoHistory()
                lastUndoTrackTime = currentTime
            }
            triggerAutoSave()
        }
        currentText = newValue
    }

    private fun triggerAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(800) // Debounce auto-save
            savePageImmediately()
        }
    }

    private suspend fun savePageImmediately() {
        val notebook = currentNotebook ?: return
        val pageNo = currentPageNumber
        val pages = _currentNotebookPages.value
        val page = pages.find { it.pageNumber == pageNo }
        val textToSave = currentText.text
        if (page != null) {
            repository.savePage(page.copy(content = textToSave))
        } else {
            // Keep safe in case DB page was not loaded yet
            val dbPage = repository.getPageByNumber(notebook.id, pageNo)
            if (dbPage != null) {
                repository.savePage(dbPage.copy(content = textToSave))
            }
        }
    }

    // Keyboard Insertion actions
    private fun addToUndoHistory() {
        // Cap undo stack size to prevent excess memory consumption
        if (undoStack.size > 50) {
            undoStack.removeAt(0)
        }
        undoStack.add(currentText)
        redoStack.clear()
    }

    fun insertSymbol(symbol: String) {
        addToUndoHistory()
        val text = currentText.text
        val selection = currentText.selection

        val before = text.substring(0, selection.start)
        val after = text.substring(selection.end)
        val newText = before + symbol + after
        val newCursor = selection.start + symbol.length

        currentText = TextFieldValue(
            text = newText,
            selection = TextRange(newCursor)
        )
        triggerAutoSave()
    }

    fun insertTemplate(beforeText: String, afterText: String) {
        addToUndoHistory()
        val text = currentText.text
        val selection = currentText.selection

        val beforeSection = text.substring(0, selection.start)
        val middleSection = text.substring(selection.start, selection.end)
        val afterSection = text.substring(selection.end)

        val newText = beforeSection + beforeText + middleSection + afterText + afterSection
        val newCursor = selection.start + beforeText.length + middleSection.length

        currentText = TextFieldValue(
            text = newText,
            selection = TextRange(newCursor)
        )
        triggerAutoSave()
    }

    fun backspace() {
        val text = currentText.text
        val selection = currentText.selection
        if (selection.start > 0 || !selection.collapsed) {
            addToUndoHistory()
            val before = if (!selection.collapsed) text.substring(0, selection.start) else text.substring(0, selection.start - 1)
            val after = text.substring(selection.end)
            val newText = before + after
            val newCursor = if (!selection.collapsed) selection.start else selection.start - 1

            currentText = TextFieldValue(
                text = newText,
                selection = TextRange(newCursor)
            )
            triggerAutoSave()
        }
    }

    fun deleteLine() {
        val text = currentText.text
        val cursor = currentText.selection.start
        if (text.isEmpty()) return

        addToUndoHistory()

        // Locate start of line
        var startOfLine = text.lastIndexOf('\n', cursor - 1)
        startOfLine = if (startOfLine == -1) 0 else startOfLine + 1

        // Locate end of line
        var endOfLine = text.indexOf('\n', cursor)
        endOfLine = if (endOfLine == -1) text.length else endOfLine + 1 // delete the newline as well to pull next line up!

        val before = text.substring(0, startOfLine)
        val after = text.substring(endOfLine)
        val newText = before + after
        val newCursor = startOfLine.coerceAtMost(newText.length)

        currentText = TextFieldValue(
            text = newText,
            selection = TextRange(newCursor)
        )
        triggerAutoSave()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(currentText)
            currentText = undoStack.removeAt(undoStack.lastIndex)
            triggerAutoSave()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(currentText)
            currentText = redoStack.removeAt(redoStack.lastIndex)
            triggerAutoSave()
        }
    }

    // Cursor navigation helpers
    fun moveCursorLeft() {
        val currentCursor = currentText.selection.start
        if (currentCursor > 0) {
            currentText = currentText.copy(selection = TextRange(currentCursor - 1))
        }
    }

    fun moveCursorRight() {
        val currentCursor = currentText.selection.start
        if (currentCursor < currentText.text.length) {
            currentText = currentText.copy(selection = TextRange(currentCursor + 1))
        }
    }

    fun moveCursorUp() {
        val text = currentText.text
        val cursor = currentText.selection.start
        val lines = text.split('\n')
        var cumulativeIndex = 0
        var currentLineIdx = -1
        var currentCol = -1

        for (i in lines.indices) {
            val lineLength = lines[i].length
            if (cursor >= cumulativeIndex && cursor <= cumulativeIndex + lineLength) {
                currentLineIdx = i
                currentCol = cursor - cumulativeIndex
                break
            }
            cumulativeIndex += lineLength + 1
        }

        if (currentLineIdx > 0) {
            var targetIndex = 0
            for (i in 0 until currentLineIdx - 1) {
                targetIndex += lines[i].length + 1
            }
            val prevLineLen = lines[currentLineIdx - 1].length
            val targetCol = currentCol.coerceAtMost(prevLineLen)
            currentText = currentText.copy(selection = TextRange(targetIndex + targetCol))
        }
    }

    fun moveCursorDown() {
        val text = currentText.text
        val cursor = currentText.selection.start
        val lines = text.split('\n')
        var cumulativeIndex = 0
        var currentLineIdx = -1
        var currentCol = -1

        for (i in lines.indices) {
            val lineLength = lines[i].length
            if (cursor >= cumulativeIndex && cursor <= cumulativeIndex + lineLength) {
                currentLineIdx = i
                currentCol = cursor - cumulativeIndex
                break
            }
            cumulativeIndex += lineLength + 1
        }

        if (currentLineIdx != -1 && currentLineIdx < lines.lastIndex) {
            var targetIndex = 0
            for (i in 0 .. currentLineIdx) {
                targetIndex += lines[i].length + 1
            }
            val nextLineLen = lines[currentLineIdx + 1].length
            val targetCol = currentCol.coerceAtMost(nextLineLen)
            currentText = currentText.copy(selection = TextRange(targetIndex + targetCol))
        }
    }

    override fun onCleared() {
        autoSaveJob?.cancel()
        pageObserveJob?.cancel()
        super.onCleared()
    }
}
