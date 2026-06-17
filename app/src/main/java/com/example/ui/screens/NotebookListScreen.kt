package com.example.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Notebook
import com.example.ui.NotebookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookListScreen(
    viewModel: NotebookViewModel,
    onOpenNotebook: (Notebook) -> Unit
) {
    val context = LocalContext.current
    val notebooks by viewModel.allNotebooks.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newNotebookName by remember { mutableStateOf("") }

    var notebookToDelete by remember { mutableStateOf<Notebook?>(null) }
    var notebookToRename by remember { mutableStateOf<Notebook?>(null) }
    var renameInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "📓",
                            fontSize = 26.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Math Notebook",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    newNotebookName = ""
                    showCreateDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_create_notebook")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Notebook")
                    Spacer(Modifier.width(6.dp))
                    Text("New Notebook", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (notebooks.isEmpty()) {
                // Friendly Empty State Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(96.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = "🎓",
                                    fontSize = 44.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(
                            text = "No Notebooks Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Create a custom notebook. Write step-by-step mathematical answers manually with high key targets, specially structured for your studying convenience.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.widthIn(max = 300.dp)
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = {
                                newNotebookName = ""
                                showCreateDialog = true
                            },
                            modifier = Modifier.testTag("btn_empty_create")
                        ) {
                            Text("Create First Notebook", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Interactive Grid of Notebooks
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("notebooks_grid")
                ) {
                    items(notebooks) { notebook ->
                        val dateFormatted = DateFormat.format("MMM dd, yyyy h:mm a", notebook.lastModified).toString()
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.72f) // Physical notebook aspect ratio
                                .clickable { onOpenNotebook(notebook) }
                                .testTag("notebook_card_${notebook.id}"),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.surface,
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            )
                                        )
                                    )
                                    .padding(14.dp)
                            ) {
                                // Real Notebook Binding Stripe UI
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // Binding Icon Representation
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "📘",
                                            fontSize = 24.sp
                                        )
                                    }

                                    // Action Dropdown menu
                                    var displayMenu by remember { mutableStateOf(false) }
                                    Box {
                                        IconButton(
                                            onClick = { displayMenu = true },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Options"
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = displayMenu,
                                            onDismissRequest = { displayMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Rename") },
                                                onClick = {
                                                    displayMenu = false
                                                    renameInput = notebook.name
                                                    notebookToRename = notebook
                                                },
                                                leadingIcon = { Icon(Icons.Default.Edit, "Rename") }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                                onClick = {
                                                    displayMenu = false
                                                    notebookToDelete = notebook
                                                },
                                                leadingIcon = { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.weight(1f))

                                // Notebook visual metadata titles
                                Text(
                                    text = notebook.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    text = "Edited: $dateFormatted",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Create Notebook Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Notebook") },
            text = {
                OutlinedTextField(
                    value = newNotebookName,
                    onValueChange = { newNotebookName = it },
                    label = { Text("Notebook Name") },
                    placeholder = { Text("e.g., Algebra Homeroom") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_notebook_input")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val nameToUse = if (newNotebookName.trim().isEmpty()) "Untitled Math Homework" else newNotebookName.trim()
                        viewModel.createNotebook(nameToUse)
                        showCreateDialog = false
                    },
                    modifier = Modifier.testTag("btn_create_confirm")
                ) {
                    Text("Create", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename confirmation dialog
    val toRename = notebookToRename
    if (toRename != null) {
        AlertDialog(
            onDismissRequest = { notebookToRename = null },
            title = { Text("Rename Notebook") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Notebook Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rename_input_popup")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            viewModel.renameNotebook(toRename.id, renameInput.trim())
                            notebookToRename = null
                        }
                    },
                    modifier = Modifier.testTag("btn_rename_popup_confirm")
                ) {
                    Text("Rename", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { notebookToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete confirmation dialog
    val toDelete = notebookToDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { notebookToDelete = null },
            title = { Text("Delete Notebook") },
            text = {
                Text("Are you sure you want to delete '${toDelete.name}'? This action will permanently erase all handwritten mathematical sheets of this notebook and can never be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNotebook(toDelete)
                        notebookToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("btn_delete_confirm")
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { notebookToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
