package com.gameside.ai

import android.app.Activity
import android.os.Bundle

/**
 * Keeps a lightweight task on display 0. AYN Thor is more stable when the primary display owns a
 * task before GameSide remains active below. The activity never renders content and immediately
 * moves behind the user's current primary app or launcher.
 */
class PrimaryAnchorActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        moveTaskToBack(true)
    }
}
