package de.pdfwerkzeugkasten.domain.usecase

import de.pdfwerkzeugkasten.domain.model.UserPlan

/**
 * Product policy: every PDF function is free for every user.
 * The plan is kept only for ad visibility elsewhere in the app.
 */
class LimitPolicy {
    fun canCompress(plan: UserPlan, bytes: Long) = true
    fun canMerge(plan: UserPlan, count: Int) = true
    fun canConvertImages(plan: UserPlan, count: Int) = true
    fun batchEnabled(plan: UserPlan) = true
}
