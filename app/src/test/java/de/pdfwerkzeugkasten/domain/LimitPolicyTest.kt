package de.pdfwerkzeugkasten.domain

import de.pdfwerkzeugkasten.domain.model.UserPlan
import de.pdfwerkzeugkasten.domain.usecase.LimitPolicy
import org.junit.Assert.assertTrue
import org.junit.Test

class LimitPolicyTest {
    private val policy = LimitPolicy()

    @Test fun freeUsersCanUseAllConfiguredPdfChecks() {
        assertTrue(policy.canCompress(UserPlan.FREE, Long.MAX_VALUE))
        assertTrue(policy.canMerge(UserPlan.FREE, 500))
        assertTrue(policy.canConvertImages(UserPlan.FREE, 500))
        assertTrue(policy.batchEnabled(UserPlan.FREE))
    }

    @Test fun proDoesNotUnlockExtraPdfCapacity() {
        assertTrue(policy.canCompress(UserPlan.PRO, Long.MAX_VALUE))
        assertTrue(policy.canMerge(UserPlan.PRO, 500))
        assertTrue(policy.canConvertImages(UserPlan.PRO, 500))
        assertTrue(policy.batchEnabled(UserPlan.PRO))
    }
}
