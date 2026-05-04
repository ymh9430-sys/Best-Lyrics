package com.lyrics.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.lyrics.app.model.LyricsResult
import com.lyrics.app.model.LyricsType
import com.lyrics.app.model.SongInfo
import com.lyrics.app.model.UiState
import com.lyrics.app.network.SearchResult
import com.lyrics.app.ui.theme.LyricsAppTheme
import com.lyrics.app.utils.LyricsConverter

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val sharedText = intent?.takeIf { it.action == Intent.ACTION_SEND }
            ?.getStringExtra(Intent.EXTRA_TEXT)

        setContent {
            LyricsAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel = viewModel, sharedText = sharedText ?: "")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, sharedText: String = "") {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedNav by remember { mutableIntStateOf(0) }

    BackHandler(enabled = uiState is UiState.Success || uiState is UiState.SongFound || uiState is UiState.Error) {
        viewModel.reset()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Best Lyrics", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    if (uiState is UiState.Success || uiState is UiState.SongFound) {
                        IconButton(onClick = { viewModel.reset() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedNav == 0,
                    onClick = { selectedNav = 0 },
                    icon = { Icon(if (selectedNav == 0) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedNav == 1,
                    onClick = { selectedNav = 1 },
                    icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    label = { Text("Manual") }
                )
                NavigationBarItem(
                    selected = selectedNav == 2,
                    onClick = { selectedNav = 2 },
                    icon = { Icon(if (selectedNav == 2) Icons.Filled.Star else Icons.Outlined.Star, contentDescription = null) },
                    label = { Text("Good Lyrics") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedNav) {
                0 -> HomeScreen(viewModel = viewModel, sharedText = sharedText)
                1 -> ManualSearchScreen(viewModel = viewModel, uiState = uiState)
                2 -> ComingSoonScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel, sharedText: String = "") {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val topCharts by viewModel.topCharts.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableIntStateOf(1) } // 0=All, 1=Songs, 2=Albums, 3=Artists
    val filters = listOf("All", "Songs", "Albums", "Artists")
    val context = LocalContext.current

    LaunchedEffect(sharedText) {
        if (sharedText.isNotBlank()) viewModel.processUrl(sharedText)
    }

    // Results screen
    AnimatedVisibility(
        visible = uiState is UiState.Loading || uiState is UiState.SongFound || uiState is UiState.Success || uiState is UiState.Error,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                AnimatedContent(
                    targetState = uiState,
                    transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                    label = "results"
                ) { state ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        when (state) {
                            is UiState.Loading -> LoadingCard()
                            is UiState.SongFound -> SongFoundCard(state.song)
                            is UiState.Error -> ErrorCard(state.message)
                            is UiState.Success -> {
                                SongInfoCard(state.song)
                                for (result in state.results) {
                                    LyricsCard(
                                        result = result,
                                        onCopy = { text ->
                                            copyToClipboard(context, text)
                                            Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        onShare = { text -> shareText(context, text, state.song) }
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    // Home screen
    AnimatedVisibility(
        visible = uiState is UiState.Idle,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Search Bar
            SearchBar(
                query = query,
                onQueryChange = {
                    query = it
                    viewModel.search(it)
                },
                onSearch = { viewModel.search(it) },
                active = searchActive,
                onActiveChange = { searchActive = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (searchActive) 0.dp else 16.dp)
                    .padding(vertical = if (searchActive) 0.dp else 8.dp),
                placeholder = { Text("Search lyrics...") },
                leadingIcon = {
                    if (searchActive) {
                        IconButton(onClick = { searchActive = false; query = ""; viewModel.search("") }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = null)
                        }
                    } else {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    }
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = ""; viewModel.search("") }) {
                            Icon(Icons.Filled.Close, contentDescription = null)
                        }
                    }
                },
                shape = RoundedCornerShape(if (searchActive) 0.dp else 50.dp)
            ) {
                // Filter chips inside search
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEachIndexed { index, label ->
                        FilterChip(
                            selected = selectedFilter == index,
                            onClick = { selectedFilter = index },
                            label = { Text(label) }
                        )
                    }
                }

                if (isSearching) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                } else if (searchResults.isEmpty() && query.isNotBlank()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No results found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (searchResults.isNotEmpty()) {
                    // Results count
                    Text(
                        "${searchResults.size} SONGS",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    LazyColumn {
                        items(searchResults) { result ->
                            SongListItem(
                                result = result,
                                showArrow = true,
                                onClick = {
                                    searchActive = false
                                    query = ""
                                    viewModel.fetchLyricsFromResult(result)
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 80.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }

            // Home content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Hero card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Find lyrics for", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "any song",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Search by song title, artist or paste a link",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Popular Songs header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Popular Songs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (topCharts.isEmpty()) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(topCharts) { result ->
                    SongListItem(
                        result = result,
                        showArrow = true,
                        onClick = { viewModel.fetchLyricsFromResult(result) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 80.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun SongListItem(result: SearchResult, showArrow: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AsyncImage(
            model = result.artworkUrl,
            contentDescription = null,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                result.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${result.artist} • ${formatDuration(result.duration)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (showArrow) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

@Composable
fun ManualSearchScreen(viewModel: MainViewModel, uiState: UiState) {
    val context = LocalContext.current
    var titleText by remember { mutableStateOf("") }
    var artistText by remember { mutableStateOf("") }
    var albumText by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.ime),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        AnimatedVisibility(
            visible = uiState is UiState.Idle || uiState is UiState.Error,
            enter = fadeIn(tween(300)) + expandVertically(tween(300)),
            exit = fadeOut(tween(300)) + shrinkVertically(tween(300))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Manual Search", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = titleText, onValueChange = { titleText = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Title") }, leadingIcon = { Icon(Icons.Filled.MusicNote, contentDescription = null) }, shape = RoundedCornerShape(12.dp), singleLine = true)
                OutlinedTextField(value = artistText, onValueChange = { artistText = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Artist") }, leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) }, shape = RoundedCornerShape(12.dp), singleLine = true)
                OutlinedTextField(value = albumText, onValueChange = { albumText = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Album") }, leadingIcon = { Icon(Icons.Filled.Album, contentDescription = null) }, shape = RoundedCornerShape(12.dp), singleLine = true)
                OutlinedTextField(value = durationText, onValueChange = { durationText = it.filter { c -> c.isDigit() } }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Duration (seconds)") }, leadingIcon = { Icon(Icons.Filled.Timer, contentDescription = null) }, shape = RoundedCornerShape(12.dp), singleLine = true)
                Button(
                    onClick = {
                        val dur = durationText.toIntOrNull() ?: 0
                        viewModel.processManual(titleText, artistText, albumText, dur)
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    enabled = titleText.isNotBlank() && artistText.isNotBlank() && albumText.isNotBlank() && durationText.isNotBlank(),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Go", fontWeight = FontWeight.Bold)
                }
            }
        }

        AnimatedContent(
            targetState = uiState,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
            label = "manualState"
        ) { state ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (state) {
                    is UiState.Loading -> LoadingCard()
                    is UiState.Error -> ErrorCard(state.message)
                    is UiState.Success -> {
                        SongInfoCard(state.song)
                        for (result in state.results) {
                            LyricsCard(
                                result = result,
                                onCopy = { text -> copyToClipboard(context, text); Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show() },
                                onShare = { text -> shareText(context, text, state.song) }
                            )
                        }
                    }
                    else -> Box(modifier = Modifier.fillMaxWidth())
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ComingSoonScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Coming Soon", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("More lyrics sources will be added here", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun LoadingCard() {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text("Fetching lyrics...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SongFoundCard(song: SongInfo) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
            Column {
                Text(song.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("${song.artist} • ${song.duration}s", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun SongInfoCard(song: SongInfo) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(song.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(song.artist, style = MaterialTheme.typography.bodyMedium)
            Text("💿 ${song.album} • ⏱ ${song.duration}s", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ErrorCard(message: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
fun LyricsCard(result: LyricsResult, onCopy: (String) -> Unit, onShare: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedFormat by remember { mutableIntStateOf(0) }

    val formats = if (result.type == LyricsType.WORD) listOf("Karaoke", "Karaoke 2", "Synced", "Plain")
    else listOf("Synced", "Plain")

    val formattedLyrics = when {
        result.type == LyricsType.LINE && selectedFormat == 0 -> LyricsConverter.toSynced(result.lyrics)
        result.type == LyricsType.LINE && selectedFormat == 1 -> LyricsConverter.toPlain(result.lyrics)
        result.type == LyricsType.WORD && selectedFormat == 0 -> result.lyrics
        result.type == LyricsType.WORD && selectedFormat == 1 -> LyricsConverter.toKaraoke2(result.lyrics)
        result.type == LyricsType.WORD && selectedFormat == 2 -> LyricsConverter.toSynced(result.lyrics)
        result.type == LyricsType.WORD && selectedFormat == 3 -> LyricsConverter.toPlain(result.lyrics)
        else -> result.lyrics
    }

    val previewText = formattedLyrics.lines().take(3).joinToString("\n")

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(result.source, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Text("${formattedLyrics.lines().size} Lines", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                    IconButton(onClick = { onCopy(formattedLyrics) }) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy") }
                    IconButton(onClick = { onShare(formattedLyrics) }) { Icon(Icons.Outlined.Share, contentDescription = "Share") }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

            AnimatedContent(
                targetState = expanded to selectedFormat,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "lyrics"
            ) { (isExpanded, _) ->
                Text(text = if (isExpanded) formattedLyrics else previewText, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, lineHeight = 20.sp))
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                formats.forEachIndexed { index, label ->
                    if (selectedFormat == index) {
                        Button(onClick = { selectedFormat = index }, shape = RoundedCornerShape(50), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp), modifier = Modifier.height(34.dp)) {
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        OutlinedButton(onClick = { selectedFormat = index }, shape = RoundedCornerShape(50), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp), modifier = Modifier.height(34.dp)) {
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("lyrics", text))
}

private fun shareText(context: Context, lyrics: String, song: SongInfo) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "${song.title} - ${song.artist}")
        putExtra(Intent.EXTRA_TEXT, lyrics)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Lyrics"))
}
