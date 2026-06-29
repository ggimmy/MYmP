package com.example.mymp


/**
 *
 * Classe enum, utilizzata da [MympViewModel.displayedSongs] per determinare l'ordinamento.
 *
 */

enum class SortOrder(val label: String) {
    TITLE_ASC("Titolo A→Z"),
    TITLE_DES("Titolo Z→A"),
    ARTIST_ASC("Artista A→Z"),
    ARTIST_DES("Artista Z→A")
}