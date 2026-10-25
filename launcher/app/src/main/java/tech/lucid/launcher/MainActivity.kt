package tech.lucid.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF0B0A10)
private val Panel = Color(0xFF17141F)
private val PanelRaised = Color(0xFF211C2B)
private val Plum = Color(0xFFB69AE8)
private val Lilac = Color(0xFFD9C9FF)
private val TextMain = Color(0xFFF5F0FC)
private val TextMuted = Color(0xFFA69AAF)

data class LaunchableApp(val label: String, val packageName: String, val icon: android.graphics.drawable.Drawable)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(11, 10, 16)
        window.navigationBarColor = android.graphics.Color.rgb(11, 10, 16)
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
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launch)
    }
}

@Composable
private fun LucidTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Plum, onPrimary = Ink, background = Ink, onBackground = TextMain,
            surface = Panel, onSurface = TextMain, secondary = Lilac
        ), content = content
    )
}

@Composable
private fun LauncherRoot(loadApps: () -> List<LaunchableApp>, launchApp: (String) -> Unit) {
    var drawer by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf(emptyList<LaunchableApp>()) }
    var refreshed by remember { mutableIntStateOf(0) }
    LaunchedEffect(refreshed) { apps = loadApps() }
    BackHandler(drawer) { drawer = false; query = "" }

    Box(Modifier.fillMaxSize().background(Ink)) {
        // DynaPunch-inspired LUCID atmosphere: deep ink, soft plum light, quiet surfaces.
        Box(Modifier.fillMaxWidth().height(380.dp).background(
            Brush.radialGradient(listOf(Color(0xFF39274C), Color(0xFF17121F), Ink), radius = 720f)
        ))
        if (drawer) {
            AppDrawer(apps.filter { it.label.contains(query, true) }, query,
                onQuery = { query = it }, onLaunch = launchApp, onClose = { drawer = false; query = "" })
        } else {
            HomeScreen(apps, onOpenDrawer = { drawer = true }, onLaunch = launchApp,
                onRefresh = { refreshed++ })
        }
    }
}

@Composable
private fun HomeScreen(
    apps: List<LaunchableApp>, onOpenDrawer: () -> Unit, onLaunch: (String) -> Unit, onRefresh: () -> Unit
) {
    val time = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }
    val date = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()) }
    val favourites = remember(apps) {
        val preferred = listOf("phone", "messages", "chrome", "camera", "settings", "music")
        preferred.mapNotNull { key -> apps.firstOrNull { it.label.contains(key, true) } }.distinctBy { it.packageName }.take(4)
            .ifEmpty { apps.take(4) }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(time, color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(date, color = TextMuted, fontSize = 12.sp)
            }
            Box(Modifier.size(38.dp).clip(CircleShape).background(PanelRaised), contentAlignment = Alignment.Center) {
                Text("L", color = Lilac, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(26.dp))
        Text("Good to see you.", color = TextMain, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.7).sp)
        Text("Make room for what matters.", color = TextMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f).height(176.dp).clip(RoundedCornerShape(28.dp)).background(
                Brush.linearGradient(listOf(Color(0xFF3B2A4C), Color(0xFF211B2A), Panel))
            ).padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Cloud, null, tint = Lilac, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(7.dp)); Text("WEATHER", color = Lilac, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Your day,", color = TextMuted, fontSize = 12.sp)
                    Text("unfolding.", color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    Text("Add a weather widget", color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
            Column(Modifier.weight(1f).height(176.dp).clip(RoundedCornerShape(28.dp)).background(Panel).padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CalendarMonth, null, tint = Lilac, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(7.dp)); Text("UP NEXT", color = Lilac, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("A little space", color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    Text("for your plans.", color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    Text("Calendar integration coming", color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Favourites", color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("ALL APPS  ↗", color = Lilac, fontSize = 10.sp, letterSpacing = 1.2.sp,
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable { onOpenDrawer() }.padding(8.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            favourites.forEach { app ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).clickable { onLaunch(app.packageName) }.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AppIcon(app, 52)
                    Spacer(Modifier.height(7.dp))
                    Text(app.label, color = TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (favourites.isEmpty()) {
                repeat(4) { Box(Modifier.weight(1f).height(76.dp).clip(RoundedCornerShape(20.dp)).background(Panel)) }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp).height(62.dp).clip(RoundedCornerShape(25.dp)).background(PanelRaised).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.weight(1f).clickable { onOpenDrawer() }, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Search, null, tint = Lilac, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text("Search your apps", color = TextMuted, fontSize = 14.sp)
            }
            Box(Modifier.size(42.dp).clip(CircleShape).background(Plum).clickable { onOpenDrawer() }, contentAlignment = Alignment.Center) {
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
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Your space.", color = TextMain, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                Text("${apps.size} applications", color = TextMuted, fontSize = 13.sp)
            }
            Text("DONE", color = Lilac, fontSize = 11.sp, letterSpacing = 1.5.sp,
                modifier = Modifier.clip(RoundedCornerShape(18.dp)).clickable { onClose() }.padding(12.dp))
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Panel).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, null, tint = Lilac)
            Spacer(Modifier.width(10.dp))
            BasicTextField(value = query, onValueChange = onQuery, singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = TextMain, fontSize = 15.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Lilac),
                decorationBox = { inner ->
                    Box { if (query.isEmpty()) Text("Find an app", color = TextMuted, fontSize = 15.sp); inner() }
                }, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(18.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(4), contentPadding = PaddingValues(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(apps, key = { it.packageName }) { app ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onLaunch(app.packageName) }.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    AppIcon(app, 48)
                    Spacer(Modifier.height(7.dp))
                    Text(app.label, color = TextMain, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 12.sp)
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
    androidx.compose.foundation.Image(
        bitmap = bitmap,
        contentDescription = app.label,
        modifier = Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.25f).dp))
    )
}
