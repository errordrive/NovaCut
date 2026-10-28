package com.procut

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.procut.repository.FileDraftRepository
import com.procut.ui.screens.EditorScreen
import com.procut.ui.screens.HomeScreen
import com.procut.ui.theme.ProCutTheme
import com.procut.ui.viewmodel.EditorViewModel
import java.io.File

enum class AppScreen {
    HOME,
    EDITOR
}

class MainActivity : ComponentActivity() {

    private val editorViewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProCutTheme {
                val context = LocalContext.current
                val repository = remember {
                    FileDraftRepository(File(context.filesDir, "drafts"))
                }

                var currentScreen by remember { mutableStateOf(AppScreen.HOME) }

                BackHandler(enabled = currentScreen == AppScreen.EDITOR) {
                    // Auto-save project upon exiting editor
                    repository.saveDraft(editorViewModel.project.value)
                    currentScreen = AppScreen.HOME
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreen) {
                        AppScreen.HOME -> {
                            HomeScreen(
                                onOpenProject = { project ->
                                    editorViewModel.loadProject(project)
                                    currentScreen = AppScreen.EDITOR
                                },
                                onNewProject = { project ->
                                    editorViewModel.loadProject(project)
                                    currentScreen = AppScreen.EDITOR
                                }
                            )
                        }

                        AppScreen.EDITOR -> {
                            EditorScreen(
                                viewModel = editorViewModel,
                                onBackToDrafts = {
                                    repository.saveDraft(editorViewModel.project.value)
                                    currentScreen = AppScreen.HOME
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
