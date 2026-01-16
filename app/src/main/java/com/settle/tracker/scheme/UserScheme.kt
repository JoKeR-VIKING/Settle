package com.settle.tracker.scheme

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserScheme(
    val upiId: String = "",
)
