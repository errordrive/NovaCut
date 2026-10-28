package com.procut.repository

import com.procut.model.EditorProject
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
data class DraftMetadata(
    val id: String,
    val name: String,
    val coverImagePath: String? = null,
    val durationUs: Long,
    val updatedAtUs: Long,
    val trackCount: Int,
    val clipCount: Int
)

interface DraftRepository {
    fun saveDraft(project: EditorProject)
    fun loadDraft(projectId: String): EditorProject?
    fun listDrafts(): List<DraftMetadata>
    fun duplicateDraft(projectId: String, newName: String): EditorProject?
    fun deleteDraft(projectId: String): Boolean
}

class FileDraftRepository(
    private val baseDirectory: File
) : DraftRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    init {
        if (!baseDirectory.exists()) {
            baseDirectory.mkdirs()
        }
    }

    private fun getProjectFile(projectId: String): File {
        return File(baseDirectory, "$projectId.procut.json")
    }

    @Synchronized
    override fun saveDraft(project: EditorProject) {
        val updatedProject = project.copy(updatedAtUs = System.currentTimeMillis() * 1000L)
        val file = getProjectFile(updatedProject.id)
        val jsonString = json.encodeToString(updatedProject)
        file.writeText(jsonString, Charsets.UTF_8)
    }

    @Synchronized
    override fun loadDraft(projectId: String): EditorProject? {
        val file = getProjectFile(projectId)
        if (!file.exists()) return null
        return try {
            val content = file.readText(Charsets.UTF_8)
            json.decodeFromString<EditorProject>(content)
        } catch (e: Exception) {
            null
        }
    }

    @Synchronized
    override fun listDrafts(): List<DraftMetadata> {
        val files = baseDirectory.listFiles { file ->
            file.isFile && file.name.endsWith(".procut.json")
        } ?: return emptyList()

        return files.mapNotNull { file ->
            try {
                val content = file.readText(Charsets.UTF_8)
                val project = json.decodeFromString<EditorProject>(content)
                DraftMetadata(
                    id = project.id,
                    name = project.name,
                    durationUs = project.totalDurationUs,
                    updatedAtUs = project.updatedAtUs,
                    trackCount = project.tracks.size,
                    clipCount = project.tracks.sumOf { it.clips.size }
                )
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.updatedAtUs }
    }

    @Synchronized
    override fun duplicateDraft(projectId: String, newName: String): EditorProject? {
        val original = loadDraft(projectId) ?: return null
        val newId = UUID.randomUUID().toString()
        val duplicatedProject = original.copy(
            id = newId,
            name = newName,
            createdAtUs = System.currentTimeMillis() * 1000L,
            updatedAtUs = System.currentTimeMillis() * 1000L
        )
        saveDraft(duplicatedProject)
        return duplicatedProject
    }

    @Synchronized
    override fun deleteDraft(projectId: String): Boolean {
        val file = getProjectFile(projectId)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
}
