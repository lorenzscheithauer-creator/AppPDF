package de.pdfwerkzeugkasten

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainFlowInstrumentedTest {
    @Test fun appPackageLoads() { assertTrue(BuildConfig.APPLICATION_ID.contains("pdfwerkzeugkasten")) }
}
