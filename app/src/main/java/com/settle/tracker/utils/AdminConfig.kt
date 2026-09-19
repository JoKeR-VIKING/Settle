package com.settle.tracker.utils

/**
 * The one account allowed to see the in-app Issue Reports dashboard. This is
 * a UI-only gate (hides the entry point) — the real access boundary has to
 * be a Firestore security rule restricting reads of `issueReports` to this
 * same address, since any signed-in client can otherwise call the SDK
 * directly.
 */
const val ADMIN_EMAIL = "prathamvasani1@gmail.com"

fun isAdminUser(email: String?): Boolean =
    email != null && email.equals(ADMIN_EMAIL, ignoreCase = true)
