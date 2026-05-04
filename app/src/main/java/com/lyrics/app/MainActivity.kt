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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
                    color = Color.Black // استخدام اللون الأسود كما في الصور
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

    BackHandler(enabled = uiState !is UiState.Idle) {
        viewModel.reset()
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            if (uiState is UiState.Idle) {
                // Header الخاص بالصفحة الرئيسية كما في الصورة الثانية
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "LyriX",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                // شريط البحث العلوي عند ظهور النتائج كما في الصورة الأولى
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.reset() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Clear search */ }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                )
            }
        },
        bottomBar = {
            // الجزء السفلي تم تركه كما طلبت
            NavigationBar(containerColor = Color(0xFF0A0A0A)) {
                NavigationBarItem(
                    selected = selectedNav == 0,
                    onClick = { selectedNav = 0 },
                    icon = { Icon(if (selectedNav == 0) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = null) },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF4CAF50), selectedTextColor = Color(0xFF4CAF50), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
                )
                NavigationBarItem(
                    selected = selectedNav == 1,
                    onClick = { selectedNav = 1 },
                    icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    label = { Text("Manual") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF4CAF50), selectedTextColor = Color(0xFF4CAF50), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
                )
                NavigationBarItem(
                    selected = selectedNav == 2,
                    onClick = { selectedNav = 2 },
                    icon = { Icon(if (selectedNav == 2) Icons.Filled.Star else Icons.Outlined.Star, contentDescription = null) },
                    label = { Text("Good Lyrics") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF4CAF50), selectedTextColor = Color(0xFF4CAF50), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(Color.Black)) {
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
    val context = LocalContext.current

    LaunchedEffect(sharedText) {
        if (sharedText.isNotBlank()) viewModel.processUrl(sharedText)
    }

    if (uiState !is UiState.Idle) {
        // واجهة نتائج البحث (الصورة الأولى)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                AnimatedContent(targetState = uiState, label = "results") { state ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        when (state) {
                            is UiState.Loading -> Box(Modifier.fillMaxWidth().padding(50.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF4CAF50)) }
                            is UiState.Success -> {
                                state.results.forEach { result ->
                                    LyricsCard(
                                        result = result,
                                        onCopy = { text -> copyToClipboard(context, text); Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show() },
                                        onShare = { text -> shareText(context, text, state.song) }
                                    )
                                }
                            }
                            is UiState.SongFound -> SongFoundCard(state.song)
                            is UiState.Error -> ErrorCard(state.message)
                            else -> {}
                        }
                    }
                }
            }
            if (uiState is UiState.Idle || searchResults.isNotEmpty()) {
                items(searchResults) { result ->
                    SongListItem(result = result, showMenu = true, onClick = { viewModel.fetchLyricsFromResult(result) })
                }
            }
        }
    } else {
        // الواجهة الرئيسية (الصورة الثانية)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                    Text(
                        text = buildAnnotatedString {
                            append("Find lyrics for\n")
                            withStyle(style = SpanStyle(color = Color(0xFF4CAF50))) {
                                append("any song")
                            }
                        },
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 42.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Search by song title, artist\nor paste a link",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    // Search Bar المخصص كما في الصورة
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it; viewModel.search(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search lyrics...", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White) },
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            containerColor = Color(0xFF1A1A1A),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(40.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Popular searches", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("See all", color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.clickable { })
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            items(topCharts) { result ->
                SongListItem(
                    result = result,
                    showMenu = false,
                    onClick = { viewModel.fetchLyricsFromResult(result) }
                )
            }
        }
    }
}

@Composable
fun SongListItem(result: SearchResult, showMenu: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = result.artworkUrl,
            contentDescription = null,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                result.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${result.artist} • ${result.duration}", // تم إضافة المدة كما في الصورة الأولى
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (showMenu) {
            Icon(Icons.Filled.MoreVert, contentDescription = null, tint = Color.Gray)
        } else {
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

// ... بقية الـ Composables (ManualSearchScreen, LyricsCard, الخ) مع تعديل الألوان لتناسب التصميم المظلم

@Composable
fun ManualSearchScreen(viewModel: MainViewModel, uiState: UiState) {
    val context = LocalContext.current
    var titleText by remember { mutableStateOf("") }
    var artistText by remember { mutableStateOf("") }
    var albumText by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Manual Search", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        
        val textFieldColors = TextFieldDefaults.outlinedTextFieldColors(
            containerColor = Color(0xFF1A1A1A),
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = Color(0xFF4CAF50),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )

        OutlinedTextField(value = titleText, onValueChange = { titleText = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Title") }, shape = RoundedCornerShape(12.dp), colors = textFieldColors)
        OutlinedTextField(value = artistText, onValueChange = { artistText = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Artist") }, shape = RoundedCornerShape(12.dp), colors = textFieldColors)
        OutlinedTextField(value = durationText, onValueChange = { durationText = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Duration (seconds)") }, shape = RoundedCornerShape(12.dp), colors = textFieldColors)
        
        Button(
            onClick = { viewModel.processManual(titleText, artistText, albumText, durationText.toIntOrNull() ?: 0) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(27.dp)
        ) {
            Text("Search Lyrics", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun LyricsCard(result: LyricsResult, onCopy: (String) -> Unit, onShare: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(result.source, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = { onCopy(result.lyrics) }) { Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = Color.White) }
                    IconButton(onClick = { expanded = !expanded }) { Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = Color.White) }
                }
            }
            Text(
                text = if (expanded) result.lyrics else result.lyrics.lines().take(3).joinToString("\n"),
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun SongFoundCard(song: SongInfo) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF4CAF50), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Finding lyrics for ${song.title}...", color = Color.White)
        }
    }
}

@Composable
fun ErrorCard(message: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF331111))) {
        Text(message, color = Color.Red, modifier = Modifier.padding(16.dp))
    }
}

@Composable
fun ComingSoonScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Coming Soon", color = Color.White, style = MaterialTheme.typography.headlineMedium)
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("lyrics", text))
}

private fun shareText(context: Context, lyrics: String, song: SongInfo) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "${song.title} - ${song.artist}\n\n$lyrics")
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Lyrics"))
}
