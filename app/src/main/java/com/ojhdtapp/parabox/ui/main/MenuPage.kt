package com.ojhdtapp.parabox.ui.main

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.ojhdtapp.parabox.ui.MainScreenSharedViewModel
import com.ojhdtapp.parabox.ui.NavGraphs
import com.ojhdtapp.parabox.ui.message.MessagePageViewModel
import com.ojhdtapp.parabox.ui.util.MenuNavGraph
import com.ojhdtapp.parabox.ui.util.NavigationBar
import com.ojhdtapp.parabox.ui.util.NavigationRail
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.dependency

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialNavigationApi::class)
@Destination
@RootNavGraph(start = true)
@Composable
fun MenuPage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    navController: NavController,
    sizeClass: WindowSizeClass
) {
    // Destination
    val menuNavController = rememberAnimatedNavController()
    val menuNavHostEngine = rememberAnimatedNavHostEngine(
        navHostContentAlignment = Alignment.TopCenter,
        rootDefaultAnimations = RootNavGraphDefaultAnimations(),
        defaultAnimationsForNestedNavGraph = mapOf(
//                    NavGraphs.message to NestedNavGraphDefaultAnimations(
//                        enterTransition = { scaleIn(tween(200), 0.9f) + fadeIn(tween(200)) },
//                        exitTransition = { scaleOut(tween(200), 1.1f) + fadeOut(tween(200)) },
//                        popEnterTransition = {scaleIn(tween(200), 1.1f) + fadeIn(tween(200))},
//                        popExitTransition = {scaleOut(tween(200), 0.9f) + fadeOut(tween(200))}
//                    )
        )
    )
// Shared View Model
    val sharedViewModel =
        hiltViewModel<MainScreenSharedViewModel>()
    com.ojhdtapp.parabox.ui.util.NavigationDrawer(
        modifier = modifier.fillMaxSize(),
        navController = menuNavController,
        messageBadge = sharedViewModel.messageBadge.value,
        onSelfItemClick = {}) {
        Column() {
            Row(modifier = Modifier.weight(1f)) {
//                              if (sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) { }
                if (sizeClass.widthSizeClass == WindowWidthSizeClass.Medium) {
                    NavigationRail(
                        navController = menuNavController,
                        messageBadge = sharedViewModel.messageBadge.value,
                        onSelfItemClick = {})
                }
                 DestinationsNavHost(
                    navGraph = NavGraphs.menu,
                    engine = menuNavHostEngine,
                    navController = menuNavController,
                    dependenciesContainerBuilder = {
//                        dependency(NavGraphs.message) {
//                            val parentEntry = remember(navBackStackEntry) {
//                                menuNavController.getBackStackEntry(NavGraphs.message.route)
//                            }
//                            hiltViewModel<MessagePageViewModel>(parentEntry)
//                        }
                        dependency(sharedViewModel)
                        dependency(sizeClass)
                    }
                )
            }
            if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact
            ) {
                NavigationBar(
                    navController = menuNavController,
                    messageBadge = sharedViewModel.messageBadge.value,
                    onSelfItemClick = {},
                )
            }
        }
    }
}