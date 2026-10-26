package com.cosmos.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
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
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFFF8F7FF)
private val GlassTop = Color(0x48FFFFFF)
private val GlassBottom = Color(0x16FFFFFF)
private val Hairline = Color(0x65FFFFFF)
private val Accent = Color(0xFFB7D7FF)

data class AppEntry(val label: String, val packageName: String, val icon: Bitmap)

class CosmosModel : ViewModel() {
    var apps by mutableStateOf<List<AppEntry>>(emptyList())
        private set
    var query by mutableStateOf("")
    var loading by mutableStateOf(true)

    fun load(context: Context) {
        if (apps.isNotEmpty() || !loading) return
        loading = true
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
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
                    AppEntry(info.loadLabel(pm).toString(), activity.packageName, drawable.toBitmapSafe(96))
                } catch (_: Exception) { null }
            }.distinctBy { it.packageName }.sortedBy { it.label.lowercase(Locale.getDefault()) }
            withContext(Dispatchers.Main) {
                apps = built
                loading = false
            }
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

@Composable
private fun CosmosHome(model: CosmosModel = viewModel()) {
    val context = LocalContext.current
    var clock by remember { mutableStateOf(nowTime()) }
    var date by remember { mutableStateOf(nowDate()) }
    var drawerOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        model.load(context.applicationContext)
        while (true) {
            clock = nowTime()
            date = nowDate()
            delay(30_000L)
        }
    }

    val filtered by remember(model.apps, model.query) {
        derivedStateOf {
            val q = model.query.trim()
            if (q.isEmpty()) model.apps else model.apps.filter {
                it.label.contains(q, ignoreCase = true)
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
            .background(Brush.verticalGradient(listOf(Color(0x140D1830), Color(0x06000000), Color(0x35000000))))
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
                    modifier = Modifier.size(54.dp).clickable { drawerOpen = !drawerOpen },
                    shape = RoundedCornerShape(19.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (drawerOpen) "⌂" else "⌕", color = Ink, fontSize = 27.sp)
                    }
                }
            }

            if (!drawerOpen) {
                GlassSurface(Modifier.fillMaxWidth().height(86.dp), shape = RoundedCornerShape(26.dp)) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Your space", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text("A little room to breathe.", color = Ink.copy(alpha = .72f), fontSize = 12.sp)
                        }
                        Box(Modifier.size(44.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xA9D8E8FF), Color(0x338C9DFF))))
                            .border(1.dp, Hairline, CircleShape), contentAlignment = Alignment.Center) {
                            Text("✦", color = Ink, fontSize = 22.sp)
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
                if (model.loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Preparing your space…", color = Ink.copy(alpha = .8f))
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 76.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filtered, key = { it.packageName }, contentType = { "app" }) { app ->
                            AppIcon(app, context, showLabel = true, modifier = Modifier.animateItem())
                        }
                    }
                }
            }
        }
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
private fun AppIcon(app: AppEntry, context: Context, showLabel: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(20.dp))
            .pointerInput(app.packageName) {
                detectTapGestures(onTap = {
                    val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(intent) }
                    }
                })
            }
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

private fun WindowInsets.statusBars.asPaddingValuesCompat(): PaddingValues =
    PaddingValues(top = 0.dp, bottom = 0.dp)
