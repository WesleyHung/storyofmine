package com.unlone.app.data.story

import com.unlone.app.domain.entities.Story
import kotlinx.serialization.Serializable

sealed class StoryResult<T>(val data: T? = null, val errorMsg: String? = null) {
    class Success<T>(data: T? = null) : StoryResult<T>(data = data)
    class Failed<T>(errorMsg: String?) : StoryResult<T>(errorMsg = errorMsg)
}


@Serializable
data class AllTopicResponse(
    val data: List<Topic>,
)

@Serializable
data class StoryResponse(
    val id: String,
    val title: String,
    val content: String,
    val topic: String,
    val author: String,
    val isPublished: Boolean,
    val isSelfWritten: Boolean,
    val commentAllowed: Boolean,
    val saveAllowed: Boolean,
    val createdDate: String,
) {
    fun toStory(): Story {
        return Story(
            this.id,
            this.title,
            this.content,
            this.topic,
            this.author,
            this.isPublished,
            this.isSelfWritten,
            this.commentAllowed,
            this.saveAllowed,
            null,
            this.createdDate,
        )
    }
}

