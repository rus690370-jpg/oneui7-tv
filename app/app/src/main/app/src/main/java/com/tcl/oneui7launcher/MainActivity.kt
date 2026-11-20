package com.tcl.oneui7launcher

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

data class TvAppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OneUI7TvLauncher()
        }
    }
}

@Composable
fun OneUI7TvLauncher() {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<TvAppInfo>>(emptyList()) }
    var isShadeOpen by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }
    var weatherText by remember { mutableStateOf("Загрузка погоды...") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale("ru"))
        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now).replaceFirstChar { it.uppercase() }
            delay(1000)
        }
    }

    LaunchedEffect(Unit) {
        weatherText = fetchWeather(lat = 55.7558, lon = 37.6173)
    }

    LaunchedEffect(Unit) {
        apps = loadInstalledApps(context)
    }

    BackHandler(enabled = isShadeOpen) {
        isShadeOpen = false
    }

    val oneUiBackground = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0A0F1F),
            Color(0xFF141E38),
            Color(0xFF1B1535)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(oneUiBackground)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Menu) {
                    isShadeOpen = !isShadeOpen
                    true
                } else false
            }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 32.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.38f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = currentTime,
                        fontSize = 76.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-2).sp
                    )
                    Text(
                        text = currentDate,
                        fontSize = 20.sp,
                        color = Color(0xFFB0C4DE),
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color.White.copy(alpha = 0.10f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WbSunny,
                            contentDescription = "Погода",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = weatherText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    OneUiPillButton(
                        icon = Icons.Rounded.Tune,
                        label = "Панель",
                        onClick = { isShadeOpen = true }
                    )
                }
            }

            Text(
                text = "Приложения",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.62f),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(apps) { app ->
                    OneUiAppCard(app = app) {
                        val launchIntent = context.packageManager.getLeanbackLaunchIntentForPackage(app.packageName)
                            ?: context.packageManager.getLaunchIntentForPackage(app.packageName)
                        launchIntent?.let { context.startActivity(it) }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isShadeOpen,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it })
        ) {
            OneUiQuickSettingsShade(
                onClose = { isShadeOpen = false },
                context = context
            )
        }
    }
}

@Composable
fun OneUiAppCard(app: TvAppInfo, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.10f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 350f),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    if (isFocused) Color.White.copy(alpha = 0.22f)
                    else Color.White.copy(alpha = 0.10f)
                )
                .border(
                    width = if (isFocused) 2.5.dp else 1.dp,
                    color = if (isFocused) Color(0xFF64B5F6) else Color.White.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val bitmap = remember(app.packageName) {
                app.icon.toBitmap(140, 140).asImageBitmap()
            }
            Image(
                bitmap = bitmap,
                contentDescription = app.label,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = app.label,
            fontSize = 15.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
            color = if (isFocused) Color.White else Color.White.copy(alpha = 0.75f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun OneUiPillButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isFocused) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.14f))
            .border(1.5.dp, if (isFocused) Color.White else Color.Transparent, CircleShape)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = Color.White)
        Text(text = label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun OneUiQuickSettingsShade(onClose: () -> Unit, context: Context) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .padding(48.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(36.dp))
                .background(Color(0xFF161D2E))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(36.dp))
                .padding(32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Быстрые настройки One UI",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                OneUiPillButton(icon = Icons.Rounded.Close, label = "Закрыть", onClick = onClose)
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                QuickTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Wifi,
                    title = "Wi-Fi",
                    subtitle = "Настройки сети"
                ) {
                    context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                }
                QuickTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Bluetooth,
                    title = "Bluetooth",
                    subtitle = "Устройства и пульт"
                ) {
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                }
                QuickTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.SettingsInputHdmi,
                    title = "Входы / Экран",
                    subtitle = "Настройки ТВ"
                ) {
                    context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
                }
                QuickTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Settings,
                    title = "Все настройки",
                    subtitle = "Система TCL"
                ) {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
        }
    }
}

@Composable
fun QuickTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .height(140.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (isFocused) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.09f))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(28.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(32.dp))
        Column {
            Text(text = title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = subtitle, fontSize = 14.sp, color = Color.White.copy(alpha = 0.75f))
        }
    }
}

suspend fun loadInstalledApps(context: Context): List<TvAppInfo> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val tvIntent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
    }
    val allResolveInfos = (pm.queryIntentActivities(intent, 0) + pm.queryIntentActivities(tvIntent, 0))
        .distinctBy { it.activityInfo.packageName }
        .filter { it.activityInfo.packageName != context.packageName }

    allResolveInfos.map {
        TvAppInfo(
            label = it.loadLabel(pm).toString(),
            packageName = it.activityInfo.packageName,
            icon = it.loadIcon(pm)
        )
    }.sortedBy { it.label.lowercase() }
}

suspend fun fetchWeather(lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
    try {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"
        val response = URL(url).readText()
        val current = JSONObject(response).getJSONObject("current_weather")
        val temp = current.getDouble("temperature").toInt()
        val sign = if (temp > 0) "+" else ""
        "$sign$temp°C"
    } catch (e: Exception) {
        "Погода офлайн"
    }
}
