package com.unlone.app.android.ui.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.navigation
import com.unlone.app.android.ui.auth.signin.SignInEmailScreen
import com.unlone.app.android.ui.auth.signin.SignInPasswordScreen
import com.unlone.app.android.ui.auth.signup.EmailVerificationScreen
import com.unlone.app.android.ui.auth.signup.SetUsernameScreen
import com.unlone.app.android.ui.auth.signup.SignUpScreen
import com.unlone.app.android.viewmodel.SignInViewModel
import com.unlone.app.android.viewmodel.SignUpViewModel
import kotlinx.coroutines.InternalCoroutinesApi
import org.koin.androidx.compose.viewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


enum class AuthNav {
    SignIn,
    SignUp
}


@ExperimentalAnimationApi
@OptIn(InternalCoroutinesApi::class)
fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    onSigninOrSignupFinished: () -> Unit,
) {

    navigation(AuthNav.SignUp.name, route = "auth") {

        composable(AuthNav.SignUp.name) {
            val viewModelStoreOwner = remember { navController.getBackStackEntry("auth") }
            val viewModel by viewModel<SignUpViewModel>(owner = viewModelStoreOwner)

            SignUpScreen(
                viewModel = viewModel,
                navToSendEmailOtp = { navigateToEmailVerification(navController) },
                navToSignIn = { navigateToSignInEmail(navController) }
            )
        }
        composable(AuthNav.SignUp.name + "/setUsername") {
            val viewModelStoreOwner = remember { navController.getBackStackEntry("auth") }
            val viewModel by viewModel<SignUpViewModel>(owner = viewModelStoreOwner)

            SetUsernameScreen(
                viewModel = viewModel,
                onSignUpSuccess = onSigninOrSignupFinished,
            )
        }

        composable(AuthNav.SignUp.name + "/emailVerification") {
            val viewModelStoreOwner = remember { navController.getBackStackEntry("auth") }
            val viewModel by viewModel<SignUpViewModel>(owner = viewModelStoreOwner)

            EmailVerificationScreen(
                state = viewModel.uiState,
                onCancelSignUp = {
                    viewModel.removeSignUpRecord()
                    navController.popBackStack()
                },
                setOtp = viewModel.setOtp,
                navToSetUsername = { navigateToSetUsername(navController) },
                onOtpVerified = { viewModel.verifyOtp() },
                onOtpGenerate = { viewModel.generateOtp() }
            )
        }

        composable(
            AuthNav.SignIn.name + "/email",
        ) {
            val viewModelStoreOwner = remember { navController.getBackStackEntry("auth") }
            val viewModel by viewModel<SignInViewModel>(owner = viewModelStoreOwner)
            SignInEmailScreen(
                navToSignInPw = { navigateToSignInPw(navController) },
                navToSignUp = { navigateToSignUp(navController) },
                viewModel = viewModel
            )
        }
        composable(
            AuthNav.SignIn.name + "/password",
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentScope.SlideDirection.Start,
                    animationSpec = tween(200)
                )
            },
        ) {
            val viewModelStoreOwner = remember { navController.getBackStackEntry("auth") }
            val viewModel by viewModel<SignInViewModel>(owner = viewModelStoreOwner)
            SignInPasswordScreen(
                onSignInSuccess = onSigninOrSignupFinished,
                back = { navController.popBackStack() },
                viewModel = viewModel
            )
        }
    }
}

fun navigateToAuth(
    navController: NavHostController,
) {
    navController.navigate("auth")
}

fun navigateToSignUp(navController: NavHostController) {
    navController.navigate(AuthNav.SignUp.name)
}

fun navigateToSignInEmail(navController: NavHostController) {
    navController.navigate(AuthNav.SignIn.name + "/email")
}

fun navigateToSignInPw(navController: NavHostController) {
    navController.navigate(AuthNav.SignIn.name + "/password")
}

fun navigateToSetUsername(navController: NavHostController) {
    navController.navigate(AuthNav.SignUp.name + "/setUsername")
}

fun navigateToEmailVerification(navController: NavHostController) {
    navController.navigate(AuthNav.SignUp.name + "/emailVerification")
}
