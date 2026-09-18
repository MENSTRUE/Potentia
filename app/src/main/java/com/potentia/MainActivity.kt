package com.potentia

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.potentia.reminder.WeeklyReminderWorker
import com.potentia.ui.PotentiaApp
import com.potentia.ui.theme.PotentiaTheme

class MainActivity : ComponentActivity() {

    private var growthOpenRequest by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        setContent {
            PotentiaTheme {
                PotentiaApp(growthOpenRequest = growthOpenRequest)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(WeeklyReminderWorker.EXTRA_OPEN_GROWTH, false) == true) {
            growthOpenRequest += 1
            intent.removeExtra(WeeklyReminderWorker.EXTRA_OPEN_GROWTH)
        }
    }
}
