package com.gameside.data.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeystoreCredentialStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = KeystoreCredentialStore(context)

    @After
    fun cleanUp() = runBlocking { store.remove(TEST_IDENTIFIER) }

    @Test
    fun credentialRoundTripsThroughAndroidKeystoreEncryption() = runBlocking {
        store.put(TEST_IDENTIFIER, "temporary-test-value")

        assertTrue(store.contains(TEST_IDENTIFIER))
        assertEquals("temporary-test-value", store.get(TEST_IDENTIFIER))

        store.remove(TEST_IDENTIFIER)
        assertFalse(store.contains(TEST_IDENTIFIER))
    }

    private companion object {
        const val TEST_IDENTIFIER = "instrumentation.test"
    }
}
