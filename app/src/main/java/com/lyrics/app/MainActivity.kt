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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
        val sharedText = intent?.takeIf { it.action == Intent.ACTION_SEND }?.getStringExtra(Intent.EXTRA_TEXT)
        setContent {
            LyricsAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0A0A0A)) {
                    MainScreen(viewModel = viewModel, sharedText = sharedText ?: "")
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel, sharedText: String = "") {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedNav by remember { mutableIntStateOf(0) }
    var searchActive by remember { mutableStateOf(false) }
    var showSeeAll by remember { mutableStateOf(false) }

    // هل التطبيق في وضع عرض الكلمات
    val isShowingLyrics = uiState is UiState.Success || uiState is UiState.SongFound || uiState is UiState.Loading

    BackHandler(enabled = showSeeAll) { showSeeAll = false }
    BackHandler(enabled = !showSeeAll && searchActive) { searchActive = false }
    BackHandler(enabled = !showSeeAll && !searchActive && (uiState is UiState.Success || uiState is UiState.SongFound || uiState is UiState.Error)) {
        viewModel.reset()
    }

    Scaffold(
        bottomBar = {
            // يختفي في صفحة الكلمات وصفحة البحث
            AnimatedVisibility(
                visible = !searchActive && !showSeeAll && !isShowingLyrics,
                enter = slideInVertically(tween(200)) { it },
                exit = slideOutVertically(tween(200)) { it }
            ) {
                NavigationBar(containerColor = Color(0xFF111111), tonalElevation = 0.dp) {
                    NavigationBarItem(
                        selected = selectedNav == 0,
                        onClick = { selectedNav = 0 },
                        icon = { Icon(if (selectedNav == 0) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = null) },
                        label = { Text("Home", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color(0xFF888888),
                            unselectedTextColor = Color(0xFF888888),
                            indicatorColor = Color(0xFF1A2A1A)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedNav == 1,
                        onClick = { selectedNav = 1 },
                        icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        label = { Text("Manual", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color(0xFF888888),
                            unselectedTextColor = Color(0xFF888888),
                            indicatorColor = Color(0xFF1A2A1A)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedNav == 2,
                        onClick = { selectedNav = 2 },
                        icon = { Icon(if (selectedNav == 2) Icons.Filled.Star else Icons.Outlined.Star, contentDescription = null) },
                        label = { Text("Good Lyrics", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color(0xFF888888),
                            unselectedTextColor = Color(0xFF888888),
                            indicatorColor = Color(0xFF1A2A1A)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(bottom = if (searchActive || showSeeAll || isShowingLyrics) 0.dp else padding.calculateBottomPadding())) {
            when (selectedNav) {
                0 -> HomeScreen(
                    viewModel = viewModel,
                    sharedText = sharedText,
                    searchActive = searchActive,
                    onSearchActiveChange = { searchActive = it },
                    showSeeAll = showSeeAll,
                    onShowSeeAll = { showSeeAll = it },
                    onBack = { viewModel.reset() }
                )
                1 -> ManualSearchScreen(viewModel = viewModel, uiState = uiState)
                2 -> ComingSoonScreen()
            }
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    sharedText: String = "",
    searchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    showSeeAll: Boolean,
    onShowSeeAll: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    LaunchedEffect(sharedText) {
        if (sharedText.isNotBlank()) viewModel.processUrl(sharedText)
    }

    LaunchedEffect(searchActive) {
        if (searchActive) focusRequester.requestFocus()
        else { focusManager.clearFocus(); query = ""; viewModel.search("") }
    }

    // See All
    if (showSeeAll) {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)).statusBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onShowSeeAll(false) }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Text("Recently Searched", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
            }
            HorizontalDivider(color = Color(0xFF282828))
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(recentSearches.take(30)) { result ->
                    RecentSongCard(result = result, onClick = { onShowSeeAll(false); viewModel.fetchLyricsFromResult(result) })
                }
            }
        }
        return
    }

    // Search screen
    if (searchActive) {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)).statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { onSearchActiveChange(false) }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Row(
                    modifier = Modifier.weight(1f).height(44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFFB3B3B3), modifier = Modifier.size(20.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) Text("Songs, artists...", color = Color(0xFFB3B3B3), fontSize = 15.sp)
                        BasicTextField(
                            value = query,
                            onValueChange = {
                                query = it
                                // بحث بالنص أو اللينك
                                if (it.startsWith("http")) viewModel.processUrl(it)
                                else viewModel.search(it)
                            },
                            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                focusManager.clearFocus()
                                if (query.startsWith("http")) viewModel.processUrl(query)
                            }),
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                        )
                    }
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = ""; viewModel.search("") }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = null, tint = Color(0xFFB3B3B3), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            HorizontalDivider(color = Color(0xFF282828))

            // لو URL بيجلب الكلمات مباشرة
            if (query.startsWith("http")) {
                AnimatedContent(targetState = uiState, transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) }, label = "urlState") { state ->
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        when (state) {
                            is UiState.Loading -> LoadingCard()
                            is UiState.SongFound -> SongFoundCard(state.song)
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
                            else -> {}
                        }
                    }
                }
            } else if (isSearching) {
                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), color = MaterialTheme.colorScheme.primary)
                }
            } else if (query.isNotBlank() && searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Text("No results found", color = Color(0xFFB3B3B3))
                }
            } else if (searchResults.isNotEmpty()) {
                Text(
                    "${searchResults.size} SONGS",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp
                )
                LazyColumn {
                    items(searchResults) { result ->
                        SearchSongItem(result = result, onClick = {
                            onSearchActiveChange(false)
                            query = ""
                            viewModel.fetchLyricsFromResult(result)
                        })
                        HorizontalDivider(modifier = Modifier.padding(start = 80.dp), color = Color(0xFF1E1E1E))
                    }
                }
            }
        }
        return
    }

    // Results screen - بدون nav bar
    if (uiState is UiState.Loading || uiState is UiState.SongFound || uiState is UiState.Success || uiState is UiState.Error) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Best Lyrics", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    AnimatedContent(targetState = uiState, transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) }, label = "results") { state ->
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            when (state) {
                                is UiState.Loading -> LoadingCard()
                                is UiState.SongFound -> SongFoundCard(state.song)
                                is UiState.Error -> ErrorCard(state.message)
                                is UiState.Success -> {
                                    // Song info مع صورة الألبوم
                                    SongInfoCardWithImage(state.song)
                                    for (result in state.results) {
                                        LyricsCard(
                                            result = result,
                                            onCopy = { text -> copyToClipboard(context, text); Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show() },
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
        return
    }

    // Main Home
    LazyColumn(modifier = Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("Best Lyrics", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text("Find lyrics for", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text("any song", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Search by song title, artist\nor paste a link", fontSize = 14.sp, color = Color(0xFFB3B3B3))
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF1E1E1E))
                    .clickable { onSearchActiveChange(true) }
                    .padding(horizontal = 18.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFFB3B3B3), modifier = Modifier.size(18.dp))
                Text("Search lyrics...", color = Color(0xFFB3B3B3), fontSize = 14.sp)
            }
        }

        if (recentSearches.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recently searched", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Text("See all", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, modifier = Modifier.clickable { onShowSeeAll(true) })
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            items(recentSearches.take(6)) { result ->
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        RecentSongCard(
            result = result,
            onClick = { viewModel.fetchLyricsFromResult(result) }
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}
        }
    }
}


@Composable
fun RecentSongCard(
    result: SearchResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }, // ❗ مفيش padding هنا
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF181818)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // صورة الأغنية
            AsyncImage(
                model = result.artworkUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            // النصوص
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = result.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = result.artist,
                    fontSize = 13.sp,
                    color = Color(0xFFB3B3B3),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // السهم
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF888888),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SongInfoCardWithImage(song: SongInfo) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF181818))) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            // صورة الألبوم من الـ recently searched لو موجودة
            Box(
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF282828)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color(0xFF555555), modifier = Modifier.size(28.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Text(song.artist, fontSize = 13.sp, color = Color(0xFFB3B3B3))
                Spacer(modifier = Modifier.height(2.dp))
                Text("💿 ${song.album} • ⏱ ${song.duration}s", fontSize = 11.sp, color = Color(0xFF888888))
            }
        }
    }
}

@Composable
fun SearchSongItem(result: SearchResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(model = result.artworkUrl, contentDescription = null, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
        Column(modifier = Modifier.weight(1f)) {
            Text(result.title, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${result.artist} • ${formatDuration(result.duration)}", fontSize = 12.sp, color = Color(0xFFB3B3B3), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Filled.MoreVert, contentDescription = null, tint = Color(0xFFB3B3B3), modifier = Modifier.size(20.dp))
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60; val s = seconds % 60
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
        modifier = Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp).windowInsetsPadding(WindowInsets.ime),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text("Best Lyrics", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
        }
        Spacer(modifier = Modifier.height(8.dp))

        AnimatedVisibility(
            visible = uiState is UiState.Idle || uiState is UiState.Error,
            enter = fadeIn(tween(300)) + expandVertically(tween(300)),
            exit = fadeOut(tween(300)) + shrinkVertically(tween(300))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Manual Search", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                OutlinedTextField(value = titleText, onValueChange = { titleText = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Title") }, leadingIcon = { Icon(Icons.Filled.MusicNote, contentDescription = null) }, shape = RoundedCornerShape(12.dp), singleLine = true)
                OutlinedTextField(value = artistText, onValueChange = { artistText = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Artist") }, leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) }, shape = RoundedCornerShape(12.dp), singleLine = true)
                OutlinedTextField(value = albumText, onValueChange = { albumText = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Album") }, leadingIcon = { Icon(Icons.Filled.Album, contentDescription = null) }, shape = RoundedCornerShape(12.dp), singleLine = true)
                OutlinedTextField(value = durationText, onValueChange = { durationText = it.filter { c -> c.isDigit() } }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Duration (seconds)") }, leadingIcon = { Icon(Icons.Filled.Timer, contentDescription = null) }, shape = RoundedCornerShape(12.dp), singleLine = true)
                Button(
                    onClick = { val dur = durationText.toIntOrNull() ?: 0; viewModel.processManual(titleText, artistText, albumText, dur) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    enabled = titleText.isNotBlank() && artistText.isNotBlank() && albumText.isNotBlank() && durationText.isNotBlank(),
                    shape = RoundedCornerShape(50)
                ) { Text("Go", fontWeight = FontWeight.Bold) }
            }
        }

        AnimatedContent(targetState = uiState, transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) }, label = "manualState") { state ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (state) {
                    is UiState.Loading -> LoadingCard()
                    is UiState.Error -> ErrorCard(state.message)
                    is UiState.Success -> {
                        SongInfoCardWithImage(state.song)
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
            Text("Coming Soon", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White)
            Text("More lyrics sources will be added here", fontSize = 14.sp, color = Color(0xFFB3B3B3), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun LoadingCard() {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF181818))) {
        Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text("Fetching lyrics...", fontSize = 14.sp, color = Color(0xFFB3B3B3))
        }
    }
}

@Composable
fun SongFoundCard(song: SongInfo) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF181818))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
            Column {
                Text(song.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                Text("${song.artist} • ${song.duration}s", fontSize = 12.sp, color = Color(0xFFB3B3B3))
            }
        }
    }
}

@Composable
fun SongInfoCard(song: SongInfo) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF181818))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(song.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            Text(song.artist, fontSize = 13.sp, color = Color(0xFFB3B3B3))
            Text("💿 ${song.album} • ⏱ ${song.duration}s", fontSize = 12.sp, color = Color(0xFF888888))
        }
    }
}

@Composable
fun ErrorCard(message: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF3D0B0F))) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFE91429))
            Text(message, color = Color(0xFFFFB3B8))
        }
    }
}

@Composable
fun LyricsCard(result: LyricsResult, onCopy: (String) -> Unit, onShare: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedFormat by remember { mutableIntStateOf(0) }

    val formats = if (result.type == LyricsType.WORD) listOf("Karaoke", "Karaoke 2", "Synced", "Plain") else listOf("Synced", "Plain")

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

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF181818))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(result.source, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("${formattedLyrics.lines().size} Lines", fontSize = 11.sp, color = Color(0xFFB3B3B3))
                }
                Row {
                    IconButton(onClick = { onCopy(formattedLyrics) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFB3B3B3)) }
                    IconButton(onClick = { onShare(formattedLyrics) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFB3B3B3)) }
                    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(36.dp)) {
                        Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFB3B3B3))
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF282828))

            AnimatedContent(targetState = expanded to selectedFormat, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }, label = "lyrics") { (isExpanded, _) ->
                Text(text = if (isExpanded) formattedLyrics else previewText, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 20.sp, color = Color.White))
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                formats.forEachIndexed { index, label ->
                    if (selectedFormat == index) {
                        Button(onClick = { selectedFormat = index }, shape = RoundedCornerShape(50), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp), modifier = Modifier.height(32.dp)) {
                            Text(label, fontSize = 12.sp)
                        }
                    } else {
                        OutlinedButton(onClick = { selectedFormat = index }, shape = RoundedCornerShape(50), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp), modifier = Modifier.height(32.dp)) {
                            Text(label, fontSize = 12.sp)
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
