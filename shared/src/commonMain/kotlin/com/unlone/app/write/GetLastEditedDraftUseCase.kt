package com.unlone.app.write

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetLastEditedDraftUseCase(private val draftRepository: DraftRepository) {

    operator fun invoke(): Flow<Pair<String, ChildDraft>?> {
        return draftRepository.getLastEditedDraft().map {
            it?.let { it1 ->
                val latestDraft = it.childDrafts.maxByOrNull { it2 -> it2.timeStamp }!!
                Pair(it1.id.toString(), latestDraft)
            }
        }
    }
}