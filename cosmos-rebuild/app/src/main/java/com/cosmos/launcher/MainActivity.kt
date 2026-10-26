package com.cosmos.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFFF8F7FF)
private val GlassTop = Color(0x48FFFFFF)
private val GlassBottom = Color(0x16FFFFFF)
private val Hairline = Color(0x65FFFFFF)
private val Accent = Color(0xFFC38FFF)
private val DeepPlum = Color(0xFF2A2633)
private val SoftPlum = Color(0xFF3A3445)

data class AppEntry(val label: String, val packageName: String, val icon: Bitmap, val category: String = "Apps")

class CosmosModel : ViewModel() {
    var apps by mutableStateOf<List<AppEntry>>(emptyList())
        private set
    var query by mutableStateOf("")
    var loading by mutableStateOf(true)
    var weather by mutableStateOf<Pair<String, String>?>(null)

    fun load(context: Context) {
        if (apps.isNotEmpty() || !loading) return
        loading = true
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolved = try {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            } catch (_: Exception) { emptyList() }
            val built = resolved.mapNotNull { info ->
                try {
                    val activity = info.activityInfo ?: return@mapNotNull null
                    if (activity.packageName == context.packageName) return@mapNotNull null
                    val drawable = info.loadIcon(pm)
                    val categoryId = activity.applicationInfo.category
                val category = when (categoryId) {
                    android.content.pm.ApplicationInfo.CATEGORY_GAME -> "Games"
                    android.content.pm.ApplicationInfo.CATEGORY_AUDIO,
                    android.content.pm.ApplicationInfo.CATEGORY_VIDEO,
                    android.content.pm.ApplicationInfo.CATEGORY_IMAGE -> "Media"
                    android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> "Social"
                    android.content.pm.ApplicationInfo.CATEGORY_NEWS -> "News"
                    android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Work"
                    else -> "Apps"
                }
                AppEntry(info.loadLabel(pm).toString(), activity.packageName, drawable.toBitmapSafe(96), category)
                } catch (_: Exception) { null }
            }.distinctBy { it.packageName }.sortedBy { it.label.lowercase(Locale.getDefault()) }
            withContext(Dispatchers.Main) {
                apps = built
                loading = false
            }
            val freshWeather = fetchCosmosWeather()
            withContext(Dispatchers.Main) { weather = freshWeather }
        }
    }
}

private fun Drawable.toBitmapSafe(size: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val oldBounds = bounds
    setBounds(0, 0, size, size)
    draw(canvas)
    bounds = oldBounds
    return bitmap
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.setFormat(android.graphics.PixelFormat.TRANSLUCENT)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() or
            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        setContent {
            MaterialTheme {
                CosmosHome()
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun CosmosHome(model: CosmosModel = viewModel()) {
    val context = LocalContext.current
    var clock by remember { mutableStateOf(nowTime()) }
    var date by remember { mutableStateOf(nowDate()) }
    var drawerOpen by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var showSettings by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<AppEntry?>(null) }
    var showHiddenManager by remember { mutableStateOf(false) }
    var gridColumns by remember { mutableStateOf(context.getSharedPreferences("cosmos_launcher", Context.MODE_PRIVATE).getInt("grid_columns", 4)) }
    val hiddenPackages = remember { mutableStateOf(context.getSharedPreferences("cosmos_launcher", Context.MODE_PRIVATE).getStringSet("hidden_apps", emptySet())?.toSet() ?: emptySet()) }
    val pinnedPackages = remember { mutableStateOf(context.getSharedPreferences("cosmos_launcher", Context.MODE_PRIVATE).getStringSet("pinned_apps", emptySet())?.toSet() ?: emptySet()) }
    val visibleApps = remember(model.apps, hiddenPackages.value) { model.apps.filter { it.packageName !in hiddenPackages.value } }
    val pinnedApps = remember(visibleApps, pinnedPackages.value) { visibleApps.filter { it.packageName in pinnedPackages.value } }
    BackHandler(enabled = drawerOpen || showSettings || showHiddenManager || selectedApp != null) { if (showHiddenManager) showHiddenManager = false else if (selectedApp != null) selectedApp = null else if (showSettings) showSettings = false else { drawerOpen = false; model.query = "" } }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        model.load(context.applicationContext)
        while (true) {
            clock = nowTime()
            date = nowDate()
            delay(30_000L)
        }
    }

    val filtered by remember(model.apps, model.query, selectedCategory) {
        derivedStateOf {
            val q = model.query.trim()
            visibleApps.filter { app ->
                (selectedCategory == "All" || app.category == selectedCategory) &&
                    (q.isEmpty() || app.label.contains(q, ignoreCase = true))
            }
        }
    }
    val dock = remember(model.apps) {
        val preferred = listOf("com.google.android.dialer", "com.google.android.apps.messaging",
            "com.android.chrome", "com.google.android.apps.photos")
        preferred.mapNotNull { pkg -> model.apps.firstOrNull { it.packageName == pkg } }
            .ifEmpty { model.apps.take(4) }.take(4)
    }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0x182A1838), Color(0x06000000), Color(0x3A100E17))))
            .padding(WindowInsets.statusBars.asPaddingValuesCompat())
            .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 8.dp)
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(date, color = Ink.copy(alpha = .86f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    AnimatedContent(clock, label = "clock") { value ->
                        Text(value, color = Ink, fontSize = 52.sp, lineHeight = 56.sp,
                            fontWeight = FontWeight.Light, letterSpacing = (-2).sp)
                    }
                }
                GlassSurface(
                    modifier = Modifier.size(54.dp).clickable { if (drawerOpen) drawerOpen = false else showSettings = true },
                    shape = RoundedCornerShape(19.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("⋮", color = Ink, fontSize = 27.sp)
                    }
                }
            }

            AnimatedContent(
                targetState = drawerOpen,
                transitionSpec = {
                    (fadeIn(tween(170)) + scaleIn(initialScale = 0.975f)) togetherWith
                        (fadeOut(tween(120)) + scaleOut(targetScale = 0.985f))
                },
                label = "home-drawer-transition"
            ) { showDrawer ->
            if (!showDrawer) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassSurface(
                        Modifier.weight(1f).height(112.dp),
                        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 22.dp, bottomEnd = 30.dp, bottomStart = 30.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                            Text("WEATHER", color = Ink.copy(alpha = .68f), fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(2.dp))
                            Text(model.weather?.first ?: "—°C", color = Ink, fontSize = 31.sp, fontWeight = FontWeight.Light)
                            Text(model.weather?.second ?: "Fetching conditions", color = Ink.copy(alpha = .78f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    GlassSurface(
                        Modifier.weight(1f).height(112.dp),
                        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 30.dp, bottomEnd = 30.dp, bottomStart = 30.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                            Text("COSMOS", color = Accent, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(clock, color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Light)
                            Text(date, color = Ink.copy(alpha = .75f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                if (pinnedApps.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("PINNED", color = Ink.copy(alpha = .68f), fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pinnedApps.take(5).forEach { app ->
                                AppIcon(app, context, showLabel = false, modifier = Modifier.weight(1f), onLongPress = { selectedApp = it })
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                GlassSurface(Modifier.fillMaxWidth().height(74.dp), shape = RoundedCornerShape(28.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp)) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically) {
                        dock.forEach { app -> AppIcon(app, context, showLabel = false, modifier = Modifier.weight(1f)) }
                        if (dock.isEmpty()) Text("Open apps", color = Ink.copy(alpha = .75f), fontSize = 13.sp)
                    }
                }
                GlassSurface(Modifier.fillMaxWidth().height(54.dp).clickable { drawerOpen = true },
                    shape = RoundedCornerShape(22.dp), contentPadding = PaddingValues(horizontal = 18.dp)) {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        Text("⌕", color = Ink.copy(alpha = .9f), fontSize = 23.sp)
                        Spacer(Modifier.width(10.dp))
                        Text("Search apps", color = Ink.copy(alpha = .78f), fontSize = 15.sp)
                        Spacer(Modifier.weight(1f))
                        Text("↑", color = Ink.copy(alpha = .7f), fontSize = 16.sp)
                    }
                }
            } else {
                GlassSurface(Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(22.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)) {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        Text("⌕", color = Ink, fontSize = 22.sp)
                        Spacer(Modifier.width(10.dp))
                        BasicTextField(
                            value = model.query, onValueChange = { model.query = it },
                            singleLine = true, textStyle = TextStyle(color = Ink, fontSize = 15.sp),
                            cursorBrush = Brush.verticalGradient(listOf(Accent, Ink)),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                Box {
                                    if (model.query.isEmpty()) Text("Search apps…", color = Ink.copy(alpha = .65f), fontSize = 15.sp)
                                    inner()
                                }
                            }
                        )
                        Text("×", Modifier.clickable { model.query = ""; drawerOpen = false },
                            color = Ink, fontSize = 22.sp)
                    }
                }
                val categories = remember(visibleApps) {
                    listOf("All") + visibleApps.map { it.category }.distinct().sorted()
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val selected = selectedCategory == category
                        Box(
                            Modifier.clip(RoundedCornerShape(16.dp))
                                .background(if (selected) Accent else SoftPlum.copy(alpha = .82f))
                                .border(1.dp, if (selected) Color.White.copy(alpha = .42f) else Hairline.copy(alpha = .45f), RoundedCornerShape(16.dp))
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 15.dp, vertical = 9.dp)
                        ) {
                            Text(category, color = if (selected) DeepPlum else Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (model.loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Preparing your space…", color = Ink.copy(alpha = .8f))
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns.coerceIn(3, 6)),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filtered, key = { it.packageName }, contentType = { "app" }) { app ->
                            AppIcon(app, context, showLabel = true, modifier = Modifier.animateItem(), onLongPress = { selectedApp = it })
                        }
                    }
                }
            }
            }
        }
    }

    androidx.compose.animation.AnimatedVisibility(visible = showSettings, enter = androidx.compose.animation.slideInHorizontally { it } + fadeIn(), exit = androidx.compose.animation.slideOutHorizontally { it } + fadeOut()) {
        Box(Modifier.fillMaxSize().background(DeepPlum).padding(WindowInsets.statusBars.asPaddingValuesCompat())) {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("←", color = Ink, fontSize = 26.sp, modifier = Modifier.clickable { showSettings = false }.padding(end = 16.dp))
                    Column { Text("Cosmos Settings", color = Ink, fontSize = 23.sp, fontWeight = FontWeight.SemiBold); Text("Personalize your launcher", color = Ink.copy(alpha = .65f), fontSize = 12.sp) }
                }
                Spacer(Modifier.height(22.dp))
                Text("PERSONALIZATION", color = Accent, fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                SettingsRow("Change wallpaper", "Open Android wallpaper picker") { runCatching { context.startActivity(Intent(Intent.ACTION_SET_WALLPAPER)) } }
                SettingsRow("Pinned apps", "${pinnedApps.size} apps pinned") { showSettings = false; drawerOpen = true }
                SettingsRow("App grid", "$gridColumns columns · tap to cycle density") {
                    gridColumns = if (gridColumns >= 6) 3 else gridColumns + 1
                    context.getSharedPreferences("cosmos_launcher", Context.MODE_PRIVATE).edit().putInt("grid_columns", gridColumns).apply()
                }
                SettingsRow("Hidden apps", "${hiddenPackages.value.size} hidden · manage visibility") { showHiddenManager = true }
                SettingsRow("Widgets", "Open Android widget picker") {
                    runCatching { context.startActivity(Intent("android.intent.action.APPWIDGET_PICK").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) } }
                }
                Spacer(Modifier.height(20.dp))
                Text("SYSTEM", color = Accent, fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                SettingsRow("Android settings", "Network, display, battery and more") { runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) } }
                SettingsRow("Cosmos Launcher", "Version 0.420 · fluid preview") {}
            }
        }
    }
    if (showHiddenManager) {
        AlertDialog(
            onDismissRequest = { showHiddenManager = false },
            containerColor = DeepPlum,
            title = { Text("App visibility", color = Ink) },
            text = {
                Column(Modifier.height(420.dp)) {
                    Text("Tap an app to hide or show it in Cosmos.", color = Ink.copy(alpha = .7f), fontSize = 12.sp)
                    androidx.compose.foundation.lazy.LazyColumn {
                        items(model.apps, key = { it.packageName }) { app ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    val updated = hiddenPackages.value.toMutableSet()
                                    if (!updated.add(app.packageName)) updated.remove(app.packageName)
                                    hiddenPackages.value = updated.toSet()
                                    context.getSharedPreferences("cosmos_launcher", Context.MODE_PRIVATE).edit().putStringSet("hidden_apps", updated).apply()
                                }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(app.icon.asImageBitmap(), contentDescription = null, modifier = Modifier.size(30.dp))
                                Text(app.label, color = Ink, modifier = Modifier.weight(1f).padding(start = 10.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(if (app.packageName in hiddenPackages.value) "HIDDEN" else "VISIBLE",
                                    color = if (app.packageName in hiddenPackages.value) Accent else Ink.copy(alpha = .55f), fontSize = 10.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showHiddenManager = false }) { Text("Done", color = Ink) } }
        )
    }
    selectedApp?.let { app ->
        AlertDialog(onDismissRequest = { selectedApp = null }, containerColor = DeepPlum, title = { Text(app.label, color = Ink) },
            text = { Column {
                Text(if (app.packageName in pinnedPackages.value) "Remove from pinned apps" else "Pin to home screen", color = Accent, modifier = Modifier.fillMaxWidth().clickable {
                    val updated = pinnedPackages.value.toMutableSet()
                    if (!updated.add(app.packageName)) updated.remove(app.packageName)
                    pinnedPackages.value = updated.toSet()
                    context.getSharedPreferences("cosmos_launcher", Context.MODE_PRIVATE).edit().putStringSet("pinned_apps", updated).apply()
                    selectedApp = null
                }.padding(vertical = 12.dp))
                Text("App info", color = Ink, modifier = Modifier.fillMaxWidth().clickable {
                    runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + app.packageName))) }
                    selectedApp = null
                }.padding(vertical = 12.dp))
            } }, confirmButton = { TextButton(onClick = { selectedApp = null }) { Text("Done", color = Ink) } })
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit) {
    GlassSurface(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Column { Text(title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium); Spacer(Modifier.height(3.dp)); Text(subtitle, color = Ink.copy(alpha = .64f), fontSize = 12.sp) }
    }
}

@Composable
private fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = PaddingValues(12.dp),
    content: @Composable () -> Unit
) {
    Box(
        modifier.shadow(8.dp, shape, clip = false)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(GlassTop, GlassBottom)))
            .border(1.dp, Brush.verticalGradient(listOf(Color(0xAFFFFFFF), Color(0x25FFFFFF))), shape)
            .background(Color(0x0CFFFFFF))
            .padding(contentPadding)
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
        contentAlignment = Alignment.CenterStart
    ) { content() }
}

@Composable
private fun AppIcon(app: AppEntry, context: Context, showLabel: Boolean, modifier: Modifier = Modifier, onLongPress: (AppEntry) -> Unit = {}) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(20.dp))
            .combinedClickable(onClick = {
                val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                if (intent != null) { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); runCatching { context.startActivity(intent) } }
            }, onLongClick = { onLongPress(app) })
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            Modifier.size(54.dp).shadow(5.dp, RoundedCornerShape(17.dp))
                .clip(RoundedCornerShape(17.dp))
                .background(Brush.verticalGradient(listOf(Color(0x42FFFFFF), Color(0x10FFFFFF))))
                .border(1.dp, Color(0x45FFFFFF), RoundedCornerShape(17.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(bitmap = app.icon.asImageBitmap(), contentDescription = app.label, modifier = Modifier.size(42.dp))
        }
        if (showLabel) Text(
            app.label, color = Ink.copy(alpha = .95f), fontSize = 11.sp, lineHeight = 13.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
        )
    }
}

private fun nowTime() = SimpleDateFormat("h:mm", Locale.getDefault()).format(Date())
private fun nowDate() = SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(Date())

private fun WindowInsets.asPaddingValuesCompat(): PaddingValues =
    PaddingValues(top = 0.dp, bottom = 0.dp)

private suspend fun fetchCosmosWeather(): Pair<String, String>? = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
        connection = (URL("https://api.open-meteo.com/v1/forecast?latitude=25.28&longitude=85.85&current_weather=true")
            .openConnection() as HttpURLConnection).apply {
            connectTimeout = 3500
            readTimeout = 3500
            requestMethod = "GET"
        }
        if (connection.responseCode !in 200..299) return@withContext null
        val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            .getJSONObject("current_weather")
        val temperature = "${json.getDouble("temperature").toInt()}°C"
        val condition = when (json.getInt("weathercode")) {
            0 -> "Clear sky"
            1, 2, 3 -> "Partly cloudy"
            45, 48 -> "Fog"
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> "Rain"
            71, 73, 75, 77, 85, 86 -> "Snow"
            95, 96, 99 -> "Thunderstorm"
            else -> "Current conditions"
        }
        temperature to condition
    } catch (_: Exception) {
        null
    } finally {
        connection?.disconnect()
    }
}
