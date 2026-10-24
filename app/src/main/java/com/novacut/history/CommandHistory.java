package com.novacut.history;

import com.novacut.model.ProjectDocument;

import java.util.Stack;

public class CommandHistory {
    private final Stack<EditorCommand> undoStack;
    private final Stack<EditorCommand> redoStack;
    private final int maxHistorySize;

    public CommandHistory() {
        this(50);
    }

    public CommandHistory(int maxHistorySize) {
        this.undoStack = new Stack<>();
        this.redoStack = new Stack<>();
        this.maxHistorySize = maxHistorySize;
    }

    public void executeCommand(ProjectDocument project, EditorCommand command) {
        command.execute(project);
        undoStack.push(command);
        redoStack.clear();

        if (undoStack.size() > maxHistorySize) {
            undoStack.remove(0);
        }
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public EditorCommand undo(ProjectDocument project) {
        if (!canUndo()) return null;
        EditorCommand command = undoStack.pop();
        command.undo(project);
        redoStack.push(command);
        return command;
    }

    public EditorCommand redo(ProjectDocument project) {
        if (!canRedo()) return null;
        EditorCommand command = redoStack.pop();
        command.execute(project);
        undoStack.push(command);
        return command;
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
