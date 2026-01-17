package com.settle.tracker.scheme

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserScheme(
    val id: String = "",
    val upiId: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val photoUrl: String = ""
)
