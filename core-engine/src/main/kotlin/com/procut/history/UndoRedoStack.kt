package com.procut.history

import com.procut.model.EditorProject

data class HistoryEntry(
    val state: EditorProject,
    val actionLabel: String,
    val timestampMs: Long = System.currentTimeMillis()
)

class UndoRedoStack(private val maxCapacity: Int = 100) {

    private val undoStack = ArrayDeque<HistoryEntry>()
    private val redoStack = ArrayDeque<HistoryEntry>()

    @Synchronized
    fun push(state: EditorProject, actionLabel: String) {
        if (undoStack.size >= maxCapacity) {
            undoStack.removeFirst()
        }
        undoStack.addLast(HistoryEntry(state, actionLabel))
        redoStack.clear()
    }

    @Synchronized
    fun undo(currentState: EditorProject): Pair<EditorProject, String>? {
        if (undoStack.isEmpty()) return null
        val previousEntry = undoStack.removeLast()
        redoStack.addLast(HistoryEntry(currentState, previousEntry.actionLabel))
        return Pair(previousEntry.state, previousEntry.actionLabel)
    }

    @Synchronized
    fun redo(currentState: EditorProject): Pair<EditorProject, String>? {
        if (redoStack.isEmpty()) return null
        val nextEntry = redoStack.removeLast()
        undoStack.addLast(HistoryEntry(currentState, nextEntry.actionLabel))
        return Pair(nextEntry.state, nextEntry.actionLabel)
    }

    val canUndo: Boolean
        @Synchronized get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        @Synchronized get() = redoStack.isNotEmpty()

    val undoHistoryLabels: List<String>
        @Synchronized get() = undoStack.map { it.actionLabel }

    val redoHistoryLabels: List<String>
        @Synchronized get() = redoStack.map { it.actionLabel }

    @Synchronized
    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
