package com.unlone.app.domain.useCases.write

import com.unlone.app.data.write.DraftRepository
import com.unlone.app.domain.entities.ChildDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class GetDraftAllVersionsUseCase(private val draftRepository: DraftRepository) {
    operator fun invoke(id: String): Flow<Map<String, List<ChildDraft>>> {
        return draftRepository.queryDraft(id).map { parent ->
            mapOf(parent.id to parent.childDrafts)
        }
    }
}