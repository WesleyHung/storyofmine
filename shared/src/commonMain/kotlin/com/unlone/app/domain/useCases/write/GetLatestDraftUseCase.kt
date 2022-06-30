package com.unlone.app.domain.useCases.write

import com.unlone.app.domain.entities.ChildDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetLatestDraftUseCase(private val getDraftAllVersionsUseCase: GetDraftAllVersionsUseCase) {
    operator fun invoke(id: String): Flow<Map<String, ChildDraft?>> {
        return getDraftAllVersionsUseCase(id).map {
            it.mapValues { it1 ->
                it1.value.maxByOrNull { it2 -> it2.timeStamp }
            }
        }
    }
}

