package com.opensetlist.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import com.opensetlist.app.data.DataTransfer
import com.opensetlist.app.data.DatabaseDriverFactory
import com.opensetlist.app.data.SongRepository
import com.opensetlist.app.data.db.AppDatabase
import com.opensetlist.app.data.rememberBackupActions
import com.opensetlist.app.data.rememberFileActions
import com.opensetlist.app.data.rememberSetlistHelperActions
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
import com.opensetlist.app.ui.screens.EditorScreen
import com.opensetlist.app.ui.screens.FilteredSongListScreen
import com.opensetlist.app.ui.screens.SetlistListScreen
import com.opensetlist.app.ui.screens.SetlistScreen
import com.opensetlist.app.ui.screens.SettingsScreen
import com.opensetlist.app.ui.screens.SongListScreen
import com.opensetlist.app.ui.screens.TagsScreen
import com.opensetlist.app.ui.theme.AppTheme
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(driverFactory: DatabaseDriverFactory) {
    val database = remember { AppDatabase(driverFactory.createDriver()) }
    val repository = remember { SongRepository(database) }

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
    var showRenameSetlistDialog by remember { mutableStateOf(false) }
    var showDeleteSetlistDialog by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var pendingRestoreData by remember { mutableStateOf<BackupData?>(null) }
    var pendingRenameSetlist by remember { mutableStateOf<Setlist?>(null) }
    var dialogText by remember { mutableStateOf("") }

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
                when (val origin = screen.origin) {
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
                        showMessage("Arquivo de backup inválido")
                    }
                }
                "songs" -> {
                    val parsed = DataTransfer.parseSongsBundleJson(content).orEmpty()
                    if (parsed.isNotEmpty()) {
                        val count = repository.importSongs(parsed)
                        reload()
                        showMessage("$count músicas importadas")
                    } else {
                        showMessage("Arquivo de músicas inválido")
                    }
                }
                "set" -> {
                    val parsed = DataTransfer.parseSetJson(content)
                    if (parsed != null) {
                        val created = repository.importSet(parsed)
                        reload()
                        currentDrawerSection = DrawerSection.SETLISTS
                        currentScreen = Screen.SetlistView(created)
                        showMessage("Setlist \"${created.name}\" importada")
                    } else {
                        showMessage("Arquivo de setlist inválido")
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
            showMessage(if (ok) "Arquivo salvo" else "Falha ao salvar arquivo")
        },
        onShared = { showMessage("Conteúdo compartilhado") },
        getExportBytes = { pendingExportBytes }
    )

    val backupActions = rememberBackupActions(
        onImported = { data ->
            if (data != null && (data.songs.isNotEmpty() || data.setlists.isNotEmpty())) {
                pendingRestoreData = data
                showRestoreConfirm = true
            } else {
                showMessage("Arquivo .db inválido ou não é um backup deste app")
            }
        }
    )

    val setlistHelperActions = rememberSetlistHelperActions(
        onImported = { data ->
            if (data != null) {
                val (songCount, setCount) = repository.importSetlistHelper(data)
                reload()
                showMessage("Importadas $songCount músicas e $setCount setlists do SetList Helper")
            } else {
                showMessage("Falha ao importar backup do SetList Helper")
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
            if (share) fileActions.shareFile("setlist_backup.db", "application/octet-stream")
            else fileActions.saveFile("setlist_backup.db", "application/octet-stream")
        } else {
            showMessage("Falha ao ler o banco de dados")
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

    AppTheme {
        val isTopLevel = currentScreen is Screen.SongList ||
            currentScreen is Screen.SetlistList ||
            currentScreen is Screen.ArtistList ||
            currentScreen is Screen.TagList ||
            currentScreen is Screen.Settings
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
                        }
                    )
                }
            }
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = when (val screen = currentScreen) {
                                    is Screen.SongList -> "Todas as Músicas"
                                    is Screen.SetlistList -> "Set Lists"
                                    is Screen.ArtistList -> "Artistas"
                                    is Screen.TagList -> "Tags"
                                    is Screen.Settings -> "Configurações"
                                    is Screen.ChordView -> screen.song.title
                                    is Screen.SetlistView -> screen.setlist.name
                                    is Screen.ArtistSongs -> screen.artist.name
                                    is Screen.TagSongs -> screen.tag.name
                                    is Screen.Editor -> "Editar Música"
                                }
                            )
                        },
                        navigationIcon = {
                            if (!isTopLevel) {
                                IconButton(onClick = { goBack() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Voltar"
                                    )
                                }
                            } else {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Menu"
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
                                            contentDescription = "Importar .pro"
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
                                            contentDescription = "Nova setlist"
                                        )
                                    }
                                }
                                is Screen.ArtistList -> {
                                    IconButton(onClick = { showNewArtistDialog = true }) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Novo artista"
                                        )
                                    }
                                }
                                is Screen.TagList -> {
                                    IconButton(onClick = { showNewTagDialog = true }) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Nova tag"
                                        )
                                    }
                                }
                                is Screen.SetlistView -> {
                                    IconButton(onClick = { shareSetlist(screen.setlist) }) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Compartilhar setlist"
                                        )
                                    }
                                    IconButton(onClick = {
                                        dialogText = screen.setlist.name
                                        showRenameSetlistDialog = true
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Renomear setlist"
                                        )
                                    }
                                    IconButton(onClick = { showDeleteSetlistDialog = true }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Excluir setlist"
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
                                emptyText = "Nenhuma música deste artista"
                            )
                        }
                        is Screen.TagSongs -> {
                            FilteredSongListScreen(
                                songs = repository.songsByTag(screen.tag.id),
                                onSongClick = { song ->
                                    currentScreen = Screen.ChordView(song, origin = screen)
                                },
                                emptyText = "Nenhuma música com esta tag"
                            )
                        }
                        is Screen.Settings -> {
                            SettingsScreen(
                                setlists = setlists,
                                onExportBackup = { share -> exportBackup(share) },
                                onImportBackup = { backupActions.importBackup() },
                                onExportAllSongs = { share -> exportAllSongs(share) },
                                onImportSongs = { fileActions.importFile() },
                                onImportSet = { fileActions.importFile() },
                                onShareSetlist = { setlist -> shareSetlist(setlist) },
                                onImportSetlistHelper = {
                                    setlistHelperActions.importBackup()
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
                                onCancel = { goBack() }
                            )
                        }
                    }
                }
            }
        }

        if (showNewSetlistDialog) {
            NameDialog(
                title = "Nova setlist",
                confirmLabel = "Criar",
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
                title = "Renomear setlist",
                confirmLabel = "Salvar",
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
                onDismissRequest = { showDeleteSetlistDialog = false },
                title = { Text("Excluir setlist") },
                text = {
                    val current = currentScreen as? Screen.SetlistView
                    Text("Excluir \"${current?.setlist?.name}\"?")
                },
                confirmButton = {
                    TextButton(onClick = {
                        val current = currentScreen as? Screen.SetlistView
                        if (current != null) {
                            repository.deleteSetlist(current.setlist.id)
                            reload()
                            currentScreen = current.backTarget ?: Screen.SongList
                        }
                        showDeleteSetlistDialog = false
                    }) {
                        Text("Excluir", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteSetlistDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (showNewArtistDialog) {
            NameDialog(
                title = "Novo artista",
                confirmLabel = "Criar",
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
                title = "Renomear artista",
                confirmLabel = "Salvar",
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
            AlertDialog(
                onDismissRequest = { showDeleteArtistDialog = false },
                title = { Text("Excluir artista") },
                text = { Text("Excluir \"${pendingDeleteArtist?.name}\"?") },
                confirmButton = {
                    TextButton(onClick = {
                        val target = pendingDeleteArtist
                        if (target != null) repository.deleteArtist(target.id)
                        reload()
                        showDeleteArtistDialog = false
                        pendingDeleteArtist = null
                    }) {
                        Text("Excluir", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteArtistDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (showNewTagDialog) {
            NameDialog(
                title = "Nova tag",
                confirmLabel = "Criar",
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
                title = "Renomear tag",
                confirmLabel = "Salvar",
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
                title = { Text("Excluir tag") },
                text = { Text("Excluir \"${pendingDeleteTag?.name}\"?") },
                confirmButton = {
                    TextButton(onClick = {
                        val target = pendingDeleteTag
                        if (target != null) repository.deleteTag(target.id)
                        reload()
                        showDeleteTagDialog = false
                        pendingDeleteTag = null
                    }) {
                        Text("Excluir", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteTagDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (showRestoreConfirm) {
            AlertDialog(
                onDismissRequest = { showRestoreConfirm = false },
                title = { Text("Restaurar backup") },
                text = {
                    Text("Isso substituirá todos os dados atuais pelos dados do backup. Continuar?")
                },
                confirmButton = {
                    TextButton(onClick = {
                        val data = pendingRestoreData
                        if (data != null && repository.restoreBackup(data)) {
                            reload()
                            currentScreen = Screen.SongList
                            showMessage("Backup restaurado com sucesso")
                        } else {
                            showMessage("Backup inválido ou vazio")
                        }
                        showRestoreConfirm = false
                        pendingRestoreData = null
                    }) {
                        Text("Restaurar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showRestoreConfirm = false
                        pendingRestoreData = null
                    }) {
                        Text("Cancelar")
                    }
                }
            )
        }
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
                placeholder = { Text("Nome") },
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
                Text("Cancelar")
            }
        }
    )
}
