package com.example.mymp

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun mympMainScreen(
    viewModel: MympViewModel,
    onSettingsClick: () -> Unit
) {
    val uiState = viewModel.uiState
    val state by viewModel.syncState.collectAsState()
    val context = LocalContext.current

    var serverDropdownExpanded by remember { mutableStateOf(false) }
    var playlistDropdownExpanded by remember { mutableStateOf(false) }
    var sortDropdownExpanded by remember { mutableStateOf(false) }
    var songForPlaylist by remember { mutableStateOf<Song?>(null) }
    var songToRemoveFromPlaylist by remember {mutableStateOf<Song?>(null)}

    val displayedSongs = viewModel.displayedSongs

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding()
            .padding(horizontal = 8.dp)
    ) {

        // --- Riga 1: Ricerca + Sort ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = {
                    Text(
                        "Cerca canzone ...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancella")
                        }
                    }
                }
            )

            Box {
                IconButton(onClick = { sortDropdownExpanded = true }) {
                    Icon(
                        Icons.Default.Sort,
                        contentDescription = "Ordinamento",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                DropdownMenu(
                    expanded = sortDropdownExpanded,
                    onDismissRequest = { sortDropdownExpanded = false }
                ) {
                    SortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.label) },
                            onClick = {
                                viewModel.onSortOrderChange(order)
                                sortDropdownExpanded = false
                                Toast.makeText(
                                    context,
                                    "Ordinamento: ${order.label}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            trailingIcon = {
                                if (uiState.sortOrder == order) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        // --- Riga 2: Server + Playlist + Settings + Connessione ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dropdown Server
            Box {
                TextButton(onClick = { serverDropdownExpanded = true }) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        uiState.activeServer?.serverName ?: "Server",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = serverDropdownExpanded,
                    onDismissRequest = { serverDropdownExpanded = false }
                ) {
                    uiState.servers.forEach { server ->
                        DropdownMenuItem(
                            text = { Text(server.serverName) },
                            onClick = {
                                viewModel.selectServer(server)
                                serverDropdownExpanded = false
                            },
                            trailingIcon = {
                                if (uiState.activeServer?.id == server.id) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // Dropdown Playlist
            Box {
                TextButton(onClick = { playlistDropdownExpanded = true }) {
                    Text(
                        uiState.activePlaylist?.playlistName ?: "All Songs",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = playlistDropdownExpanded,
                    onDismissRequest = { playlistDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Tutti i brani") },
                        onClick = {
                            viewModel.selectPlaylist(null)
                            playlistDropdownExpanded = false
                        },
                        trailingIcon = {
                            if (uiState.activePlaylist == null) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                    HorizontalDivider()
                    uiState.playlists.forEach { playlist ->
                        DropdownMenuItem(
                            text = { Text(playlist.playlistName) },
                            onClick = {
                                viewModel.selectPlaylist(playlist)
                                playlistDropdownExpanded = false
                            },
                            trailingIcon = {
                                if (uiState.activePlaylist?.playlistId == playlist.playlistId) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // Spacer per spingere Settings a destra
            Box(modifier = Modifier.weight(1f))

            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Impostazioni",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        // --- Lista brani ---
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(displayedSongs.size) { index ->
                val song = displayedSongs[index]
                val isPlaying = uiState.currentSong == song

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isPlaying) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.background
                        )
                        .combinedClickable(
                            onClick = { viewModel.playSong(context, song) },
                            //onLongClick = { songForPlaylist = song }
                            onLongClick = { if (uiState.activePlaylist != null) {
                                            songToRemoveFromPlaylist = song
                                            } else {
                                                songForPlaylist = song
                                            }
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = if (isPlaying)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = song.artist,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        MiniPlayerBar(
            currentSong = uiState.currentSong,
            isPlaying = uiState.isPlaying,
            progress = uiState.playbackProgress,
            onPauseResume = { viewModel.pauseResume(context) },
            onSkip = { viewModel.skipSong(context) },
            onStop = { viewModel.stopSong(context) },
            onSeek = {progress -> viewModel.seekTo(context, progress)}
        )
    }

    songForPlaylist?.let { song ->
        AddToPlaylistDialog(
            song = song,
            playlists = uiState.playlists,
            onAddToPlaylist = { playlist ->
                viewModel.addSongToPlaylist(playlist.playlistId, song.serverId, song.remoteId)
                songForPlaylist = null
            },
            onCreatePlaylist = { name ->
                viewModel.addSongToNewPlaylist(name, song)
                songForPlaylist = null
            },
            onDismiss = { songForPlaylist = null }
        )
    }

    songToRemoveFromPlaylist?.let { song ->
        val playlistId = uiState.activePlaylist?.playlistId ?: return@let
        RemoveFromPlaylistDialog(
            song = song,
            onConfirm = {
                viewModel.removeSongFromPlaylist(playlistId, song.serverId, song.remoteId)
                songToRemoveFromPlaylist = null
            },
            onDismiss = { songToRemoveFromPlaylist = null }
        )
    }

}

@Composable
fun MiniPlayerBar(
    currentSong: Song?,
    isPlaying: Boolean,
    progress: Float,
    onPauseResume: () -> Unit,
    onSkip: () -> Unit,
    onStop: () -> Unit,
    onSeek: (Float) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 3.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = currentSong?.title ?: "Nessun brano in riproduzione",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentSong?.artist ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onPauseResume, enabled = currentSong != null) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausa" else "Play"
                    )
                }

                IconButton(onClick = onSkip, enabled = currentSong != null) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Successivo")
                }

                IconButton(onClick = onStop, enabled = currentSong != null) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop")
                }
            }

            Slider(
                value = progress,
                onValueChange = { onSeek(it) },
                enabled = currentSong != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
fun RemoveFromPlaylistDialog(
    song: Song,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rimuovi brano") },
        text = { Text("Vuoi rimuovere \"${song.title}\" dalla playlist?") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Rimuovi") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}