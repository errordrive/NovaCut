package com.procut

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.procut.engine.TimelineEngine
import com.procut.history.UndoRedoStack
import com.procut.model.CanvasConfig
import com.procut.model.EditorProject
import com.procut.model.Track
import com.procut.model.TrackType
import com.procut.ui.theme.ProCutTheme
import java.util.UUID

class MainActivity : ComponentActivity() {

    private val engine = TimelineEngine()
    private val undoRedoStack = UndoRedoStack()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val defaultProject = EditorProject(
            id = UUID.randomUUID().toString(),
            name = "New ProCut Project",
            canvasConfig = CanvasConfig(),
            tracks = listOf(
                Track(id = UUID.randomUUID().toString(), type = TrackType.MAIN_VIDEO, name = "Main Video"),
                Track(id = UUID.randomUUID().toString(), type = TrackType.PIP_OVERLAY, name = "Overlay 1"),
                Track(id = UUID.randomUUID().toString(), type = TrackType.AUDIO, name = "Audio 1")
            )
        )

        setContent {
            ProCutTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EditorScreen(defaultProject = defaultProject)
                }
            }
        }
    }
}

@Composable
fun EditorScreen(defaultProject: EditorProject) {
    var project by remember { mutableStateOf(defaultProject) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = project.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Aspect Ratio: ${project.canvasConfig.aspectRatio.label}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Total Duration: ${project.totalDurationUs / 1_000_000.0}s | Tracks: ${project.tracks.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
