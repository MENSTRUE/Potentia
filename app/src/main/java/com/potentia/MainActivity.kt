package com.potentia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.potentia.ui.PotentiaApp
import com.potentia.ui.theme.PotentiaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PotentiaTheme {
                PotentiaApp()
            }
        }
    }
}