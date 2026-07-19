package com.gameside.ai

import android.content.Context
import android.content.Intent
import com.gameside.device.CompanionRestoreResult
import com.gameside.device.CompanionRestorer
import com.gameside.device.SecondaryDisplayLauncher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidCompanionRestorer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val launcher: SecondaryDisplayLauncher,
) : CompanionRestorer {
    override fun restoreCompanion(displayId: Int): CompanionRestoreResult = launcher.restore(
        context,
        Intent(context, CompanionActivity::class.java),
        displayId,
    )
}
