package com.example.opencut.data

import com.example.opencut.domain.CanvasSettings
import com.example.opencut.domain.ProjectDocument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.util.UUID

data class ProjectSummary(
    val id: String,
    val name: String,
    val updatedAt: Long,
    val width: Int,
    val height: Int,
    val clipCount: Int,
    val durationTicks: Long
)

class ProjectRepository(private val dao: ProjectDao) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val projects: Flow<List<ProjectSummary>> = dao.observeAll().map { entities ->
        entities.map { entity ->
            ProjectSummary(
                id = entity.id,
                name = entity.name,
                updatedAt = entity.updatedAt,
                width = entity.width,
                height = entity.height,
                clipCount = entity.clipCount,
                durationTicks = entity.durationTicks
            )
        }
    }

    suspend fun getProject(id: String): ProjectDocument? {
        val entity = dao.getById(id) ?: return null
        return runCatching {
            json.decodeFromString<ProjectDocument>(entity.documentJson)
        }.getOrNull()
    }

    suspend fun createProject(name: String, width: Int, height: Int): ProjectDocument {
        val doc = ProjectDocument(
            id = UUID.randomUUID().toString(),
            name = name,
            updatedAt = System.currentTimeMillis(),
            canvas = CanvasSettings(width = width, height = height)
        )
        saveProject(doc)
        return doc
    }

    suspend fun saveProject(document: ProjectDocument) {
        val stamped = document.copy(updatedAt = System.currentTimeMillis())
        val entity = ProjectEntity(
            id = stamped.id,
            name = stamped.name,
            updatedAt = stamped.updatedAt,
            width = stamped.canvas.width,
            height = stamped.canvas.height,
            clipCount = stamped.clips.size,
            durationTicks = stamped.totalDurationTicks(),
            documentJson = json.encodeToString(ProjectDocument.serializer(), stamped)
        )
        dao.upsert(entity)
    }

    suspend fun duplicateProject(id: String): ProjectDocument? {
        val existing = getProject(id) ?: return null
        val copy = existing.copy(
            id = UUID.randomUUID().toString(),
            name = "${existing.name} Copy",
            updatedAt = System.currentTimeMillis()
        )
        saveProject(copy)
        return copy
    }

    suspend fun deleteProject(id: String) {
        dao.deleteById(id)
    }
}
