package com.novacut.history;

import com.novacut.model.ProjectDocument;

public interface EditorCommand {
    String getName();
    void execute(ProjectDocument project);
    void undo(ProjectDocument project);
}
