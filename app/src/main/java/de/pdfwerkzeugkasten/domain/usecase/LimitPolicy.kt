package de.pdfwerkzeugkasten.domain.usecase

import de.pdfwerkzeugkasten.domain.model.UserPlan

class LimitPolicy {
    fun canCompress(plan: UserPlan, bytes: Long) = plan == UserPlan.PRO || bytes <= FREE_MAX_PDF_BYTES
    fun canMerge(plan: UserPlan, count: Int) = plan == UserPlan.PRO || count <= FREE_MAX_MERGE_COUNT
    fun canConvertImages(plan: UserPlan, count: Int) = plan == UserPlan.PRO || count <= FREE_MAX_IMAGE_COUNT
    fun batchEnabled(plan: UserPlan) = plan == UserPlan.PRO
    companion object { const val FREE_MAX_PDF_BYTES = 25L * 1024L * 1024L; const val FREE_MAX_MERGE_COUNT = 5; const val FREE_MAX_IMAGE_COUNT = 10 }
}
