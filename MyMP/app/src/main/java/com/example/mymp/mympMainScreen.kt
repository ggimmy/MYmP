package com.example.mymp

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun mympMainScreen(
    viewModel: mympViewModel,
    onSettingsClick: () -> Unit
) {
    val uiState = viewModel.uiState
    val state by viewModel.SyncState.collectAsState()
    val context = LocalContext.current

    var serverDropdownExpanded by remember { mutableStateOf(false) }
    var playlistDropdownExpanded by remember { mutableStateOf(false) }
    var songForPlaylist by remember { mutableStateOf<Song?>(null) }

    val displayedSongs = if (uiState.activePlaylist != null)
        uiState.currentPlaylistSongs
    else
        uiState.songs

    Column(
        modifier = Modifier
            .padding(2.dp)
            .safeContentPadding()
            .fillMaxSize()
    ) {

        // --- Barra superiore ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Dropdown Server
            Box {
                Button(onClick = { serverDropdownExpanded = true }) {
                    Text(uiState.activeServer?.serverName ?: "Server")
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
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
                            }
                        )
                    }
                }
            }

            // Dropdown Playlist
            Box {
                Button(onClick = { playlistDropdownExpanded = true }) {
                    Text(uiState.activePlaylist?.playlistName ?: "Playlist")
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
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
                        }
                    )
                    HorizontalDivider()
                    uiState.playlists.forEach { playlist ->
                        DropdownMenuItem(
                            text = { Text(playlist.playlistName) },
                            onClick = {
                                viewModel.selectPlaylist(playlist)
                                playlistDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Settings + stato connessione
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Impostazioni")
            }

            Icon(
                imageVector = if (uiState.isConnected) Icons.Default.Check else Icons.Default.Close,
                contentDescription = "Connection Status",
                modifier = Modifier.padding(2.dp)
            )
        }

        Text("Sync: $state Songs: ${displayedSongs.size}")

        // --- Lista brani ---
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(displayedSongs.size) { index ->
                val song = displayedSongs[index]
                ListItem(
                    headlineContent = { Text(song.title) },
                    supportingContent = { Text(song.artist) },
                    modifier = Modifier
                        .padding(2.dp)
                        .combinedClickable(
                            onClick = { viewModel.playSong(context, song) },
                            onLongClick = { songForPlaylist = song }
                        )
                )
                HorizontalDivider()
            }
        }

        // --- Mini player ---
        MiniPlayerBar(
            currentSong = uiState.currentSong,
            isPlaying = uiState.isPlaying,
            onPauseResume = { viewModel.pauseResume(context) },
            onSkip = { viewModel.skipSong(context) },
            onStop = { viewModel.stopSong(context) }
        )
    }

    // --- Dialog aggiungi a playlist ---
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
}

@Composable
private fun MiniPlayerBar(
    currentSong: Song?,
    isPlaying: Boolean,
    onPauseResume: () -> Unit,
    onSkip: () -> Unit,
    onStop: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 3.dp
    ) {
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
                    color = Color.Gray
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
    }
}