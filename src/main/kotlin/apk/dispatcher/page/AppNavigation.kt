package apk.dispatcher.page

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import apk.dispatcher.page.config.ApkConfigPage
import apk.dispatcher.page.home.HomePage
import apk.dispatcher.page.splash.SplashPage
import apk.dispatcher.page.start.StartPage
import apk.dispatcher.page.upload.UploadPage
import apk.dispatcher.page.upload.UploadParam
import apk.dispatcher.style.AppColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "splash",
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
        modifier = Modifier.fillMaxSize().background(AppColors.pageBackground),
    ) {
        composable(route = "splash") {
            SplashPage(navController)
        }
        composable(route = "start") {
            StartPage(navController)
        }
        composable(route = "home") {
            HomePage(navController)
        }
        composable(route = "edit?id={id}") {
            val id = it.arguments?.getString("id")
            ApkConfigPage(navController, id)
        }
        composable("upload/{param}") {
            val param = it.arguments?.getString("param") ?: ""
            val uploadParam = requireNotNull(UploadParam.adapter.fromJson(param))
            UploadPage(uploadParam) {
                navController.popBackStack()
            }
        }
    }
}

