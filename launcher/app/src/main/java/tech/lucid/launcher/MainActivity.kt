package tech.lucid.launcher

import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF09080D)
private val Panel = Color(0xB8171420)
private val PanelRaised = Color(0xC8201B2A)
private val Plum = Color(0xFFC4A7F5)
private val Lilac = Color(0xFFE0D2FF)
private val TextMain = Color(0xFFFAF7FF)
private val TextMuted = Color(0xFFD0C7D8)

data class LaunchableApp(val label: String, val packageName: String, val icon: android.graphics.drawable.Drawable)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility = 0
        setContent { LucidTheme { LauncherRoot(::loadApps, ::launchApp) } }
    }

    private fun loadApps(): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                LaunchableApp(info.loadLabel(packageManager).toString(), pkg, info.loadIcon(packageManager))
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    private fun launchApp(packageName: String) {
        packageManager.getLaunchIntentForPackage(packageName)?.let { startActivity(it) }
    }
}

@Composable
private fun LucidTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Plum, onPrimary = Ink, background = Ink,
        onBackground = TextMain, surface = Panel, onSurface = TextMain, secondary = Lilac), content = content)
}

@Composable
private fun LauncherRoot(loadApps: () -> List<LaunchableApp>, launchApp: (String) -> Unit) {
    var drawer by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf(emptyList<LaunchableApp>()) }
    val context = LocalContext.current
    var wallpaper by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        wallpaper = withContext(Dispatchers.IO) {
            runCatching {
                val drawable = WallpaperManager.getInstance(context).drawable
                val w = 540
                val h = 1200
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, w, h)
                drawable.draw(canvas)
                bitmap.asImageBitmap()
            }.getOrNull()
        }
        apps = withContext(Dispatchers.IO) { loadApps() }
    }
    BackHandler(drawer) { drawer = false; query = "" }

    Box(Modifier.fillMaxSize().background(Ink)) {
        wallpaper?.let {
            Image(bitmap = it, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Color(0x4D08060D)))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x550A0711), Color(0x220A0711), Color(0x8809080D)))))
        }
        if (drawer) {
            AppDrawer(apps.filter { it.label.contains(query, true) }, query,
                onQuery = { query = it }, onLaunch = launchApp, onClose = { drawer = false; query = "" })
        } else {
            HomeScreen(apps, onOpenDrawer = { drawer = true }, onLaunch = launchApp)
        }
    }
}

@Composable
private fun HomeScreen(apps: List<LaunchableApp>, onOpenDrawer: () -> Unit, onLaunch: (String) -> Unit) {
    var clock by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            clock = Date()
            kotlinx.coroutines.delay(30_000)
        }
    }
    val time = remember(clock) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(clock) }
    val date = remember(clock) { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(clock) }
    val favourites = remember(apps) {
        val preferred = listOf("phone", "messages", "chrome", "camera", "settings", "music")
        val selected = preferred.mapNotNull { key -> apps.firstOrNull { it.label.contains(key, true) } }
            .distinctBy { it.packageName }
        (selected + apps.filterNot { app -> selected.any { it.packageName == app.packageName } }).take(4)
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(time, color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Text(date, color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Box(Modifier.size(38.dp).clip(CircleShape).background(PanelRaised), contentAlignment = Alignment.Center) {
                Text("L", color = Lilac, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(26.dp))
        // Keep the wallpaper in charge: one compact glance card instead of a wall of copy.
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Panel).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(clock).uppercase(Locale.getDefault()),
                    color = Lilac, fontSize = 10.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(7.dp))
                Text("Your day, at a glance.", color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            }
            Text(SimpleDateFormat("d", Locale.getDefault()).format(clock), color = Lilac, fontSize = 38.sp, fontWeight = FontWeight.Light)
        }

        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Favourites", color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text("ALL APPS", color = Lilac, fontSize = 10.sp, letterSpacing = 1.4.sp,
                modifier = Modifier.clip(RoundedCornerShape(18.dp)).clickable { onOpenDrawer() }.padding(9.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            favourites.forEach { app ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).clickable { onLaunch(app.packageName) }
                    .padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AppIcon(app, 52)
                    Spacer(Modifier.height(6.dp))
                    Text(app.label, color = TextMain, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp).height(58.dp).clip(RoundedCornerShape(23.dp))
            .background(PanelRaised).padding(horizontal = 17.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).fillMaxHeight().clickable { onOpenDrawer() },
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Search, null, tint = Lilac, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text("Search", color = TextMuted, fontSize = 14.sp)
            }
            Box(Modifier.size(40.dp).clip(CircleShape).background(Plum).clickable { onOpenDrawer() },
                contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Apps, null, tint = Ink)
            }
        }
    }
}

@Composable
private fun AppDrawer(
    apps: List<LaunchableApp>, query: String, onQuery: (String) -> Unit,
    onLaunch: (String) -> Unit, onClose: () -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Apps", color = TextMain, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp)
            Text("DONE", color = Lilac, fontSize = 10.sp, letterSpacing = 1.5.sp,
                modifier = Modifier.clip(RoundedCornerShape(18.dp)).clickable { onClose() }.padding(12.dp))
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(PanelRaised)
            .padding(horizontal = 15.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, null, tint = Lilac, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(10.dp))
            BasicTextField(value = query, onValueChange = onQuery, singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = TextMain, fontSize = 14.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Lilac),
                decorationBox = { inner -> Box { if (query.isEmpty()) Text("Search apps", color = TextMuted, fontSize = 14.sp); inner() } },
                modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(14.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(4), contentPadding = PaddingValues(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(apps, key = { it.packageName }) { app ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .clickable { onLaunch(app.packageName) }.padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    AppIcon(app, 46)
                    Spacer(Modifier.height(5.dp))
                    Text(app.label, color = TextMain, fontSize = 10.sp, maxLines = 2,
                        overflow = TextOverflow.Ellipsis, lineHeight = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun AppIcon(app: LaunchableApp, size: Int) {
    val bitmap = remember(app.icon) {
        val image = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        app.icon.setBounds(0, 0, canvas.width, canvas.height)
        app.icon.draw(canvas)
        image.asImageBitmap()
    }
    Image(bitmap = bitmap, contentDescription = app.label,
        modifier = Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.25f).dp)))
}
