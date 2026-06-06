package de.pdfwerkzeugkasten.domain

import de.pdfwerkzeugkasten.domain.model.UserPlan
import de.pdfwerkzeugkasten.domain.usecase.LimitPolicy
import org.junit.Assert.*
import org.junit.Test

class LimitPolicyTest {
    private val policy = LimitPolicy()
    @Test fun freeLimitsAreEnforced() { assertTrue(policy.canCompress(UserPlan.FREE, 25L*1024*1024)); assertFalse(policy.canCompress(UserPlan.FREE, 26L*1024*1024)); assertFalse(policy.canMerge(UserPlan.FREE, 6)); assertFalse(policy.canConvertImages(UserPlan.FREE, 11)) }
    @Test fun proIsUnlimitedForConfiguredChecks() { assertTrue(policy.canCompress(UserPlan.PRO, Long.MAX_VALUE)); assertTrue(policy.canMerge(UserPlan.PRO, 500)); assertTrue(policy.batchEnabled(UserPlan.PRO)) }
}
