package com.subarudl.app.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.subarudl.app.MainViewModel
import com.subarudl.app.R
import com.subarudl.app.Tab
import com.subarudl.app.ui.batch.PlaylistsScreen
import com.subarudl.app.ui.gallery.GalleryScreen
import com.subarudl.app.ui.home.HomeScreen
import com.subarudl.app.ui.queue.QueueScreen
import com.subarudl.app.ui.settings.SettingsScreen
import com.subarudl.app.ui.theme.C
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

    Column(Modifier.fillMaxSize().background(C.Surface).windowInsetsPadding(WindowInsets.statusBars)) {
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
                Snackbar(data, containerColor = C.CardHighest, contentColor = C.Text, actionColor = C.Gold, shape = RoundedCornerShape(14.dp))
            }
        }
        NavigationBar(containerColor = C.Container, tonalElevation = 0.dp) {
            Tab.entries.forEach { t ->
                NavigationBarItem(
                    selected = t == vm.tab,
                    onClick = { vm.tab = t; if (t != Tab.SETTINGS) settingsPage = null },
                    icon = { Icon(tabIcons.getValue(t), t.label) },
                    label = { Text(t.label, style = T.labelMd.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = C.Gold, selectedTextColor = C.Gold, indicatorColor = Color(0x33FACC15),
                        unselectedIconColor = C.Muted, unselectedTextColor = C.Muted,
                    ),
                )
            }
        }
    }
}

/** Barra de cima: marca, nome da tela, botão de colar e atalho para as contas. */
@Composable
private fun Header(vm: MainViewModel, settingsPage: String?, onProfile: () -> Unit) {
    val ctx = LocalContext.current
    val subtitle = if (vm.tab == Tab.SETTINGS && settingsPage != null) "Ajustes" else vm.tab.label
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_logo), null, Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Subaru DL", style = T.headlineSm, maxLines = 1)
            Text(subtitle, style = T.labelMd, maxLines = 1)
        }
        FilledTonalButton(
            onClick = {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val text = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
                if (text.contains("http")) vm.handleIncoming(text) else vm.toast("Nenhum link na área de transferência")
            },
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = C.Blue, contentColor = Color.White),
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) {
            Icon(Icons.Outlined.ContentPaste, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Colar", style = T.labelLg.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
        }
        Spacer(Modifier.width(8.dp))
        FilledTonalIconButton(onProfile, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = C.CardHigh, contentColor = C.BluePale)) {
            Icon(Icons.Outlined.Person, "Contas e cookies")
        }
    }
}
