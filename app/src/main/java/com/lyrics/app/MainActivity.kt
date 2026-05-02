package com.lyrics.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lyrics.app.model.LyricsResult
import com.lyrics.app.model.SongInfo
import com.lyrics.app.model.UiState
import com.lyrics.app.ui.theme.LyricsAppTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
    var selectedNav by remember { mutableIntStateOf(0) }

    Scaffold(
        
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedNav == 0,
                    onClick = { selectedNav = 0 },
                    icon = { Icon(Icons.Filled.MusicNote, contentDescription = null) },
                    label = { Text("Best Lyrics") }
                )
                NavigationBarItem(
                    selected = selectedNav == 1,
                    onClick = { selectedNav = 1 },
                    icon = { Icon(if (selectedNav == 1) Icons.Filled.Star else Icons.Outlined.Star, contentDescription = null) },
                    label = { Text("Good Lyrics") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedNav) {
                0 -> BestLyricsScreen(viewModel = viewModel, sharedText = sharedText)
                1 -> ComingSoonScreen()
            }
        }
    }
}

@Composable
fun BestLyricsScreen(viewModel: MainViewModel, sharedText: String = "") {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(sharedText) {
        if (sharedText.isNotBlank()) {
            viewModel.processUrl(sharedText)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = {
                    selectedTab = 0
                    viewModel.reset()
                },
                text = { Text("Auto Search") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = {
                    selectedTab = 1
                    viewModel.reset()
                },
                text = { Text("Manual Search") }
            )
        }

        when (selectedTab) {
            0 -> AutoSearchScreen(viewModel = viewModel, uiState = uiState)
            1 -> ManualSearchScreen(viewModel = viewModel, uiState = uiState)
        }
    }
}

@Composable
fun AutoSearchScreen(viewModel: MainViewModel, uiState: UiState) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var urlText by remember { mutableStateOf("") }
    var titleText by remember { mutableStateOf("") }
    var artistText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // URL Section
        AnimatedVisibility(
            visible = uiState is UiState.Idle || uiState is UiState.Error,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "You can use YT music, Spotify, Apple music link",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Put the URL") },
                    leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Button(
                    onClick = { viewModel.processUrl(urlText) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    enabled = urlText.isNotBlank(),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Search")
                }

                HorizontalDivider()

                Text(
                    "You can also search by title and artist name",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Title") },
                    leadingIcon = { Icon(Icons.Filled.MusicNote, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = artistText,
                    onValueChange = { artistText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Artist") },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Button(
                    onClick = { viewModel.processTitleArtist(titleText, artistText) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    enabled = titleText.isNotBlank() && artistText.isNotBlank(),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Search")
                }
            }
        }

        // Results
        when (val state = uiState) {
            is UiState.Loading -> LoadingCard()
            is UiState.SongFound -> SongFoundCard(state.song)
            is UiState.Error -> {
                ErrorCard(state.message)
                Button(
                    onClick = { viewModel.reset() },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Try Again")
                }
            }
            is UiState.Success -> {
                SongInfoCard(state.song)
                for (result in state.results) {
                    LyricsCard(
                        result = result,
                        song = state.song,
                        onCopy = {
                            copyToClipboard(context, result.lyrics)
                            Toast.makeText(context, "تم النسخ!", Toast.LENGTH_SHORT).show()
                        },
                        onShare = { shareText(context, result.lyrics, state.song) }
                    )
                }
                Button(
                    onClick = { viewModel.reset() },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Search")
                }
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ManualSearchScreen(viewModel: MainViewModel, uiState: UiState) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var titleText by remember { mutableStateOf("") }
    var artistText by remember { mutableStateOf("") }
    var albumText by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        AnimatedVisibility(
            visible = uiState is UiState.Idle || uiState is UiState.Error,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Title") },
                    leadingIcon = { Icon(Icons.Filled.MusicNote, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = artistText,
                    onValueChange = { artistText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Artist") },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = albumText,
                    onValueChange = { albumText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Album") },
                    leadingIcon = { Icon(Icons.Filled.Album, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Duration (seconds)") },
                    leadingIcon = { Icon(Icons.Filled.Timer, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
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

        when (val state = uiState) {
            is UiState.Loading -> LoadingCard()
            is UiState.Error -> {
                ErrorCard(state.message)
                Button(
                    onClick = { viewModel.reset() },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Try Again")
                }
            }
            is UiState.Success -> {
                SongInfoCard(state.song)
                for (result in state.results) {
                    LyricsCard(
                        result = result,
                        song = state.song,
                        onCopy = {
                            copyToClipboard(context, result.lyrics)
                            Toast.makeText(context, "تم النسخ!", Toast.LENGTH_SHORT).show()
                        },
                        onShare = { shareText(context, result.lyrics, state.song) }
                    )
                }
                Button(
                    onClick = { viewModel.reset() },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Search")
                }
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ComingSoonScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "Coming Soon",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "More lyrics sources will be added here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LoadingCard() {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text("جاري جلب الكلمات...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SongFoundCard(song: SongInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
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
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(song.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(song.artist, style = MaterialTheme.typography.bodyMedium)
                Text("💿 ${song.album} • ⏱ ${song.duration}s", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
fun LyricsCard(result: LyricsResult, song: SongInfo, onCopy: () -> Unit, onShare: () -> Unit) {
    var expanded by remember { mutableStateOf(true) }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(result.source, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Text("${result.lyrics.lines().size} سطر", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                    IconButton(onClick = onCopy) { Icon(Icons.Outlined.ContentCopy, contentDescription = "نسخ") }
                    IconButton(onClick = onShare) { Icon(Icons.Outlined.Share, contentDescription = "مشاركة") }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
                    }
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Text(text = result.lyrics, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, lineHeight = 20.sp))
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
    context.startActivity(Intent.createChooser(shareIntent, "مشاركة الكلمات"))
}
