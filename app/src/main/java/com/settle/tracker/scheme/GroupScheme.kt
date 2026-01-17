package com.settle.tracker.scheme

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class GroupScheme(
    val id: String = "",
    val groupName: String = "",
    val members: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val createdBy: String = ""
)
