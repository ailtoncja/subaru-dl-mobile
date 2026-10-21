package com.subarudl.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.subarudl.app.MainViewModel
import com.subarudl.app.Screen
import com.subarudl.app.service.AppUpdater
import com.subarudl.app.ui.home.HomeScreen
import com.subarudl.app.ui.settings.SettingsScreen
import com.subarudl.app.ui.setup.SetupScreen
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/** Curvas "enfatizadas" do Material 3: entra desacelerando, sai acelerando. */
private val EnterEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val ExitEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

/** Eixo horizontal compartilhado do Material: a tela nova vem do lado para onde se avança. */
private fun sharedAxisX(forward: Boolean): ContentTransform {
    val sign = if (forward) 1 else -1
    return (slideInHorizontally(tween(350, easing = EnterEasing)) { w -> sign * w / 8 } + fadeIn(tween(200, delayMillis = 80))) togetherWith
        (slideOutHorizontally(tween(200, easing = ExitEasing)) { w -> -sign * w / 8 } + fadeOut(tween(100)))
}

@Composable
fun AppRoot(vm: MainViewModel) {
    val snackbar = remember { SnackbarHostState() }
    val holder = rememberSaveableStateHolder()

    LaunchedEffect(Unit) {
        vm.toasts.collect { t ->
            // O aviso novo troca o que estiver na tela; o que tem "Desfazer" fica mais tempo
            snackbar.currentSnackbarData?.dismiss()
            launch {
                val result = snackbar.showSnackbar(t.text, actionLabel = t.action, duration = if (t.action != null) SnackbarDuration.Long else SnackbarDuration.Short)
                if (result == SnackbarResult.ActionPerformed) t.onAction?.invoke()
            }
        }
    }
    LaunchedEffect(Unit) {
        // Acabou de atualizar por dentro do app: o aviso aparece aqui e a notificação some
        AppUpdater.justUpdated.filterNotNull().collect { v ->
            AppUpdater.consumeJustUpdated()
            vm.toast("Subaru DL atualizado para a versão $v")
        }
    }
    BackHandler(enabled = vm.screen == Screen.SETTINGS && !vm.setupOpen) {
        if (vm.settingsPage != null) vm.settingsPage = null else vm.screen = Screen.HOME
    }

    // Profundidade de cada tela: decide de que lado a próxima entra
    val depth = when {
        vm.screen == Screen.HOME -> 0
        vm.settingsPage == null -> 1
        else -> 2
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // Assistente de primeiro uso: tela cheia no lugar de tudo, só enquanto estiver aberto
        if (vm.setupOpen) SetupScreen(vm, snackbar) else AnimatedContent(
            targetState = Triple(vm.screen, vm.settingsPage, depth),
            transitionSpec = { sharedAxisX(forward = targetState.third > initialState.third) },
            label = "tela",
        ) { (screen, page, _) ->
            holder.SaveableStateProvider(if (screen == Screen.HOME) "home" else "ajustes-${page ?: ""}") {
                when (screen) {
                    Screen.HOME -> HomeScreen(vm, snackbar)
                    Screen.SETTINGS -> SettingsScreen(vm, page, snackbar)
                }
            }
        }
    }
}
