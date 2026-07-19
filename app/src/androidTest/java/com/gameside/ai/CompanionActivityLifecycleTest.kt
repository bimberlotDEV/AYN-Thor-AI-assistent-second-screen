package com.gameside.ai

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gameside.device.CompanionSessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompanionActivityLifecycleTest {
    @Test
    fun companionManifestUsesRetainedSingleTopTask() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        @Suppress("DEPRECATION")
        val info = context.packageManager.getActivityInfo(ComponentName(context, CompanionActivity::class.java), 0)
        assertEquals(ActivityInfo.LAUNCH_SINGLE_TOP, info.launchMode)
        assertTrue(info.flags and ActivityInfo.FLAG_ALWAYS_RETAIN_TASK_STATE != 0)
        assertTrue(info.taskAffinity.endsWith(".companion"))
    }

    @Test
    fun onStopDoesNotEndCompanionSession() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, CompanionActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
        )
        ActivityScenario.launch<CompanionActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.sessionCoordinator.state.value.sessionActive)
                assertEquals(CompanionSessionStatus.visible, activity.sessionCoordinator.state.value.status)
            }
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.onActivity { activity -> assertTrue(activity.sessionCoordinator.state.value.sessionActive) }
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity { activity -> activity.sessionCoordinator.stopSession() }
        }
    }
}
