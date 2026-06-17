package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.MathDatabase
import com.example.data.MathRepository
import com.example.ui.NotebookViewModel
import com.example.ui.NotebookViewModelFactory
import com.example.ui.screens.NotebookEditorScreen
import com.example.ui.screens.NotebookListScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core Room database setup
        val database = MathDatabase.getDatabase(applicationContext)
        val repository = MathRepository(database.mathDao())
        val viewModelFactory = NotebookViewModelFactory(repository)

        setContent {
            // Apply theme configuration
            MyApplicationTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("app_root_surface")
                ) {
                    MathNotebookApp(viewModelFactory)
                }
            }
        }
    }
}

@Composable
fun MathNotebookApp(
    factory: NotebookViewModelFactory,
    viewModel: NotebookViewModel = viewModel(factory = factory)
) {
    val currentNotebook = viewModel.currentNotebook

    if (currentNotebook == null) {
        NotebookListScreen(
            viewModel = viewModel,
            onOpenNotebook = { notebook ->
                viewModel.selectNotebook(notebook)
            }
        )
    } else {
        NotebookEditorScreen(
            viewModel = viewModel,
            onBackToList = {
                viewModel.selectNotebook(null)
            }
        )
    }
}
