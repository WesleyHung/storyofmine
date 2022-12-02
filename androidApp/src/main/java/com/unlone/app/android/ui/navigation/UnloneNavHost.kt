package com.unlone.app.android.ui.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.accompanist.insets.ExperimentalAnimatedInsets
import com.unlone.app.android.ui.UnloneAppState
import com.unlone.app.android.viewmodel.*
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.nanoseconds


@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalLayoutApi
@OptIn(
    ExperimentalAnimatedInsets::class
)
@Composable
fun MainNavHost(
    appState: UnloneAppState,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    navigateUp: () -> Unit,

    ) {
    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = modifier,
        route = "main",
    ) {

        composable("splash"){
            LaunchedEffect(Unit) {
                delay(duration = 1.nanoseconds)
                navController.popBackStack()
                navController.navigate(UnloneBottomDestinations.Write.routeWithArgs)
            }
//            SplashScreen {
//                navController.popBackStack()
//                navController.navigate(UnloneBottomDestinations.Write.routeWithArgs)
//            }
        }

        // todo: Add on-boarding Screens

        authGraph(
            navController,
            onSigninOrSignupFinished = { lastRoute ->
                navController.popBackStack(
                    route = lastRoute,
                    inclusive = true,
                    saveState = false
                )
            },
        )

        writeGraph(navController, navToStories = {
            appState.navigateToBottomBarRoute(UnloneBottomDestinations.Stories.route + "?requestedStoryId=$it")
        })
        storiesGraph(navController, navigateUp)
        profileGraph(navController, navigateUp)

    }
}

