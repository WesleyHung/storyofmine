package com.unlone.app.domain.entities

data class Comment(
    val cid: String,
    val username: String,
    val text: String,
    val createdTime: String,
)
