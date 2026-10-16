package com.codingwithtashi.dailyprayer.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.codingwithtashi.dailyprayer.core.notification.DeepLink
import com.codingwithtashi.dailyprayer.core.notification.DeepLinks
import com.codingwithtashi.dailyprayer.ui.main.MainScaffold
import com.codingwithtashi.dailyprayer.ui.main.MainViewModel
import com.codingwithtashi.dailyprayer.ui.notifications.NotificationsScreen
import com.codingwithtashi.dailyprayer.ui.onboarding.OnboardingScreen
import com.codingwithtashi.dailyprayer.ui.player.NowPlayingScreen
import com.codingwithtashi.dailyprayer.ui.reader.ReaderScreen
import com.codingwithtashi.dailyprayer.ui.routine.RoutineDetailScreen
import com.codingwithtashi.dailyprayer.ui.routine.RoutineEditorScreen
import com.codingwithtashi.dailyprayer.ui.settings.SettingsScreen
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme

private typealias Scope = AnimatedContentTransitionScope<NavBackStackEntry>

@Composable
fun AppNavHost(
    startDestination: Any,
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val k = if (PrayerTheme.reduceMotion) 0 else 1
    val pushMs = 200 * k
    val playerMs = 300 * k
    val activity = LocalActivity.current
    // Activity-scoped so Settings / deep links can switch the tab shown by the scaffold.
    val mainViewModel: MainViewModel = hiltViewModel(
        viewModelStoreOwner = checkNotNull(activity as? ViewModelStoreOwner) { "Host must be a ViewModelStoreOwner" },
    )
    val eventBus = hiltViewModel<NavigationEventsViewModel>().bus

    LaunchedEffect(navController) {
        eventBus.events.collect { link ->
            val current = navController.currentDestination
            // Ignore links until onboarding is done.
            if (current != null && !current.hasRoute<Onboarding>()) {
                when (link) {
                    is DeepLink.OpenRoutine -> {
                        val showing = navController.currentBackStackEntry
                            ?.takeIf { it.destination.hasRoute<RoutineDetail>() }?.toRoute<RoutineDetail>()?.routineId
                        if (!DeepLinks.isRedundant(link, showing)) {
                            navController.navigate(RoutineDetail(link.routineId))
                        }
                    }
                    DeepLink.OpenNotifications -> navController.navigate(Notifications)
                    DeepLink.OpenPlayer -> navController.navigate(NowPlaying)
                    is DeepLink.OpenPrayer -> navController.navigate(Reader(link.prayerId))
                    DeepLink.OpenLibrary -> {
                        mainViewModel.selectTab(MainTab.LIBRARY)
                        navController.popBackStack<Main>(inclusive = false)
                    }
                    DeepLink.OpenListen -> {
                        mainViewModel.selectTab(MainTab.LISTEN)
                        navController.popBackStack<Main>(inclusive = false)
                    }
                }
            }
            eventBus.consumed()
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(tween(pushMs)) { it / 4 } + fadeIn(tween(pushMs))
        },
        exitTransition = {
            if (targetState.destination.hasRoute<NowPlaying>()) ExitTransition.None
            else slideOutHorizontally(tween(pushMs)) { -it / 4 } + fadeOut(tween(pushMs))
        },
        popEnterTransition = {
            if (initialState.destination.hasRoute<NowPlaying>()) EnterTransition.None
            else slideInHorizontally(tween(pushMs)) { -it / 4 } + fadeIn(tween(pushMs))
        },
        popExitTransition = {
            slideOutHorizontally(tween(pushMs)) { it / 4 } + fadeOut(tween(pushMs))
        },
    ) {
        composable<Onboarding> {
            OnboardingScreen(onFinished = {
                navController.navigate(Main) { popUpTo<Onboarding> { inclusive = true } }
            })
        }
        composable<Main> {
            MainScaffold(
                onOpenSettings = { navController.navigate(Settings) },
                onOpenNotifications = { navController.navigate(Notifications) },
                onOpenRoutine = { navController.navigate(RoutineDetail(it)) },
                onNewRoutine = { navController.navigate(RoutineEditor()) },
                onOpenReader = { prayerId, routineId -> navController.navigate(Reader(prayerId, routineId)) },
                onOpenPlayer = { navController.navigate(NowPlaying) },
                viewModel = mainViewModel,
            )
        }
        composable<RoutineDetail> { entry ->
            val route = entry.toRoute<RoutineDetail>()
            RoutineDetailScreen(
                routineId = route.routineId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(RoutineEditor(it)) },
                onOpenReader = { prayerId, routineId -> navController.navigate(Reader(prayerId, routineId)) },
                onOpenPlayer = { navController.navigate(NowPlaying) },
            )
        }
        composable<RoutineEditor> { entry ->
            val route = entry.toRoute<RoutineEditor>()
            RoutineEditorScreen(
                routineId = route.routineId,
                onClose = { navController.popBackStack() },
                onDeleted = { navController.popBackStack<Main>(inclusive = false) },
            )
        }
        composable<Reader> { entry ->
            val route = entry.toRoute<Reader>()
            ReaderScreen(
                prayerId = route.prayerId,
                routineId = route.routineId,
                onBack = { navController.popBackStack() },
                onReplaceWith = { next, rid ->
                    navController.navigate(Reader(next, rid)) { popUpTo<Reader> { inclusive = true } }
                },
                onFinished = { navController.popBackStack() },
                onOpenPlayer = { navController.navigate(NowPlaying) },
            )
        }
        composable<Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenDownloads = {
                    mainViewModel.showDownloads()
                    navController.popBackStack<Main>(inclusive = false)
                },
                onOpenNotifications = { navController.navigate(Notifications) },
                onExit = { activity?.finishAffinity() },
            )
        }
        composable<Notifications> {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }
        composable<NowPlaying>(
            enterTransition = { slideInVertically(tween(playerMs, easing = FastOutSlowInEasing)) { it } },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { slideOutVertically(tween(playerMs, easing = FastOutSlowInEasing)) { it } },
        ) {
            NowPlayingScreen(onClose = { navController.popBackStack() })
        }
    }
}
