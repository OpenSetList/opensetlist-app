package com.opensetlist.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opensetlist.app.data.DataTransfer
import com.opensetlist.app.data.DatabaseDriverFactory
import com.opensetlist.app.data.SongRepository
import com.opensetlist.app.data.db.AppDatabase
import com.opensetlist.app.data.rememberBackupActions
import com.opensetlist.app.data.rememberFileActions
import com.opensetlist.app.data.rememberSetlistHelperActions
import com.opensetlist.app.data.KeepScreenOn
import com.opensetlist.app.data.rememberSettingsStore
import com.opensetlist.app.data.currentTimestampCompact
import com.opensetlist.app.model.Artist
import com.opensetlist.app.model.BackupData
import com.opensetlist.app.model.Setlist
import com.opensetlist.app.model.Song
import com.opensetlist.app.model.Tag
import com.opensetlist.app.ui.components.AppBackHandler
import com.opensetlist.app.ui.components.DrawerSection
import com.opensetlist.app.ui.components.SideDrawer
import com.opensetlist.app.ui.screens.ArtistsScreen
import com.opensetlist.app.ui.screens.ChordViewerScreen
import com.opensetlist.app.ui.screens.CloudTarget
import com.opensetlist.app.ui.screens.EditorScreen
import com.opensetlist.app.ui.screens.FilteredSongListScreen
import com.opensetlist.app.ui.screens.SetlistListScreen
import com.opensetlist.app.ui.screens.SetlistScreen
import com.opensetlist.app.ui.screens.SettingsScreen
import com.opensetlist.app.ui.screens.SongListScreen
import com.opensetlist.app.ui.screens.TagsScreen
import com.opensetlist.app.ui.theme.AppTheme
import kotlinx.coroutines.launch

/**
 * Telas navegáveis do app e seus parâmetros.
 *
 * @author ruanitto
 */
sealed class Screen {
    data object SongList : Screen()
    data object SetlistList : Screen()
    data object ArtistList : Screen()
    data object TagList : Screen()
    data object Settings : Screen()
    data class ChordView(
        val song: Song,
        val siblings: List<Song> = listOf(song),
        val index: Int = 0,
        val origin: Screen? = null
    ) : Screen()
    data class SetlistView(val setlist: Setlist, val backTarget: Screen? = null) : Screen()
    data class ArtistSongs(val artist: Artist) : Screen()
    data class TagSongs(val tag: Tag) : Screen()
    data class Editor(val song: Song, val returnTo: Screen.ChordView? = null) : Screen()
}

/**
 * Componente raiz do app: banco, navegação, tema e estado global.
 *
 * @author ruanitto
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(driverFactory: DatabaseDriverFactory) {
    val database = remember { AppDatabase(driverFactory.createDriver()) }
    val repository = remember { SongRepository(database) }
    val settingsStore = rememberSettingsStore()

    val systemDark = isSystemInDarkTheme()
    var darkMode by remember {
        mutableStateOf(settingsStore.isDarkMode() ?: systemDark)
    }
    var keepScreenOnViewer by remember { mutableStateOf(settingsStore.keepScreenOnViewer()) }
    var keepScreenOnPlaylist by remember { mutableStateOf(settingsStore.keepScreenOnPlaylist()) }
    var keepScreenOnAlways by remember { mutableStateOf(settingsStore.keepScreenOnAlways()) }

    var songs by remember { mutableStateOf(emptyList<Song>()) }
    var setlists by remember { mutableStateOf(emptyList<Setlist>()) }
    var artists by remember { mutableStateOf(emptyList<Artist>()) }
    var tags by remember { mutableStateOf(emptyList<Tag>()) }
    var artistSongCounts by remember { mutableStateOf(emptyMap<String, Int>()) }
    var tagSongCounts by remember { mutableStateOf(emptyMap<String, Int>()) }
    var tagsBySong by remember { mutableStateOf(emptyMap<String, List<Tag>>()) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.SongList) }
    var currentDrawerSection by remember { mutableStateOf(DrawerSection.ALL_SONGS) }

    var showNewSetlistDialog by remember { mutableStateOf(false) }
    var showCloudSyncDialog by remember { mutableStateOf(false) }
    var showRenameSetlistDialog by remember { mutableStateOf(false) }
    var showDeleteSetlistDialog by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var pendingRestoreData by remember { mutableStateOf<BackupData?>(null) }
    var pendingRenameSetlist by remember { mutableStateOf<Setlist?>(null) }
    var pendingDeleteSetlist by remember { mutableStateOf<Setlist?>(null) }
    var dialogText by remember { mutableStateOf("") }

    var showDeleteSongDialog by remember { mutableStateOf(false) }
    var pendingDeleteSong by remember { mutableStateOf<Song?>(null) }

    var showNewArtistDialog by remember { mutableStateOf(false) }
    var showRenameArtistDialog by remember { mutableStateOf(false) }
    var showDeleteArtistDialog by remember { mutableStateOf(false) }
    var pendingRenameArtist by remember { mutableStateOf<Artist?>(null) }
    var pendingDeleteArtist by remember { mutableStateOf<Artist?>(null) }

    var showNewTagDialog by remember { mutableStateOf(false) }
    var showRenameTagDialog by remember { mutableStateOf(false) }
    var showDeleteTagDialog by remember { mutableStateOf(false) }
    var pendingRenameTag by remember { mutableStateOf<Tag?>(null) }
    var pendingDeleteTag by remember { mutableStateOf<Tag?>(null) }

    var pendingExportContent by remember { mutableStateOf<String?>(null) }
    var pendingExportBytes by remember { mutableStateOf<ByteArray?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun reload() {
        songs = repository.allSongs()
        setlists = repository.allSetlists()
        artists = repository.allArtists()
        tags = repository.allTags()
        artistSongCounts = repository.songCountByArtist()
        tagSongCounts = repository.songCountByTag()
        tagsBySong = repository.tagsBySong()
    }

    fun refreshSetlistScreen() {
        reload()
        val current = currentScreen
        if (current is Screen.SetlistView) {
            val updated = setlists.find { it.id == current.setlist.id }
            if (updated != null) currentScreen = Screen.SetlistView(updated, current.backTarget)
        }
    }

    fun navigateBackFromChord(chord: Screen.ChordView) {
        when (val origin = chord.origin) {
            is Screen.SetlistView -> {
                val setlist = setlists.find { it.id == origin.setlist.id }
                currentScreen = if (setlist != null) {
                    Screen.SetlistView(setlist, origin.backTarget)
                } else {
                    Screen.SongList
                }
            }
            is Screen.ArtistSongs -> currentScreen = Screen.ArtistSongs(origin.artist)
            is Screen.TagSongs -> currentScreen = Screen.TagSongs(origin.tag)
            else -> currentScreen = Screen.SongList
        }
    }

    fun goBack() {
        when (val screen = currentScreen) {
            is Screen.Editor -> {
                reload()
                val saved = repository.getSong(screen.song.id)
                currentScreen = if (saved != null) {
                    if (screen.returnTo != null) screen.returnTo.copy(song = saved)
                    else Screen.ChordView(saved)
                } else {
                    Screen.SongList
                }
            }
            is Screen.ChordView -> {
                reload()
                navigateBackFromChord(screen)
            }
            is Screen.SetlistView -> {
                reload()
                currentScreen = screen.backTarget ?: Screen.SongList
            }
            is Screen.ArtistSongs -> {
                reload()
                currentScreen = Screen.ArtistList
            }
            is Screen.TagSongs -> {
                reload()
                currentScreen = Screen.TagList
            }
            else -> {
                reload()
                currentScreen = Screen.SongList
            }
        }
    }

    fun deleteSong(target: Song) {
        repository.delete(target.id)
        reload()
        when (val screen = currentScreen) {
            is Screen.Editor -> {
                val origin = screen.returnTo
                if (origin != null) navigateBackFromChord(origin) else currentScreen = Screen.SongList
            }
            is Screen.ChordView -> navigateBackFromChord(screen)
            else -> currentScreen = Screen.SongList
        }
    }

    LaunchedEffect(Unit) {
        repository.seedIfEmpty()
        reload()
    }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun importSingleSong(content: String) {
        val imported = repository.importSong(content)
        reload()
        currentScreen = Screen.ChordView(imported)
    }

    fun handleImported(content: String) {
        val trimmed = content.trimStart()
        if (trimmed.startsWith("{")) {
            when (DataTransfer.detectType(content)) {
                "backup" -> {
                    val data = DataTransfer.parseBackupJson(content)
                    if (data != null) {
                        pendingRestoreData = data
                        showRestoreConfirm = true
                    } else {
                        showMessage(AppStrings.invalidBackupFile)
                    }
                }
                "songs" -> {
                    val parsed = DataTransfer.parseSongsBundleJson(content).orEmpty()
                    if (parsed.isNotEmpty()) {
                        val count = repository.importSongs(parsed)
                        reload()
                        showMessage(AppStrings.songsImported(count))
                    } else {
                        showMessage(AppStrings.invalidSongsFile)
                    }
                }
                "set" -> {
                    val parsed = DataTransfer.parseSetJson(content)
                    if (parsed != null) {
                        val created = repository.importSet(parsed)
                        reload()
                        currentDrawerSection = DrawerSection.SETLISTS
                        currentScreen = Screen.SetlistView(created)
                        showMessage(AppStrings.setlistImported(created.name))
                    } else {
                        showMessage(AppStrings.invalidSetlistFile)
                    }
                }
                else -> importSingleSong(content)
            }
        } else {
            importSingleSong(content)
        }
    }

    val fileActions = rememberFileActions(
        getExportContent = { pendingExportContent },
        onImported = { content -> handleImported(content) },
        onExported = { ok ->
            showMessage(if (ok) AppStrings.fileSaved else AppStrings.fileSaveFailed)
        },
        onShared = { showMessage(AppStrings.contentShared) },
        getExportBytes = { pendingExportBytes }
    )

    val backupActions = rememberBackupActions(
        onImported = { data ->
            if (data != null && (data.songs.isNotEmpty() || data.setlists.isNotEmpty())) {
                pendingRestoreData = data
                showRestoreConfirm = true
            } else {
                showMessage(AppStrings.invalidDbFile)
            }
        }
    )

    val setlistHelperActions = rememberSetlistHelperActions(
        onImported = { data ->
            if (data != null) {
                val (songCount, setCount) = repository.importSetlistHelper(data)
                reload()
                showMessage(AppStrings.slhImported(songCount, setCount))
            } else {
                showMessage(AppStrings.slhImportFailed)
            }
        }
    )

    fun doExport(fileName: String, mimeType: String, content: String, share: Boolean) {
        pendingExportContent = content
        pendingExportBytes = null
        if (share) fileActions.shareFile(fileName, mimeType)
        else fileActions.saveFile(fileName, mimeType)
    }

    fun exportBackup(share: Boolean) {
        val bytes = backupActions.exportBytes()
        if (bytes != null) {
            pendingExportBytes = bytes
            pendingExportContent = null
            val fileName = "setlist_backup_${currentTimestampCompact()}.db"
            if (share) fileActions.shareFile(fileName, "application/octet-stream")
            else fileActions.saveFile(fileName, "application/octet-stream")
        } else {
            showMessage(AppStrings.dbReadFailed)
        }
    }

    fun exportAllSongs(share: Boolean) {
        val json = DataTransfer.buildSongsBundleJson(repository.allSongs())
        doExport("setlist_musicas.json", "application/json", json, share)
    }

    fun shareSetlist(setlist: Setlist) {
        val json = DataTransfer.buildSetJson(setlist, repository.songsInSetlist(setlist.id))
        doExport("set_${setlist.name}.json", "application/json", json, share = true)
    }

    AppTheme(darkTheme = darkMode) {
        val isTopLevel = currentScreen is Screen.SongList ||
            currentScreen is Screen.SetlistList ||
            currentScreen is Screen.ArtistList ||
            currentScreen is Screen.TagList ||
            currentScreen is Screen.Settings
        val hidesTopBar = currentScreen is Screen.ChordView || currentScreen is Screen.Editor
        val keepScreenOn = keepScreenOnAlways ||
            (keepScreenOnViewer && currentScreen is Screen.ChordView) ||
            (keepScreenOnPlaylist && currentScreen is Screen.SetlistView)
        KeepScreenOn(enabled = keepScreenOn)
        AppBackHandler(enabled = !isTopLevel, onBack = ::goBack)

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    SideDrawer(
                        currentSection = currentDrawerSection,
                        onSectionSelected = { section ->
                            currentDrawerSection = section
                            currentScreen = when (section) {
                                DrawerSection.ALL_SONGS -> Screen.SongList
                                DrawerSection.SETLISTS -> Screen.SetlistList
                                DrawerSection.ARTISTS -> Screen.ArtistList
                                DrawerSection.TAGS -> Screen.TagList
                                DrawerSection.SETTINGS -> Screen.Settings
                            }
                            scope.launch { drawerState.close() }
                        },
                        onSyncClick = {
                            scope.launch { drawerState.close() }
                            showCloudSyncDialog = true
                        }
                    )
                }
            }
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    if (!hidesTopBar) {
                        TopAppBar(
                            title = {
                                Text(
                                    text = when (val screen = currentScreen) {
                                        is Screen.SongList -> AppStrings.allSongsTitle
                                        is Screen.SetlistList -> AppStrings.setlistsTitle
                                        is Screen.ArtistList -> AppStrings.artistsTitle
                                        is Screen.TagList -> AppStrings.tagsTitle
                                        is Screen.Settings -> AppStrings.settingsTitle
                                        is Screen.ChordView -> screen.song.title
                                        is Screen.SetlistView -> screen.setlist.name
                                        is Screen.ArtistSongs -> screen.artist.name
                                        is Screen.TagSongs -> screen.tag.name
                                        is Screen.Editor -> AppStrings.editSongTitle
                                    }
                                )
                            },
                            navigationIcon = {
                                if (!isTopLevel) {
                                    IconButton(onClick = { goBack() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = AppStrings.back
                                        )
                                    }
                                } else {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = AppStrings.menu
                                        )
                                    }
                                }
                            },
                            actions = {
                                when (val screen = currentScreen) {
                                    is Screen.SongList -> {
                                        IconButton(onClick = { fileActions.importFile() }) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = AppStrings.importPro
                                            )
                                        }
                                    }
                                    is Screen.SetlistList -> {
                                        IconButton(onClick = {
                                            dialogText = ""
                                            showNewSetlistDialog = true
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = AppStrings.newSetlist
                                            )
                                        }
                                    }
                                    is Screen.ArtistList -> {
                                        IconButton(onClick = { showNewArtistDialog = true }) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = AppStrings.newArtist
                                            )
                                        }
                                    }
                                    is Screen.TagList -> {
                                        IconButton(onClick = { showNewTagDialog = true }) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = AppStrings.newTag
                                            )
                                        }
                                    }
                                    is Screen.SetlistView -> {
                                        IconButton(onClick = { shareSetlist(screen.setlist) }) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = AppStrings.shareSetlist
                                            )
                                        }
                                        IconButton(onClick = {
                                            dialogText = screen.setlist.name
                                            showRenameSetlistDialog = true
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = AppStrings.renameSetlist
                                            )
                                        }
                                        IconButton(onClick = { showDeleteSetlistDialog = true }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = AppStrings.deleteSetlist
                                            )
                                        }
                                    }
                                    else -> {}
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                },
                contentWindowInsets = if (hidesTopBar) {
                    WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                } else {
                    ScaffoldDefaults.contentWindowInsets
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    when (val screen = currentScreen) {
                        is Screen.SongList -> {
                            SongListScreen(
                                songs = songs,
                                setlists = setlists,
                                onSongClick = { song ->
                                    currentScreen = Screen.ChordView(song)
                                },
                                onSetlistClick = { setlist ->
                                    currentDrawerSection = DrawerSection.SETLISTS
                                    currentScreen = Screen.SetlistView(setlist)
                                },
                                onNewSong = {
                                    currentScreen = Screen.Editor(repository.newSong())
                                },
                                onDeleteSong = { song ->
                                    pendingDeleteSong = song
                                    showDeleteSongDialog = true
                                }
                            )
                        }
                        is Screen.SetlistList -> {
                            SetlistListScreen(
                                setlists = setlists,
                                onSetlistClick = { setlist ->
                                    currentDrawerSection = DrawerSection.SETLISTS
                                    currentScreen = Screen.SetlistView(setlist, backTarget = Screen.SetlistList)
                                },
                                onShare = { setlist -> shareSetlist(setlist) },
                                onEdit = { setlist ->
                                    dialogText = setlist.name
                                    pendingRenameSetlist = setlist
                                    showRenameSetlistDialog = true
                                },
                                onDelete = { setlist ->
                                    pendingDeleteSetlist = setlist
                                    showDeleteSetlistDialog = true
                                }
                            )
                        }
                        is Screen.ArtistList -> {
                            ArtistsScreen(
                                artists = artists,
                                songCounts = artistSongCounts,
                                onArtistClick = { artist ->
                                    currentDrawerSection = DrawerSection.ARTISTS
                                    currentScreen = Screen.ArtistSongs(artist)
                                },
                                onEdit = { artist ->
                                    dialogText = artist.name
                                    pendingRenameArtist = artist
                                    showRenameArtistDialog = true
                                },
                                onDelete = { artist ->
                                    pendingDeleteArtist = artist
                                    showDeleteArtistDialog = true
                                }
                            )
                        }
                        is Screen.TagList -> {
                            TagsScreen(
                                tags = tags,
                                songCounts = tagSongCounts,
                                onTagClick = { tag ->
                                    currentDrawerSection = DrawerSection.TAGS
                                    currentScreen = Screen.TagSongs(tag)
                                },
                                onEdit = { tag ->
                                    dialogText = tag.name
                                    pendingRenameTag = tag
                                    showRenameTagDialog = true
                                },
                                onDelete = { tag ->
                                    pendingDeleteTag = tag
                                    showDeleteTagDialog = true
                                }
                            )
                        }
                        is Screen.ArtistSongs -> {
                            FilteredSongListScreen(
                                songs = repository.songsByArtist(screen.artist.name),
                                onSongClick = { song ->
                                    currentScreen = Screen.ChordView(song, origin = screen)
                                },
                                emptyText = AppStrings.noSongsOfArtist
                            )
                        }
                        is Screen.TagSongs -> {
                            FilteredSongListScreen(
                                songs = repository.songsByTag(screen.tag.id),
                                onSongClick = { song ->
                                    currentScreen = Screen.ChordView(song, origin = screen)
                                },
                                emptyText = AppStrings.noSongsWithTag
                            )
                        }
                        is Screen.Settings -> {
                            SettingsScreen(
                                onExportBackup = { share -> exportBackup(share) },
                                onImportBackup = { backupActions.importBackup() },
                                onExportAllSongs = { share -> exportAllSongs(share) },
                                onImportSongs = { fileActions.importFile() },
                                onImportSet = { fileActions.importFile() },
                                onImportSetlistHelper = {
                                    setlistHelperActions.importBackup()
                                },
                                onCloudExport = { exportBackup(false) },
                                onCloudImport = { fileActions.importFile() },
                                darkMode = darkMode,
                                onDarkModeChange = { value ->
                                    darkMode = value
                                    settingsStore.setDarkMode(value)
                                },
                                keepScreenOnViewer = keepScreenOnViewer,
                                onKeepScreenOnViewerChange = { value ->
                                    keepScreenOnViewer = value
                                    settingsStore.setKeepScreenOnViewer(value)
                                },
                                keepScreenOnPlaylist = keepScreenOnPlaylist,
                                onKeepScreenOnPlaylistChange = { value ->
                                    keepScreenOnPlaylist = value
                                    settingsStore.setKeepScreenOnPlaylist(value)
                                },
                                keepScreenOnAlways = keepScreenOnAlways,
                                onKeepScreenOnAlwaysChange = { value ->
                                    keepScreenOnAlways = value
                                    settingsStore.setKeepScreenOnAlways(value)
                                }
                            )
                        }
                        is Screen.ChordView -> {
                            ChordViewerScreen(
                                songs = screen.siblings,
                                initialIndex = screen.index,
                                songTags = tagsBySong,
                                onBack = { goBack() },
                                onEdit = { song ->
                                    currentScreen = Screen.Editor(song, screen)
                                },
                                onDelete = { song ->
                                    pendingDeleteSong = song
                                    showDeleteSongDialog = true
                                },
                                onNavigateTo = { index ->
                                    if (index in screen.siblings.indices) {
                                        currentScreen = screen.copy(
                                            song = screen.siblings[index],
                                            index = index
                                        )
                                    }
                                }
                            )
                        }
                        is Screen.SetlistView -> {
                            SetlistScreen(
                                setlist = screen.setlist,
                                allSongs = songs,
                                onSongClick = { song ->
                                    val setSongs = screen.setlist.songs
                                    val index = setSongs.indexOfFirst { it.id == song.id }
                                        .coerceAtLeast(0)
                                    currentScreen = Screen.ChordView(
                                        song = song,
                                        siblings = setSongs,
                                        index = index,
                                        origin = screen
                                    )
                                },
                                onRename = { name ->
                                    repository.renameSetlist(screen.setlist.id, name)
                                    refreshSetlistScreen()
                                },
                                onDelete = {
                                    repository.deleteSetlist(screen.setlist.id)
                                    reload()
                                    currentScreen = screen.backTarget ?: Screen.SongList
                                },
                                onUpdateInfo = { date, location, time ->
                                    repository.updateSetlistInfo(
                                        screen.setlist.id,
                                        date,
                                        location,
                                        time
                                    )
                                    refreshSetlistScreen()
                                },
                                onReorder = { ordered ->
                                    repository.reorderSetlistSongs(
                                        screen.setlist.id,
                                        ordered.map { it.id }
                                    )
                                    refreshSetlistScreen()
                                },
                                onAddSong = { song ->
                                    repository.addSongToSetlist(screen.setlist.id, song.id)
                                    refreshSetlistScreen()
                                },
                                onRemoveSong = { song ->
                                    repository.removeSongFromSetlist(screen.setlist.id, song.id)
                                    refreshSetlistScreen()
                                }
                            )
                        }
                        is Screen.Editor -> {
                            EditorScreen(
                                song = screen.song,
                                allTags = tags,
                                initialTags = tagsBySong[screen.song.id].orEmpty(),
                                artistSuggestions = artists.map { it.name },
                                onSave = { updated, tagIds ->
                                    repository.upsert(updated)
                                    repository.setSongTags(updated.id, tagIds)
                                    songs = repository.allSongs()
                                    setlists = repository.allSetlists()
                                    reload()
                                    currentScreen = if (screen.returnTo != null) {
                                        val siblings = screen.returnTo.siblings.map {
                                            if (it.id == updated.id) updated else it
                                        }
                                        screen.returnTo.copy(
                                            song = updated,
                                            siblings = siblings
                                        )
                                    } else {
                                        Screen.ChordView(updated)
                                    }
                                },
                                onNewTag = { name ->
                                    repository.createTag(name)
                                    reload()
                                },
                                onCancel = { goBack() },
                                onDelete = { song ->
                                    pendingDeleteSong = song
                                    showDeleteSongDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showCloudSyncDialog) {
            AlertDialog(
                onDismissRequest = { showCloudSyncDialog = false },
                title = { Text(AppStrings.syncTitle) },
                text = {
                    Column {
                        CloudSyncRow(
                            icon = Icons.Default.CloudUpload,
                            label = AppStrings.exportToGoogleDrive,
                            onClick = {
                                showCloudSyncDialog = false
                                exportBackup(false)
                            }
                        )
                        CloudSyncRow(
                            icon = Icons.Default.CloudDownload,
                            label = AppStrings.importFromGoogleDrive,
                            onClick = {
                                showCloudSyncDialog = false
                                fileActions.importFile()
                            }
                        )
                        CloudSyncRow(
                            icon = Icons.Default.CloudUpload,
                            label = AppStrings.exportToDropbox,
                            onClick = {
                                showCloudSyncDialog = false
                                exportBackup(false)
                            }
                        )
                        CloudSyncRow(
                            icon = Icons.Default.CloudDownload,
                            label = AppStrings.importFromDropbox,
                            onClick = {
                                showCloudSyncDialog = false
                                fileActions.importFile()
                            }
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showCloudSyncDialog = false }) {
                        Text(AppStrings.cancel)
                    }
                }
            )
        }

        if (showNewSetlistDialog) {
            NameDialog(
                title = AppStrings.newSetlist,
                confirmLabel = AppStrings.create,
                initialName = dialogText,
                onConfirm = { name ->
                    val created = repository.createSetlist(name)
                    reload()
                    currentDrawerSection = DrawerSection.SETLISTS
                    currentScreen = Screen.SetlistView(created, backTarget = Screen.SetlistList)
                    showNewSetlistDialog = false
                },
                onDismiss = { showNewSetlistDialog = false }
            )
        }

        if (showRenameSetlistDialog) {
            val current = currentScreen as? Screen.SetlistView
            val targetId = pendingRenameSetlist?.id ?: current?.setlist?.id
            NameDialog(
                title = AppStrings.renameSetlist,
                confirmLabel = AppStrings.save,
                initialName = dialogText,
                onConfirm = { name ->
                    if (targetId != null) repository.renameSetlist(targetId, name)
                    refreshSetlistScreen()
                    showRenameSetlistDialog = false
                    pendingRenameSetlist = null
                },
                onDismiss = {
                    showRenameSetlistDialog = false
                    pendingRenameSetlist = null
                }
            )
        }

        if (showDeleteSetlistDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteSetlistDialog = false
                    pendingDeleteSetlist = null
                },
                title = { Text(AppStrings.deleteSetlist) },
                text = {
                    val target = pendingDeleteSetlist
                        ?: (currentScreen as? Screen.SetlistView)?.setlist
                    Text(AppStrings.deleteConfirmation.format(target?.name ?: ""))
                },
                confirmButton = {
                    TextButton(onClick = {
                        val target = pendingDeleteSetlist
                        val current = currentScreen as? Screen.SetlistView
                        when {
                            target != null -> {
                                repository.deleteSetlist(target.id)
                                reload()
                            }
                            current != null -> {
                                repository.deleteSetlist(current.setlist.id)
                                reload()
                                currentScreen = current.backTarget ?: Screen.SongList
                            }
                        }
                        showDeleteSetlistDialog = false
                        pendingDeleteSetlist = null
                    }) {
                        Text(AppStrings.delete, color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteSetlistDialog = false
                        pendingDeleteSetlist = null
                    }) {
                        Text(AppStrings.cancel)
                    }
                }
            )
        }

        if (showDeleteSongDialog) {
            val target = pendingDeleteSong
            AlertDialog(
                onDismissRequest = {
                    showDeleteSongDialog = false
                    pendingDeleteSong = null
                },
                title = { Text(AppStrings.deleteSong) },
                text = { Text(AppStrings.deleteConfirmation.format(target?.title ?: "")) },
                confirmButton = {
                    TextButton(onClick = {
                        val song = target
                        if (song != null) deleteSong(song)
                        showDeleteSongDialog = false
                        pendingDeleteSong = null
                    }) {
                        Text(AppStrings.delete, color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteSongDialog = false
                        pendingDeleteSong = null
                    }) {
                        Text(AppStrings.cancel)
                    }
                }
            )
        }

        if (showNewArtistDialog) {
            NameDialog(
                title = AppStrings.newArtist,
                confirmLabel = AppStrings.create,
                initialName = "",
                onConfirm = { name ->
                    repository.createArtist(name)
                    reload()
                    currentDrawerSection = DrawerSection.ARTISTS
                    showNewArtistDialog = false
                },
                onDismiss = { showNewArtistDialog = false }
            )
        }

        if (showRenameArtistDialog) {
            val target = pendingRenameArtist
            NameDialog(
                title = AppStrings.renameArtist,
                confirmLabel = AppStrings.save,
                initialName = target?.name ?: "",
                onConfirm = { name ->
                    if (target != null) repository.renameArtist(target.id, name)
                    reload()
                    showRenameArtistDialog = false
                    pendingRenameArtist = null
                },
                onDismiss = {
                    showRenameArtistDialog = false
                    pendingRenameArtist = null
                }
            )
        }

        if (showDeleteArtistDialog) {
            val target = pendingDeleteArtist
            var deleteSongs by remember(target) { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = { showDeleteArtistDialog = false },
                title = { Text(AppStrings.deleteArtist) },
                text = {
                    Column {
                        Text(AppStrings.deleteConfirmation.format(target?.name ?: ""))
                        val count = target?.let { artistSongCounts[it.name] ?: 0 } ?: 0
                        if (count > 0) {
                            Row(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .clickable { deleteSongs = !deleteSongs },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = deleteSongs,
                                    onCheckedChange = { deleteSongs = it }
                                )
                                Text(
                                    text = AppStrings.deleteArtistWithSongs.format(count),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val t = target
                        if (t != null) {
                            if (deleteSongs) repository.deleteArtistAndSongs(t.id)
                            else repository.deleteArtist(t.id)
                            reload()
                        }
                        showDeleteArtistDialog = false
                        pendingDeleteArtist = null
                    }) {
                        Text(AppStrings.delete, color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteArtistDialog = false }) {
                        Text(AppStrings.cancel)
                    }
                }
            )
        }

        if (showNewTagDialog) {
            NameDialog(
                title = AppStrings.newTag,
                confirmLabel = AppStrings.create,
                initialName = "",
                onConfirm = { name ->
                    repository.createTag(name)
                    reload()
                    currentDrawerSection = DrawerSection.TAGS
                    showNewTagDialog = false
                },
                onDismiss = { showNewTagDialog = false }
            )
        }

        if (showRenameTagDialog) {
            val target = pendingRenameTag
            NameDialog(
                title = AppStrings.renameTag,
                confirmLabel = AppStrings.save,
                initialName = target?.name ?: "",
                onConfirm = { name ->
                    if (target != null) repository.renameTag(target.id, name)
                    reload()
                    showRenameTagDialog = false
                    pendingRenameTag = null
                },
                onDismiss = {
                    showRenameTagDialog = false
                    pendingRenameTag = null
                }
            )
        }

        if (showDeleteTagDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteTagDialog = false },
                title = { Text(AppStrings.deleteTag) },
                text = { Text(AppStrings.deleteConfirmation.format(pendingDeleteTag?.name ?: "")) },
                confirmButton = {
                    TextButton(onClick = {
                        val target = pendingDeleteTag
                        if (target != null) repository.deleteTag(target.id)
                        reload()
                        showDeleteTagDialog = false
                        pendingDeleteTag = null
                    }) {
                        Text(AppStrings.delete, color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteTagDialog = false }) {
                        Text(AppStrings.cancel)
                    }
                }
            )
        }

        if (showRestoreConfirm) {
            AlertDialog(
                onDismissRequest = { showRestoreConfirm = false },
                title = { Text(AppStrings.restoreBackup) },
                text = {
                    Text(AppStrings.restoreBackupConfirm)
                },
                confirmButton = {
                    TextButton(onClick = {
                        val data = pendingRestoreData
                        if (data != null && repository.restoreBackup(data)) {
                            reload()
                            currentScreen = Screen.SongList
                            showMessage(AppStrings.backupRestored)
                        } else {
                            showMessage(AppStrings.invalidOrEmptyBackup)
                        }
                        showRestoreConfirm = false
                        pendingRestoreData = null
                    }) {
                        Text(AppStrings.restore, color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showRestoreConfirm = false
                        pendingRestoreData = null
                    }) {
                        Text(AppStrings.cancel)
                    }
                }
            )
        }
    }
}

@Composable
private fun CloudSyncRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun NameDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(AppStrings.name) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.cancel)
            }
        }
    )
}
