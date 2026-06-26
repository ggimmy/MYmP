package com.example.mymp

data class mympUiState(
    val songs: List<Song> = emptyList(),
    val servers: List<ServerEntity> = emptyList(),
    val activeServer: ServerEntity? = null,
    var ipAddress: String = "",
    val isConnected: Boolean = false,
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val connectionStatus: String? = null,
    val playlists: List<PlaylistEntity> = emptyList(),
    val currentPlaylistSongs: List<Song> = emptyList(),
    val activePlaylist: PlaylistEntity? = null,  // null = vista server, non-null = vista playlist
    var searchQuery: String = "",
    var sortOrder: SortOrder = SortOrder.TITLE_ASC, //title ascending di default
    val playbackProgress: Float = 0f
)