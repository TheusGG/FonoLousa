package com.fonolousa.app.data

import android.content.Context
import com.fonolousa.app.data.local.ClinicalResultEntity
import com.fonolousa.app.data.local.FonoLocalDatabase
import com.fonolousa.app.data.local.ItemProgressEntity
import com.fonolousa.app.data.local.SessionEventEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class SessionRepository(context: Context) {
    private val dao = FonoLocalDatabase.get(context).dao()
    private val sessionId = UUID.randomUUID().toString()

    val progress: Flow<List<ItemProgressEntity>> = dao.observeProgress()
    val favorites: Flow<List<ItemProgressEntity>> = dao.observeFavorites()
    val recentEvents: Flow<List<SessionEventEntity>> = dao.observeRecentEvents()
    val clinicalResults: Flow<List<ClinicalResultEntity>> = dao.observeClinicalResults()

    suspend fun recordView(categoryId: String, level: Int, item: ItemFono) {
        updateProgress(categoryId, level, item, eventType = "view") { existing, now ->
            existing.copy(views = existing.views + 1, lastSeenAt = now, updatedAt = now)
        }
    }

    suspend fun recordPlay(categoryId: String, level: Int, item: ItemFono) {
        updateProgress(categoryId, level, item, eventType = "play") { existing, now ->
            existing.copy(plays = existing.plays + 1, lastSeenAt = now, updatedAt = now)
        }
    }

    suspend fun setFavorite(categoryId: String, level: Int, item: ItemFono, favorite: Boolean) {
        updateProgress(categoryId, level, item, eventType = if (favorite) "favorite" else "unfavorite") { existing, now ->
            existing.copy(isFavorite = favorite, updatedAt = now)
        }
    }

    suspend fun recordClinicalResult(
        childName: String,
        clinicalSessionId: String,
        activity: String,
        categoryId: String,
        level: Int,
        item: ItemFono,
        isCorrect: Boolean
    ) {
        val now = System.currentTimeMillis()
        dao.insertClinicalResult(
            ClinicalResultEntity(
                sessionId = clinicalSessionId,
                childName = sanitizeChildName(childName),
                activity = activity,
                categoryId = categoryId,
                level = level,
                itemId = item.id,
                word = if (level == 4) item.frase else item.palavra,
                isCorrect = isCorrect,
                createdAt = now
            )
        )
        updateProgress(categoryId, level, item, eventType = if (isCorrect) "clinical_correct" else "clinical_error") { existing, _ ->
            existing.copy(lastSeenAt = now, updatedAt = now)
        }
    }

    suspend fun updateClinicalResult(resultId: Long, isCorrect: Boolean) {
        dao.updateClinicalResult(resultId, isCorrect)
    }

    suspend fun deleteClinicalResult(resultId: Long) {
        dao.deleteClinicalResult(resultId)
    }

    suspend fun deleteClinicalResults(resultIds: List<Long>) {
        if (resultIds.isNotEmpty()) {
            dao.deleteClinicalResults(resultIds)
        }
    }

    private suspend fun updateProgress(
        categoryId: String,
        level: Int,
        item: ItemFono,
        eventType: String,
        transform: (ItemProgressEntity, Long) -> ItemProgressEntity
    ) {
        val now = System.currentTimeMillis()
        val key = itemKey(categoryId, level, item.id)
        val current = dao.getProgress(key) ?: ItemProgressEntity(
            itemKey = key,
            categoryId = categoryId,
            level = level,
            itemId = item.id,
            word = if (level == 4) item.frase else item.palavra
        )
        dao.saveProgress(transform(current, now))
        dao.insertEvent(
            SessionEventEntity(
                sessionId = sessionId,
                categoryId = categoryId,
                level = level,
                itemId = item.id,
                word = if (level == 4) item.frase else item.palavra,
                eventType = eventType,
                createdAt = now
            )
        )
    }

    companion object {
        fun itemKey(categoryId: String, level: Int, itemId: String): String = "$categoryId:$level:$itemId"
    }
}

private fun sanitizeChildName(name: String): String =
    name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { "Crianca" }
