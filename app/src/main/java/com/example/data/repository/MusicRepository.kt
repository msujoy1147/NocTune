package com.example.data.repository

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import com.example.data.db.SongDao
import com.example.data.model.PlaylistEntity
import com.example.data.model.PlaylistSongCrossRef
import com.example.data.model.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class MusicRepository(
    private val context: Context,
    private val songDao: SongDao
) {
    val allSongs: Flow<List<SongEntity>> = songDao.getAllSongs()
    val favoriteSongs: Flow<List<SongEntity>> = songDao.getFavoriteSongs()
    val lastAddedSongs: Flow<List<SongEntity>> = songDao.getLastAddedSongs()
    val mostPlayedSongs: Flow<List<SongEntity>> = songDao.getMostPlayedSongs()
    val recentlyPlayedSongs: Flow<List<SongEntity>> = songDao.getRecentlyPlayedSongs()
    val allPlaylists: Flow<List<PlaylistEntity>> = songDao.getAllPlaylists()

    suspend fun initDefaultGenerativeTracks() = withContext(Dispatchers.IO) {
        // Automatically delete all songs to keep the application 100% lightweight, beautiful, and light-speed as requested.
        try {
            songDao.deleteAllSongs()
        } catch (e: Exception) {
            Log.e("MusicRepository", "Failed to clear all songs", e)
        }
    }

    suspend fun insertSong(song: SongEntity) = withContext(Dispatchers.IO) {
        songDao.insertSong(song)
    }

    suspend fun toggleFavorite(songId: String, isFav: Boolean) = withContext(Dispatchers.IO) {
        songDao.updateFavorite(songId, isFav)
    }

    suspend fun incrementPlayCount(songId: String) = withContext(Dispatchers.IO) {
        songDao.incrementPlayCount(songId)
    }

    // Scan physical media from device storage
    suspend fun scanLocalMusic() = withContext(Dispatchers.IO) {
        try {
            val resolver: ContentResolver = context.contentResolver
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DATE_ADDED
            )
            
            // Only fetch music files
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val cursor: Cursor? = resolver.query(uri, projection, selection, null, null)

            val scannedSongs = mutableListOf<SongEntity>()
            val prefs = context.getSharedPreferences("noctune_deleted_songs_prefs", Context.MODE_PRIVATE)
            val deletedIds = prefs.getStringSet("deleted_ids", emptySet()) ?: emptySet()
            val deletedPaths = prefs.getStringSet("deleted_paths", emptySet()) ?: emptySet()

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val dateAddedCol = c.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)

                while (c.moveToNext()) {
                    val idLong = c.getLong(idCol)
                    val id = "local_$idLong"
                    val title = c.getString(titleCol) ?: "Unknown Track"
                    val artist = c.getString(artistCol) ?: "<Unknown Artist>"
                    val album = c.getString(albumCol) ?: "Unknown Album"
                    val duration = c.getLong(durationCol)
                    val path = c.getString(dataCol) ?: ""
                    val addedDate = if (dateAddedCol != -1) c.getLong(dateAddedCol) * 1000 else System.currentTimeMillis()

                    // Exclude songs that were previously permanently deleted by the user
                    if (id in deletedIds || (path.isNotBlank() && (path in deletedPaths || deletedPaths.any { it.equals(path, ignoreCase = true) }))) {
                        continue
                    }

                    scannedSongs.add(
                        SongEntity(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            duration = duration,
                            path = path,
                            addedDate = addedDate
                        )
                    )
                }
            }

            if (scannedSongs.isNotEmpty()) {
                songDao.insertSongs(scannedSongs)
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error scanning local music from content resolver", e)
        }
    }

    // Playlist Operations
    suspend fun createPlaylist(name: String): Int = withContext(Dispatchers.IO) {
        val playlist = PlaylistEntity(name = name)
        songDao.insertPlaylist(playlist).toInt()
    }

    suspend fun deletePlaylist(playlist: PlaylistEntity) = withContext(Dispatchers.IO) {
        songDao.deletePlaylist(playlist)
    }

    suspend fun addSongToPlaylist(playlistId: Int, songId: String) = withContext(Dispatchers.IO) {
        songDao.insertPlaylistSong(PlaylistSongCrossRef(playlistId, songId))
        songDao.updatePlaylistSongCount(playlistId)
    }

    suspend fun removeSongFromPlaylist(playlistId: Int, songId: String) = withContext(Dispatchers.IO) {
        songDao.deletePlaylistSong(playlistId, songId)
        songDao.updatePlaylistSongCount(playlistId)
    }

    fun getSongsForPlaylist(playlistId: Int): Flow<List<SongEntity>> {
        return songDao.getSongsForPlaylist(playlistId)
    }

    suspend fun deleteSong(song: SongEntity) = withContext(Dispatchers.IO) {
        // Save to deleted songs Preferences so it will never show up or be re-scanned
        val prefs = context.getSharedPreferences("noctune_deleted_songs_prefs", Context.MODE_PRIVATE)
        val deletedIds = prefs.getStringSet("deleted_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        val deletedPaths = prefs.getStringSet("deleted_paths", emptySet())?.toMutableSet() ?: mutableSetOf()
        
        deletedIds.add(song.id)
        if (song.path.isNotBlank()) {
            deletedPaths.add(song.path)
        }
        
        prefs.edit()
            .putStringSet("deleted_ids", deletedIds)
            .putStringSet("deleted_paths", deletedPaths)
            .apply()

        songDao.deletePlaylistSongCrossRefs(song.id)
        songDao.deleteSong(song.id)
        
        if (!song.isGenerative && song.path.isNotBlank()) {
            try {
                // Try deleting using java.io.File First
                val file = java.io.File(song.path)
                if (file.exists()) {
                    val deleted = file.delete()
                    Log.d("MusicRepository", "Successfully physically deleted song path: ${song.path}: $deleted")
                } else {
                    val uri = android.net.Uri.parse(song.path)
                    if (uri.scheme == "content") {
                        context.contentResolver.delete(uri, null, null)
                        Log.d("MusicRepository", "ContentResolver deleted song: ${song.path}")
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicRepository", "Failed to delete physical file of ${song.path}", e)
            }
            
            // Also explicitly try to resolve/delete via MediaStore content URI if it's a MediaStore track
            if (song.id.startsWith("local_")) {
                val mediaStoreId = song.id.substringAfter("local_").toLongOrNull()
                if (mediaStoreId != null) {
                    try {
                        val contentUri = android.content.ContentUris.withAppendedId(
                            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            mediaStoreId
                        )
                        val rowsDeleted = context.contentResolver.delete(contentUri, null, null)
                        Log.d("MusicRepository", "ContentResolver deleted song uri: $contentUri, rows: $rowsDeleted")
                    } catch (e: Exception) {
                        Log.e("MusicRepository", "Failed to delete from media store row for client-side delete request $mediaStoreId", e)
                    }
                }
            }
        }
    }
}
