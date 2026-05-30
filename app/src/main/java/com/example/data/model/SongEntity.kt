package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String, // Media Store ID or custom Generative Preset ID
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long, // in milliseconds
    val path: String,   // File path or content URI
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val addedDate: Long = System.currentTimeMillis(),
    val lastPlayedDate: Long = 0,
    val isGenerative: Boolean = false,
    val generativePreset: String = ""
) : Serializable {
    companion object {
        fun createGenerative(
            id: String,
            name: String,
            description: String,
            duration: Long = 180000L // 3 minutes default display
        ) = SongEntity(
            id = id,
            title = name,
            artist = "NocTune Generative Synth",
            album = "NocTune Signature Lounge",
            duration = duration,
            path = "generative://$id",
            isFavorite = false,
            playCount = 0,
            isGenerative = true,
            generativePreset = id
        )
    }
}
