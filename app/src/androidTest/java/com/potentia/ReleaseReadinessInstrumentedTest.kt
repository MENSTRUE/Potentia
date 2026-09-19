package com.potentia

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReleaseReadinessInstrumentedTest {

    @Test
    fun appDoesNotRequestInternetPermission() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val info = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val requested = info.requestedPermissions?.toSet().orEmpty()
        assertFalse(Manifest.permission.INTERNET in requested)
    }

    @Test
    fun bundledCoreAssetsArePresentAndNonEmpty() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val modelBytes = context.assets.open("potentia_ai/creative_tfidf_ridge_v1.json").use { it.readBytes().size }
        val itemBankBytes = context.assets.open("potentia_assessment/item_bank_v1_1.json").use { it.readBytes().size }

        assertTrue(modelBytes > 100_000)
        assertTrue(itemBankBytes > 10_000)
    }

    @Test
    fun appVersionIsReleaseCandidateOrNewer() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val version = context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
            .orEmpty()
        assertTrue(version.isNotBlank())
        assertTrue(version != "0.1.0")
    }
}
