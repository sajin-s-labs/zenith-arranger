package com.zenitharranger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.zenitharranger.ui.ArrangerScreen
import com.zenitharranger.ui.ArrangerViewModel

/**
 * Main entry activity hosting the Zenith Arranger workstation.
 */
class MainActivity : ComponentActivity() {

    private val arrangerViewModel: ArrangerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArrangerScreen(viewModel = arrangerViewModel)
        }
    }
}
