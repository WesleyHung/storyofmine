package com.unlone.app.android.ui.navigation

import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.unlone.app.android.ui.auth.SignUpEmailScreen
import com.unlone.app.android.ui.auth.SignUpPwScreen
import com.unlone.app.android.ui.auth.signin.SignInEmailScreen
import com.unlone.app.android.ui.auth.signin.SignInPasswordScreen
import com.unlone.app.android.viewmodel.SignInViewModel
import com.unlone.app.android.viewmodel.SignUpViewModel
import kotlinx.coroutines.InternalCoroutinesApi
import org.koin.androidx.compose.viewModel


enum class AuthNav {
    SignIn,
    SignUp
}


@OptIn(InternalCoroutinesApi::class)
fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    onSigninOrSignupFinished: () -> Unit,
) {

    navigation(AuthNav.SignUp.name, route = "auth") {


        composable(AuthNav.SignUp.name) {
            val viewModelStoreOwner = remember { navController.getBackStackEntry("auth") }
            val viewModel by viewModel<SignUpViewModel>(owner = viewModelStoreOwner)

            SignUpEmailScreen(
                viewModel = viewModel,
                onEmailConfirmed = { navigateToSignUpPassword(navController) },
            ) { navigateToSignInEmail(navController) }
        }

        composable(AuthNav.SignUp.name + "/password") {
            val viewModelStoreOwner = remember { navController.getBackStackEntry("auth") }
            val viewModel by viewModel<SignUpViewModel>(owner = viewModelStoreOwner)

            SignUpPwScreen(
                viewModel = viewModel,
                onSignupSuccess = onSigninOrSignupFinished,
            )
        }

        composable(AuthNav.SignIn.name + "/email") {
            val viewModelStoreOwner = remember { navController.getBackStackEntry("auth") }
            val viewModel by viewModel<SignInViewModel>(owner = viewModelStoreOwner)
            SignInEmailScreen(
                navToSignInPw = { navigateToSignInPw(navController) },
                navToSignUp = { navigateToSignUp(navController) },
                viewModel = viewModel
            )
        }
        composable(AuthNav.SignIn.name + "/password") {
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

fun navigateToAuth(navController: NavHostController) {
    navController.navigate("auth")
}

fun navigateToSignUp(navController: NavHostController) {
    navController.navigate(AuthNav.SignUp.name)
}

fun navigateToSignUpPassword(navController: NavHostController) {
    navController.navigate(AuthNav.SignUp.name + "/password")
}

fun navigateToSignInEmail(navController: NavHostController) {
    navController.navigate(AuthNav.SignIn.name+ "/email")
}

fun navigateToSignInPw(navController: NavHostController) {
    navController.navigate(AuthNav.SignIn.name+ "/password")
}
