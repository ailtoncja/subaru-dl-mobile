package com.subarudl.app.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subarudl.app.MainViewModel
import com.subarudl.app.R
import com.subarudl.app.Tab
import com.subarudl.app.ui.batch.PlaylistsScreen
import com.subarudl.app.ui.components.Mono
import com.subarudl.app.ui.gallery.GalleryScreen
import com.subarudl.app.ui.home.HomeScreen
import com.subarudl.app.ui.queue.QueueScreen
import com.subarudl.app.ui.settings.SettingsScreen
import com.subarudl.app.ui.theme.C
import com.subarudl.app.ui.theme.SpaceGrotesk
import com.subarudl.app.ui.theme.T

private val tabIcons: Map<Tab, ImageVector> = mapOf(
    Tab.HOME to Icons.Outlined.Download,
    Tab.QUEUE to Icons.Outlined.SwapVert,
    Tab.PLAYLISTS to Icons.Outlined.PlaylistPlay,
    Tab.GALLERY to Icons.Outlined.PhotoLibrary,
    Tab.SETTINGS to Icons.Outlined.Settings,
)

@Composable
fun AppRoot(vm: MainViewModel) {
    val snackbar = remember { SnackbarHostState() }
    val holder = rememberSaveableStateHolder()
    var settingsPage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.toasts.collect { snackbar.showSnackbar(it) } }
    BackHandler(enabled = vm.tab != Tab.HOME || settingsPage != null) {
        if (vm.tab == Tab.SETTINGS && settingsPage != null) settingsPage = null else vm.tab = Tab.HOME
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0D1220), C.Surface)))) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
            Header(vm, settingsPage, onProfile = { vm.tab = Tab.SETTINGS; settingsPage = "contas" })
            Box(Modifier.weight(1f)) {
                holder.SaveableStateProvider(vm.tab.name) {
                    when (vm.tab) {
                        Tab.HOME -> HomeScreen(vm)
                        Tab.QUEUE -> QueueScreen(vm)
                        Tab.PLAYLISTS -> PlaylistsScreen(vm)
                        Tab.GALLERY -> GalleryScreen(vm)
                        Tab.SETTINGS -> SettingsScreen(vm, settingsPage, onPage = { settingsPage = it })
                    }
                }
                SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(12.dp)) { data ->
                    Snackbar(data, containerColor = C.CardHighest, contentColor = C.Text, shape = RoundedCornerShape(10.dp))
                }
            }
            BottomBar(vm.tab) { vm.tab = it; if (it != Tab.SETTINGS) settingsPage = null }
        }
    }
}

@Composable
private fun Header(vm: MainViewModel, settingsPage: String?, onProfile: () -> Unit) {
    val ctx = LocalContext.current
    val subtitle = if (vm.tab == Tab.SETTINGS && settingsPage != null) "Ajustes" else vm.tab.label
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painterResource(R.drawable.ic_logo), null,
            Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, C.Border, RoundedCornerShape(10.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Subaru DL", style = T.headlineSm.copy(fontFamily = SpaceGrotesk, fontSize = 22.sp), maxLines = 1)
                Spacer(Modifier.width(8.dp))
                Text(
                    "WRC MOBILE",
                    Modifier.clip(RoundedCornerShape(3.dp)).background(Color(0x1FFACC15)).border(1.dp, Color(0x59FACC15), RoundedCornerShape(3.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                    style = T.labelSm.copy(fontSize = 9.sp), color = C.Gold, maxLines = 1,
                )
            }
            Mono(subtitle, color = C.Muted, style = T.labelMd)
        }
        Row(
            Modifier.height(44.dp).clip(RoundedCornerShape(8.dp)).background(C.Blue).border(1.dp, Color(0x663B82F6), RoundedCornerShape(8.dp))
                .clickable {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val text = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
                    if (text.contains("http")) vm.handleIncoming(text) else vm.toast("Nenhum link na área de transferência")
                }.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.ContentPaste, null, Modifier.size(19.dp), tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Colar", style = T.labelLg, color = Color.White)
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFC5CAF7)).clickable(onClick = onProfile),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Person, "Contas", Modifier.size(24.dp), tint = Color(0xFF1B2A6B)) }
    }
}

@Composable
private fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color(0xF20A0E17)).windowInsetsPadding(WindowInsets.navigationBars)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.Border))
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Tab.entries.forEach { t ->
                val on = t == selected
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (on) C.CardHigh else Color.Transparent)
                        .clickable { onSelect(t) }.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(tabIcons.getValue(t), t.label, Modifier.size(24.dp), tint = if (on) C.Gold else C.Muted)
                    Spacer(Modifier.height(3.dp))
                    Text(t.label, style = T.labelSm.copy(fontSize = 10.5.sp), color = if (on) C.Gold else C.Muted, maxLines = 1)
                }
            }
        }
    }
}
