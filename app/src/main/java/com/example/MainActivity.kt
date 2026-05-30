package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PlaylistEntity
import com.example.data.model.SongEntity
import com.example.player.MusicPlayerManager
import com.example.player.RepeatMode
import com.example.ui.components.*
import com.example.ui.theme.NocTuneTheme
import com.example.ui.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            NocTuneTheme {
                MainAppScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: PlayerViewModel) {
    val context = LocalContext.current
    
    // Core states
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentProgress by viewModel.currentPosition.collectAsStateWithLifecycle()
    val playQueue by viewModel.playbackQueue.collectAsStateWithLifecycle()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val sleepTimerMs by viewModel.sleepTimerRemaining.collectAsStateWithLifecycle()
    val stopAfterCurrent by viewModel.stopAfterCurrentSong.collectAsStateWithLifecycle()

    // Database query streams
    val allSongs by viewModel.allSongs.collectAsStateWithLifecycle()
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayedSongs.collectAsStateWithLifecycle()
    val mostPlayed by viewModel.mostPlayedSongs.collectAsStateWithLifecycle()
    val lastAdded by viewModel.lastAddedSongs.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    val filteredSongs by viewModel.filteredSongs.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val selectedPlaylist by viewModel.selectedPlaylist.collectAsStateWithLifecycle()
    val songsInSelectedPlaylist by viewModel.songsInSelectedPlaylist.collectAsStateWithLifecycle()

    // Navigation and Popup management indices
    var currentTab by remember { mutableStateOf("home") } // "home", "library", "search"
    var expandedPlayer by remember { mutableStateOf(false) }
    var activeLibrarySection by remember { mutableStateOf("songs") } // "songs", "albums", "artists", "playlists"
    
    // Dialog overlays toggles
    var showSleepTimerMenu by remember { mutableStateOf(false) }
    var showCreatePlaylistInput by remember { mutableStateOf(false) }
    var showAddToPlaylistSelector by remember { mutableStateOf<SongEntity?>(null) }
    var showQueueDrawer by remember { mutableStateOf(false) }

    // Coffee lounge visuals Constants
    val deepEspresso = Color(0xFF1E1814)
    val darkMocha = Color(0xFF2A211C)
    val coffeeBrown = Color(0xFFB08968)
    val softLatte = Color(0xFFDDB892)
    val warmCream = Color(0xFFF8F4F0)
    val secondaryText = Color(0xFFCBB9A8)

    // Check & request runtime permissions for music files
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[audioPermission] ?: false
        if (audioGranted) {
            viewModel.scanMusicFiles()
            Toast.makeText(context, "Scanning local library...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Storage permission is required to read scanned songs.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(context, audioPermission) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(audioPermission)
        } else {
            viewModel.scanMusicFiles()
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            launcher.launch(permissionsToRequest.toTypedArray())
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = deepEspresso,
        bottomBar = {
            // Standard Custom Tab BottomNavigation
            Column(modifier = Modifier.background(deepEspresso)) {
                // Inline floating mini player sits above bottom navigation bar
                val activeSong = currentSong
                if (activeSong != null) {
                    MiniFloatingPlayer(
                        song = activeSong,
                        isPlaying = isPlaying,
                        onPlayPauseToggle = { viewModel.resumeOrPause() },
                        onClick = { expandedPlayer = true }
                    )
                }

                NavigationBar(
                    containerColor = darkMocha.copy(alpha = 0.9f),
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    modifier = Modifier
                        .padding(start = 24.dp, end = 24.dp, bottom = 16.dp, top = 4.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, Color(0x24B08968), RoundedCornerShape(24.dp))
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    NavigationBarItem(
                        selected = currentTab == "home",
                        onClick = { currentTab = "home" },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home tab icon") },
                        label = { Text("Home", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = deepEspresso,
                            selectedTextColor = softLatte,
                            indicatorColor = softLatte,
                            unselectedIconColor = secondaryText,
                            unselectedTextColor = secondaryText
                        ),
                        modifier = Modifier.testTag("nav_home")
                    )
                    NavigationBarItem(
                        selected = currentTab == "library",
                        onClick = { currentTab = "library" },
                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library tab icon") },
                        label = { Text("Library", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = deepEspresso,
                            selectedTextColor = softLatte,
                            indicatorColor = softLatte,
                            unselectedIconColor = secondaryText,
                            unselectedTextColor = secondaryText
                        ),
                        modifier = Modifier.testTag("nav_library")
                    )
                    NavigationBarItem(
                        selected = currentTab == "search",
                        onClick = { currentTab = "search" },
                        icon = { Icon(Icons.Default.Search, contentDescription = "Search tab icon") },
                        label = { Text("Search", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = deepEspresso,
                            selectedTextColor = softLatte,
                            indicatorColor = softLatte,
                            unselectedIconColor = secondaryText,
                            unselectedTextColor = secondaryText
                        ),
                        modifier = Modifier.testTag("nav_search")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab router
            when (currentTab) {
                "home" -> HomeScreen(
                    recentlyPlayed = recentlyPlayed,
                    favoriteSongs = favoriteSongs,
                    mostPlayed = mostPlayed,
                    lastAdded = lastAdded,
                    allSongsList = allSongs,
                    onPlaySong = { s -> viewModel.playSong(s, allSongs) },
                    onAddToPlaylistRequest = { s -> showAddToPlaylistSelector = s },
                    modifier = Modifier.fillMaxSize()
                )
                
                "library" -> LibraryScreen(
                    songs = allSongs,
                    playlists = playlists,
                    selectedPlaylist = selectedPlaylist,
                    songsInSelectedPlaylist = songsInSelectedPlaylist,
                    activeSection = activeLibrarySection,
                    onSectionChange = { activeLibrarySection = it },
                    onPlaySong = { s, list -> viewModel.playSong(s, list) },
                    onSelectPlaylist = { viewModel.selectPlaylist(it) },
                    onCreatePlaylistRequest = { showCreatePlaylistInput = true },
                    onDeletePlaylist = { viewModel.deletePlaylist(it) },
                    onRemoveSongFromPlaylist = { pid, sid -> viewModel.removeSongFromPlaylist(pid, sid) },
                    onAddToPlaylistRequest = { s -> showAddToPlaylistSelector = s },
                    modifier = Modifier.fillMaxSize()
                )
                
                "search" -> SearchScreen(
                    songs = filteredSongs,
                    query = searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onPlaySong = { s -> viewModel.playSong(s, filteredSongs) },
                    onAddToPlaylistRequest = { s -> showAddToPlaylistSelector = s },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Full Expanded Immersive Player Screen (slides up)
            AnimatedVisibility(
                visible = expandedPlayer,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                )
            ) {
                val activeSong = currentSong
                if (activeSong != null) {
                    FullPlayerScreen(
                        song = activeSong,
                        isPlaying = isPlaying,
                        progress = currentProgress,
                        shuffleEnabled = shuffleEnabled,
                        repeatMode = repeatMode,
                        sleepTimerRemainingMs = sleepTimerMs,
                        stopAfterCurrentEnabled = stopAfterCurrent,
                        onMinimize = { expandedPlayer = false },
                        onPlayPause = { viewModel.resumeOrPause() },
                        onNext = { viewModel.next() },
                        onPrevious = { viewModel.previous() },
                        onSeek = { viewModel.seekTo(it) },
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        onToggleRepeat = { viewModel.toggleRepeat() },
                        onToggleFavorite = { viewModel.toggleFavorite() },
                        onTriggerSleepTimerMenu = { showSleepTimerMenu = true },
                        onTriggerQueueDrawer = { showQueueDrawer = true }
                    )
                }
            }

            // Sliding active playlist queue Drawer
            if (showQueueDrawer) {
                ActiveQueueDrawer(
                    queue = playQueue,
                    activeSongIndex = playQueue.indexOf(currentSong),
                    onDismiss = { showQueueDrawer = false },
                    onPlaySong = { s -> viewModel.playSong(s, playQueue) }
                )
            }
        }
    }

    // Dialog: Setup Sleep clock countdown
    if (showSleepTimerMenu) {
        SleepTimerDialog(
            currentRemainingMs = sleepTimerMs,
            stopAfterCurrentEnabled = stopAfterCurrent,
            onDismiss = { showSleepTimerMenu = false },
            onSetTimer = { mins -> viewModel.setSleepTimer(mins) },
            onToggleStopAfterCurrent = { viewModel.toggleStopAfterCurrent() }
        )
    }

    // Dialog: Create dynamic Playlist
    if (showCreatePlaylistInput) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistInput = false },
            onCreate = { name -> viewModel.createPlaylist(name) }
        )
    }

    // Dialog: Add song to playlist
    val targetSong = showAddToPlaylistSelector
    if (targetSong != null) {
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { showAddToPlaylistSelector = null },
            onSelectPlaylist = { playlist ->
                viewModel.addSongToPlaylist(playlist.id, targetSong.id)
                Toast.makeText(context, "Added to playlist ${playlist.name}", Toast.LENGTH_SHORT).show()
                showAddToPlaylistSelector = null
            },
            onCreateNewPlaylist = { showCreatePlaylistInput = true }
        )
    }
}

// ==========================================
// SCREEN: HOME TAB
// ==========================================
@Composable
fun HomeScreen(
    recentlyPlayed: List<SongEntity>,
    favoriteSongs: List<SongEntity>,
    mostPlayed: List<SongEntity>,
    lastAdded: List<SongEntity>,
    allSongsList: List<SongEntity>,
    onPlaySong: (SongEntity) -> Unit,
    onAddToPlaylistRequest: (SongEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val darkMocha = Color(0xFF2A211C)
    val coffeeBrown = Color(0xFFB08968)
    val warmCream = Color(0xFFF8F4F0)
    val secondaryText = Color(0xFFCBB9A8)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp)
    ) {
        // Welcome and App branding Title
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = "WELCOME BACK",
                    color = secondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.testTag("app_title")
                )
                Text(
                    text = "Your Sound Your Space.",
                    color = warmCream,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Section: Recently Played (Horizontal Flow Cards)
        if (recentlyPlayed.isNotEmpty()) {
            item {
                HomeSectionHeader(title = "Recently Played")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(recentlyPlayed) { song ->
                        PlayableLoungeCard(
                            song = song,
                            onClick = { onPlaySong(song) },
                            onLongClick = { onAddToPlaylistRequest(song) }
                        )
                    }
                }
            }
        }

        // Section: Last Added (Horizontal Flow Cards)
        if (lastAdded.isNotEmpty()) {
            item {
                HomeSectionHeader(title = "Last Added Songs")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(lastAdded) { song ->
                        PlayableLoungeCard(
                            song = song,
                            onClick = { onPlaySong(song) },
                            onLongClick = { onAddToPlaylistRequest(song) }
                        )
                    }
                }
            }
        }

        // Section: Favorite Tracks (Vertical List Items)
        item {
            HomeSectionHeader(title = "Your Favorite Blends")
            if (favoriteSongs.isEmpty()) {
                EmptyStateCard(message = "Tap the heart on any song to create your custom espresso favorites.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    favoriteSongs.take(6).forEach { song ->
                        SongListItem(
                            song = song,
                            onClick = { onPlaySong(song) },
                            onLongClick = { onAddToPlaylistRequest(song) }
                        )
                    }
                }
            }
        }

        // Section: Most Played Tracks
        if (mostPlayed.isNotEmpty()) {
            item {
                HomeSectionHeader(title = "Most Played")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    mostPlayed.take(6).forEach { song ->
                        SongListItem(
                            song = song,
                            onClick = { onPlaySong(song) },
                            onLongClick = { onAddToPlaylistRequest(song) }
                        )
                    }
                }
            }
        }

        // Global default catalog of preset melodies if no scanned tracks
        if (allSongsList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(darkMocha)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = coffeeBrown)
                }
            }
        }
    }
}

@Composable
fun HomeSectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFFF8F4F0), // Warm Cream
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

// ==========================================
// SCREEN: LIBRARY TAB
// ==========================================
@Composable
fun LibraryScreen(
    songs: List<SongEntity>,
    playlists: List<PlaylistEntity>,
    selectedPlaylist: PlaylistEntity?,
    songsInSelectedPlaylist: List<SongEntity>,
    activeSection: String,
    onSectionChange: (String) -> Unit,
    onPlaySong: (SongEntity, List<SongEntity>) -> Unit,
    onSelectPlaylist: (PlaylistEntity?) -> Unit,
    onCreatePlaylistRequest: () -> Unit,
    onDeletePlaylist: (PlaylistEntity) -> Unit,
    onRemoveSongFromPlaylist: (Int, String) -> Unit,
    onAddToPlaylistRequest: (SongEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val darkMocha = Color(0xFF2A211C)
    val deepEspresso = Color(0xFF1E1814)
    val coffeeBrown = Color(0xFFB08968)
    val softLatte = Color(0xFFDDB892)
    val warmCream = Color(0xFFF8F4F0)
    val secondaryText = Color(0xFFCBB9A8)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Back-action indicator if inspecting playlist details
        if (selectedPlaylist != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onSelectPlaylist(null) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back into catalog view button", tint = warmCream)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = selectedPlaylist.name, color = warmCream, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(text = "${songsInSelectedPlaylist.size} songs compiled", color = secondaryText, fontSize = 12.sp)
                }
                IconButton(
                    onClick = { onDeletePlaylist(selectedPlaylist) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete custom compiled playlist", tint = Color.Red.copy(alpha = 0.8f))
                }
            }

            // Render selected playlist songs
            if (songsInSelectedPlaylist.isEmpty()) {
                EmptyStateCard(message = "This playlist is empty. Search tracks or browse standard lists and long-click to add!")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(songsInSelectedPlaylist) { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(darkMocha.copy(alpha = 0.6f))
                                .clickable { onPlaySong(song, songsInSelectedPlaylist) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                CoffeeAlbumArt(
                                    presetId = song.generativePreset,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(text = song.title, color = warmCream, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(text = song.artist, color = secondaryText, fontSize = 11.sp)
                                }
                            }
                            IconButton(
                                onClick = { onRemoveSongFromPlaylist(selectedPlaylist.id, song.id) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove music track item", tint = Color.Red.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        } else {
            // Header
            Text(
                text = "Studio Library",
                color = warmCream,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
            )

            // Section Filter Selectors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val sections = listOf(
                    "songs" to "Songs",
                    "albums" to "Albums",
                    "artists" to "Artists",
                    "playlists" to "Playlists"
                )
                sections.forEach { (secId, secLabel) ->
                    val isSelected = activeSection == secId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) coffeeBrown else darkMocha)
                            .clickable { onSectionChange(secId) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = secLabel,
                            color = if (isSelected) deepEspresso else secondaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Selected Sub-Section rendering
            when (activeSection) {
                "songs" -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(songs) { song ->
                            SongListItem(
                                song = song,
                                onClick = { onPlaySong(song, songs) },
                                onLongClick = { onAddToPlaylistRequest(song) }
                            )
                        }
                    }
                }
                
                "albums" -> {
                    // Group songs dynamically by album
                    val albumsGroups = songs.groupBy { it.album }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(albumsGroups.keys.toList()) { albumTitle ->
                            val albumSongs = albumsGroups[albumTitle] ?: emptyList()
                            val repSong = albumSongs.firstOrNull()
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(darkMocha)
                                    .clickable {
                                        if (albumSongs.isNotEmpty()) {
                                            onPlaySong(albumSongs.first(), albumSongs)
                                        }
                                    }
                                    .padding(12.dp)
                            ) {
                                CoffeeAlbumArt(
                                    presetId = repSong?.generativePreset ?: "",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = albumTitle,
                                    color = warmCream,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${albumSongs.size} tracks",
                                    color = secondaryText,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
                
                "artists" -> {
                    // Group songs dynamically by artist
                    val artistGroups = songs.groupBy { it.artist }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(artistGroups.keys.toList()) { artistName ->
                            val artSongs = artistGroups[artistName] ?: emptyList()
                            val repSong = artSongs.firstOrNull()
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(darkMocha)
                                    .clickable {
                                        if (artSongs.isNotEmpty()) {
                                            onPlaySong(artSongs.first(), artSongs)
                                        }
                                    }
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Rounded profile visual styling for artists
                                CoffeeAlbumArt(
                                    presetId = repSong?.generativePreset ?: "",
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = artistName,
                                    color = warmCream,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${artSongs.size} tracks listed",
                                    color = secondaryText,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
                
                "playlists" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Playlists Add Header row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(darkMocha)
                                .clickable { onCreatePlaylistRequest() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Create blank compiler playlist", tint = coffeeBrown)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "Initialize New Playlist", color = softLatte, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (playlists.isEmpty()) {
                            EmptyStateCard(message = "No custom compilation lists present yet. Initialize one above!")
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 32.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(playlists) { playlist ->
                                    Column(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(darkMocha)
                                            .clickable { onSelectPlaylist(playlist) }
                                            .padding(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LibraryMusic,
                                            contentDescription = "Playlist folder visual emblem",
                                            tint = softLatte,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = playlist.name,
                                            color = warmCream,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${playlist.songCount} songs stored",
                                            color = secondaryText,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN: SEARCH TAB
// ==========================================
@Composable
fun SearchScreen(
    songs: List<SongEntity>,
    query: String,
    onQueryChange: (String) -> Unit,
    onPlaySong: (SongEntity) -> Unit,
    onAddToPlaylistRequest: (SongEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val darkMocha = Color(0xFF2A211C)
    val coffeeBrown = Color(0xFFB08968)
    val warmCream = Color(0xFFF8F4F0)
    val secondaryText = Color(0xFFCBB9A8)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // App search title header
        Text(
            text = "Instant Coffee Search",
            color = warmCream,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
        )

        // Custom search text field
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search songs, albums, and artists in NocTune...", color = secondaryText) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Active search trigger magnifier", tint = secondaryText) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = coffeeBrown,
                unfocusedBorderColor = darkMocha,
                cursorColor = coffeeBrown,
                focusedTextColor = warmCream,
                unfocusedTextColor = warmCream
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("search_field"),
            shape = RoundedCornerShape(20.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Search Results List
        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tracks fit your query inside our lounge.",
                    color = secondaryText,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(songs) { song ->
                    SongListItem(
                        song = song,
                        onClick = { onPlaySong(song) },
                        onLongClick = { onAddToPlaylistRequest(song) }
                    )
                }
            }
        }
    }
}

// ==========================================
// SUB-COMPONENT: REUSABLE ITEMS & PILLS
// ==========================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayableLoungeCard(
    song: SongEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val darkMocha = Color(0xFF2A211C)
    val warmCream = Color(0xFFF8F4F0)
    val secondaryText = Color(0xFFCBB9A8)

    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(darkMocha, Color(0xFF211A16))
                )
            )
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(12.dp)
    ) {
        CoffeeAlbumArt(
            presetId = song.generativePreset,
            modifier = Modifier
                .size(116.dp)
                .fillMaxWidth()
            // Spinning only on full player screen to conserve hardware performance
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = song.title,
            color = warmCream,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            color = secondaryText,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongListItem(
    song: SongEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val darkMocha = Color(0xFF2A211C)
    val warmCream = Color(0xFFF8F4F0)
    val secondaryText = Color(0xFFCBB9A8)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(darkMocha.copy(alpha = 0.4f))
            .border(1.dp, Color(0x0FFFFFFF), RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoffeeAlbumArt(
            presetId = song.generativePreset,
            modifier = Modifier.size(50.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = warmCream,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = secondaryText,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = "Song favorite vector flag icon",
            tint = if (song.isFavorite) Color.Red else secondaryText.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun EmptyStateCard(message: String) {
    val darkMocha = Color(0xFF2A211C)
    val secondaryText = Color(0xFFCBB9A8)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(darkMocha.copy(alpha = 0.5f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = secondaryText,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

// ==========================================
// SUB-COMPONENT: FLOATING MINI PLAYER
// ==========================================
@Composable
fun MiniFloatingPlayer(
    song: SongEntity,
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit,
    onClick: () -> Unit
) {
    val darkMocha = Color(0xFF2A211C)
    val coffeeBrown = Color(0xFFB08968)
    val warmCream = Color(0xFFF8F4F0)
    val secondaryText = Color(0xFFCBB9A8)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(darkMocha.copy(alpha = 0.95f))
            .border(1.dp, Color(0x24B08968), RoundedCornerShape(24.dp)) // Matching subtle border
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("mini_player"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rotating album mini visual
        CoffeeAlbumArt(
            presetId = song.generativePreset,
            modifier = Modifier.size(42.dp),
            isPlaying = isPlaying
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = warmCream,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = secondaryText,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onPlayPauseToggle,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("mini_play_pause")
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Floating playback toggle button",
                tint = coffeeBrown,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ==========================================
// SCREEN: FULL PLAYER DETAIL OVERLAY
// ==========================================
@Composable
fun FullPlayerScreen(
    song: SongEntity,
    isPlaying: Boolean,
    progress: Long,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    sleepTimerRemainingMs: Long,
    stopAfterCurrentEnabled: Boolean,
    onMinimize: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTriggerSleepTimerMenu: () -> Unit,
    onTriggerQueueDrawer: () -> Unit
) {
    val deepEspresso = Color(0xFF1E1814)
    val darkMocha = Color(0xFF2A211C)
    val coffeeBrown = Color(0xFFB08968)
    val softLatte = Color(0xFFDDB892)
    val warmCream = Color(0xFFF8F4F0)
    val secondaryText = Color(0xFFCBB9A8)

    // Formatted timelines
    val currentFormatted = remember(progress) {
        val totalSeconds = progress / 1000
        String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }
    val durationFormatted = remember(song.duration) {
        val totalSeconds = song.duration / 1000
        String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(deepEspresso)
    ) {
        // Dynamic Backdrop Glowing Blur based on ambient song themes
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(80.dp)
                .graphicsLayer(alpha = 0.45f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(coffeeBrown.copy(alpha = 0.25f), Color.Transparent),
                            center = Offset.Unspecified
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Action controls row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMinimize, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize full player button", tint = warmCream, modifier = Modifier.size(32.dp))
                }
                
                Text(
                    text = "NocTune Active Lounge",
                    color = warmCream,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    // Sleep lock badge decoration
                    Box(modifier = Modifier.wrapContentSize(), contentAlignment = Alignment.Center) {
                        IconButton(onClick = onTriggerSleepTimerMenu, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(
                                imageVector = Icons.Default.Snooze,
                                contentDescription = "Configure sleep countdown trigger",
                                tint = if (sleepTimerRemainingMs > 0 || stopAfterCurrentEnabled) coffeeBrown else warmCream
                            )
                        }
                    }
                    IconButton(onClick = onTriggerQueueDrawer, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Default.QueueMusic, contentDescription = "Toggle playing queue drawer", tint = warmCream)
                    }
                }
            }

            // Central spinning Album Artwork
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(24.dp)
                    .graphicsLayer {
                        shadowElevation = 16f
                        shape = RoundedCornerShape(20)
                    }
            ) {
                CoffeeAlbumArt(
                    presetId = song.generativePreset,
                    modifier = Modifier.fillMaxSize(),
                    isPlaying = isPlaying
                )
            }

            // Sleep Timer countdown bubble active visualization
            if (sleepTimerRemainingMs > 0 || stopAfterCurrentEnabled) {
                val timerLabel = if (sleepTimerRemainingMs > 0) {
                    val rem = sleepTimerRemainingMs / 1000
                    String.format("%02d:%02d left", rem / 60, rem % 60)
                } else {
                    "Stop after track"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(coffeeBrown.copy(alpha = 0.25f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Snooze, contentDescription = "Snooze active indicator link", tint = softLatte, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = timerLabel, color = softLatte, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Metadata: Titles
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = song.title,
                    color = warmCream,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.artist,
                    color = softLatte,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Timeline Progress Slider layout
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = if (song.duration > 0) progress.toFloat() / song.duration else 0f,
                    onValueChange = { percent ->
                        onSeek((percent * song.duration).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = coffeeBrown,
                        activeTrackColor = coffeeBrown,
                        inactiveTrackColor = darkMocha
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("player_progress_slider")
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = currentFormatted, color = secondaryText, fontSize = 11.sp)
                    Text(text = durationFormatted, color = secondaryText, fontSize = 11.sp)
                }
            }

            // Primary control decks row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Favorite Button
                IconButton(onClick = onToggleFavorite, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle favorite state on song",
                        tint = if (song.isFavorite) Color.Red else warmCream,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous Button
                IconButton(onClick = onPrevious, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Skip previous track button", tint = warmCream, modifier = Modifier.size(36.dp))
                }

                // Play / Pause Circle Tactile button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(coffeeBrown)
                        .clickable { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Primary player playback controller",
                        tint = deepEspresso,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Next Button
                IconButton(onClick = onNext, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Skip next track button", tint = warmCream, modifier = Modifier.size(36.dp))
                }

                // Playback order modes toggles (Shuffle / Repeat)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {
                            val trigger = (0..1).random()
                            if (trigger == 0) onToggleShuffle() else onToggleRepeat()
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        val icon = if (shuffleEnabled) Icons.Default.Shuffle else {
                            if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
                        }
                        val tint = if (shuffleEnabled || repeatMode != RepeatMode.OFF) coffeeBrown else warmCream
                        Icon(imageVector = icon, contentDescription = "Toggle play sequence order type", tint = tint, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

// ==========================================
// DRAWER: ACTIVE QUEUE OVERLAY PANEL
// ==========================================
@Composable
fun ActiveQueueDrawer(
    queue: List<SongEntity>,
    activeSongIndex: Int,
    onDismiss: () -> Unit,
    onPlaySong: (SongEntity) -> Unit
) {
    val darkMocha = Color(0xFF2A211C)
    val deepEspresso = Color(0xFF1E1814)
    val coffeeBrown = Color(0xFFB08968)
    val warmCream = Color(0xFFF8F4F0)
    val secondaryText = Color(0xFFCBB9A8)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() }
    ) {
        // Drawer body (occupies right 75% or slides up from bottom)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(darkMocha)
                .clickable(enabled = false) { } // block click dismiss
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Playing Queue", color = warmCream, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Default.Close, contentDescription = "Close queue overlay panel", tint = warmCream)
                    }
                }

                // Songs List
                if (queue.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(text = "Sequence queue is empty.", color = secondaryText, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(queue) { song ->
                            val isActive = queue.indexOf(song) == activeSongIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isActive) coffeeBrown.copy(alpha = 0.25f) else deepEspresso.copy(alpha = 0.5f))
                                    .clickable { onPlaySong(song) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CoffeeAlbumArt(
                                    presetId = song.generativePreset,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = song.title,
                                        color = if (isActive) coffeeBrown else warmCream,
                                        fontSize = 13.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                    Text(text = song.artist, color = secondaryText, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
