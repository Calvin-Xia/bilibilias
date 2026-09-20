package com.imcys.bilibilias.shared.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.imcys.bilibilias.common.event.NavigatePageMode
import com.imcys.bilibilias.common.event.analysisHandleChannel
import com.imcys.bilibilias.common.event.navigatePageEventFlow
import com.imcys.bilibilias.common.event.playVoucherErrorChannel
import com.imcys.bilibilias.common.event.requestFrequentHandleChannel
import com.imcys.bilibilias.common.event.restoreBackStackEventFlow
import com.imcys.bilibilias.common.event.saveBackStackChannel
import com.imcys.bilibilias.data.repository.AppSettingsRepository
import com.imcys.bilibilias.datastore.AppSettingsSerializer
import com.imcys.bilibilias.datastore.enabledNavAnimation
import com.imcys.bilibilias.datastore.enabledNavOnBackInvokedCallback
import com.imcys.bilibilias.datastore.navBackStack
import com.imcys.bilibilias.shared.feature.analysis.AnalysisScreen
import com.imcys.bilibilias.shared.feature.analysis.navigation.AnalysisRoute
import com.imcys.bilibilias.shared.feature.analysis.videocodeing.VideoCodingInfoRoute
import com.imcys.bilibilias.shared.feature.analysis.videocodeing.VideoCodingInfoScreen
import com.imcys.bilibilias.shared.feature.download.DownloadScreen
import com.imcys.bilibilias.shared.feature.download.navigation.DownloadRoute
import com.imcys.bilibilias.shared.feature.event.playvoucher.PlayVoucherErrorPage
import com.imcys.bilibilias.shared.feature.event.playvoucher.navigation.PlayVoucherErrorRoute
import com.imcys.bilibilias.shared.feature.event.requestFrequent.RequestFrequentRoute
import com.imcys.bilibilias.shared.feature.event.requestFrequent.RequestFrequentScreen
import com.imcys.bilibilias.shared.feature.home.HomeScreen
import com.imcys.bilibilias.shared.feature.home.navigation.HomeRoute
import com.imcys.bilibilias.shared.feature.login.CookieLoginRoute
import com.imcys.bilibilias.shared.feature.login.CookieLoginScreen
import com.imcys.bilibilias.shared.feature.login.LoginScreen
import com.imcys.bilibilias.shared.feature.login.QRCodeLoginScreen
import com.imcys.bilibilias.shared.feature.login.navigation.LoginRoute
import com.imcys.bilibilias.shared.feature.login.navigation.QRCodeLoginRoute
import com.imcys.bilibilias.shared.feature.setting.SettingScreen
import com.imcys.bilibilias.shared.feature.setting.about.AboutRoute
import com.imcys.bilibilias.shared.feature.setting.about.AboutScreen
import com.imcys.bilibilias.shared.feature.setting.complaint.ComplaintRoute
import com.imcys.bilibilias.shared.feature.setting.complaint.ComplaintScreen
import com.imcys.bilibilias.shared.feature.setting.contract.NamingConventionRoute
import com.imcys.bilibilias.shared.feature.setting.contract.NamingConventionScreen
import com.imcys.bilibilias.shared.feature.setting.developer.LineConfigRoute
import com.imcys.bilibilias.shared.feature.setting.developer.LineConfigScreen
import com.imcys.bilibilias.shared.feature.setting.download.DownloadConfigRoute
import com.imcys.bilibilias.shared.feature.setting.download.DownloadConfigScreen
import com.imcys.bilibilias.shared.feature.setting.expand.SystemExpandRoute
import com.imcys.bilibilias.shared.feature.setting.expand.SystemExpandScreen
import com.imcys.bilibilias.shared.feature.setting.layout.LayoutTypesetRoute
import com.imcys.bilibilias.shared.feature.setting.layout.LayoutTypesetScreen
import com.imcys.bilibilias.shared.feature.setting.navigation.RoamRoute
import com.imcys.bilibilias.shared.feature.setting.navigation.SettingRoute
import com.imcys.bilibilias.shared.feature.setting.platform.ParsePlatformRoute
import com.imcys.bilibilias.shared.feature.setting.platform.ParsePlatformScreen
import com.imcys.bilibilias.shared.feature.setting.roam.RoamScreen
import com.imcys.bilibilias.shared.feature.setting.storage.StorageManagementRoute
import com.imcys.bilibilias.shared.feature.setting.storage.StorageManagementScreen
import com.imcys.bilibilias.shared.feature.setting.version.AppVersionInfoRoute
import com.imcys.bilibilias.shared.feature.setting.version.AppVersionInfoScreen
import com.imcys.bilibilias.shared.feature.tools.calendar.CalendarRoute
import com.imcys.bilibilias.shared.feature.tools.calendar.CalendarScreen
import com.imcys.bilibilias.shared.feature.tools.calendar.detail.SubjectDetailRoute
import com.imcys.bilibilias.shared.feature.tools.calendar.detail.SubjectDetailScreen
import com.imcys.bilibilias.shared.feature.tools.donate.DonateRoute
import com.imcys.bilibilias.shared.feature.tools.donate.DonateScreen
import com.imcys.bilibilias.shared.feature.tools.parser.WebParserRoute
import com.imcys.bilibilias.shared.feature.tools.parser.WebParserScreen
import com.imcys.bilibilias.shared.feature.user.UserScreen
import com.imcys.bilibilias.shared.feature.user.bangumifollow.BangumiFollowRoute
import com.imcys.bilibilias.shared.feature.user.bangumifollow.BangumiFollowScreen
import com.imcys.bilibilias.shared.feature.user.folder.UserFolderRoute
import com.imcys.bilibilias.shared.feature.user.folder.UserFolderScreen
import com.imcys.bilibilias.shared.feature.user.history.UserPlayHistoryRoute
import com.imcys.bilibilias.shared.feature.user.history.UserPlayHistoryScreen
import com.imcys.bilibilias.shared.feature.user.like.LikePageType
import com.imcys.bilibilias.shared.feature.user.like.LikeVideoRoute
import com.imcys.bilibilias.shared.feature.user.like.LikeVideoScreen
import com.imcys.bilibilias.shared.feature.user.navigation.UserRoute
import com.imcys.bilibilias.shared.feature.user.work.WorkListRoute
import com.imcys.bilibilias.shared.feature.user.work.WorkListScreen
import com.imcys.bilibilias.shared.platform.firebase.FirebaseExt
import com.imcys.bilibilias.shared.platform.firebase.FirebaseExt.logOpenAppPage
import kotlinx.coroutines.flow.first
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject

/**
 * BILIBILAIS导航显示组件
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun BILIBILAISNavDisplay() {
    val rootViewModelStoreOwner = LocalViewModelStoreOwner.current ?: error("No ViewModelStoreOwner")

    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()
    val navBackStackSerializer =
        remember { NavBackStackSerializer(elementSerializer = PolymorphicSerializer(NavKey::class)) }
    val navSavedStateConfiguration = remember {
        SavedStateConfiguration {
            serializersModule = navKeySerializersModule
        }
    }
    val backStack = rememberNavBackStack(navSavedStateConfiguration, HomeRoute())

    val json = koinInject<Json>()
    val settingsRepository = koinInject<AppSettingsRepository>()
    val settings by settingsRepository.appSettingsFlow.collectAsStateWithLifecycle(
        AppSettingsSerializer.appSettingsDefault
    )
    val navAnimationEnabled = settings.enabledNavAnimation
    val predictiveBackAnimationEnabled =
        navAnimationEnabled && settings.enabledNavOnBackInvokedCallback
    val onBack = { backStack.removeLastOrNullSafe() }
    // 监听解析事件
    LaunchedEffect(Unit) {
        analysisHandleChannel.collect {
            backStack.addWithReuse(AnalysisRoute(it.analysisText))
        }
    }

    LaunchedEffect(Unit) {
        playVoucherErrorChannel.collect {
            backStack.removeLastOrNullSafe()
            backStack.addWithReuse(PlayVoucherErrorRoute)
        }
    }

    LaunchedEffect(Unit) {
        requestFrequentHandleChannel.collect {
            backStack.addWithReuse(RequestFrequentRoute(it.url))
        }
    }

    LaunchedEffect(Unit) {
        navigatePageEventFlow.collect {
            backStack.navigate(it.navKey, it.mode)
        }
    }

    LaunchedEffect(Unit) {
        saveBackStackChannel.collect {
            val saveStr = navBackStackSerializer.saveBackStack(json, backStack)
            settingsRepository.updateNavBackStack(saveStr)
            it.onSaveFinish.invoke()
        }
    }

    LaunchedEffect(Unit) {
        restoreBackStackEventFlow.collect {
            val setting = settingsRepository.appSettingsFlow.first()
            if (setting.navBackStack.isNotEmpty()) {
                val stack = navBackStackSerializer.loadBackStack(json, setting.navBackStack)
                backStack.clear()
                backStack.addAll(stack)
                settingsRepository.updateNavBackStack("")
                FirebaseExt.logRestoreBackStack(stack.lastOrNull())
            }
        }
    }

    LaunchedEffect(backStack) {
        snapshotFlow { backStack.lastOrNull() }
            .collect { navKey ->
                navKey?.let(::logOpenAppPage)
            }
    }

    fun createForwardTransitionSpec(enabled: Boolean) = if (!enabled) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        ContentTransform(
            targetContentEnter = fadeIn(
                animationSpec = tween(
                    durationMillis = 400,
                    easing = FastOutSlowInEasing
                )
            ),
            initialContentExit = scaleOut(
                targetScale = 1.1F,
                animationSpec = tween(
                    durationMillis = 400,
                    easing = FastOutSlowInEasing
                )
            )
        )
    }

    fun createPopTransitionSpec(enabled: Boolean) = if (!enabled) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        ContentTransform(
            targetContentEnter = scaleIn(
                initialScale = 1.1F,
                animationSpec = tween(
                    durationMillis = 400,
                    easing = FastOutSlowInEasing
                )
            ),
            initialContentExit = fadeOut(
                animationSpec = tween(
                    durationMillis = 400,
                    easing = FastOutSlowInEasing
                )
            )
        )
    }

    fun createPredictivePopTransitionSpec(enabled: Boolean) = if (!enabled) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        ContentTransform(
            targetContentEnter = scaleIn(
                initialScale = 1.06F,
                animationSpec = tween(
                    durationMillis = 320,
                    easing = FastOutSlowInEasing
                )
            ),
            initialContentExit = fadeOut(
                animationSpec = tween(
                    durationMillis = 320,
                    easing = FastOutSlowInEasing
                )
            )
        )
    }

    val forwardTransitionSpec = remember(navAnimationEnabled) {
        createForwardTransitionSpec(navAnimationEnabled)
    }
    val popTransitionSpec = remember(navAnimationEnabled) {
        createPopTransitionSpec(navAnimationEnabled)
    }
    val predictivePopTransitionSpec = remember(predictiveBackAnimationEnabled) {
        createPredictivePopTransitionSpec(predictiveBackAnimationEnabled)
    }

    SharedTransitionLayout {
        NavDisplay(
            backStack = backStack,
            onBack = onBack,
            sharedTransitionScope = this,
            sceneStrategies = listOf(listDetailStrategy),
            entryDecorators = listOf(
                // 防止屏幕旋转等导致的重组时，页面状态丢失
                rememberSaveableStateHolderNavEntryDecorator(),
                // 限定每个页面有自己的viewmodel store
                rememberViewModelStoreNavEntryDecorator()
            ),
            transitionSpec = {
                forwardTransitionSpec
            },
            popTransitionSpec = {
                popTransitionSpec
            },
            predictivePopTransitionSpec = {
                predictivePopTransitionSpec
            },
            entryProvider = entryProvider {
                entry<HomeRoute> {
                    HomeScreen(
                        it,
                        this@SharedTransitionLayout,
                        LocalNavAnimatedContentScope.current,
                        goToLogin = {
                            backStack.addWithReuse(LoginRoute)
                        },
                        goToUserPage = { mid ->
                            backStack.addWithReuse(UserRoute(mid = mid))
                        },
                        goToAnalysis = {
                            backStack.addWithReuse(AnalysisRoute())
                        },
                        goToDownloadPage = {
                            backStack.addWithReuse(DownloadRoute())
                        },
                        goToSetting = {
                            backStack.addWithReuse(SettingRoute)
                        },
                        goToPage = { page ->
                            backStack.addWithReuse(page)
                        }
                    )
                }
                entry<LoginRoute> {
                    LoginScreen(
                        onToBack = onBack,
                        goToQRCodeLogin = {
                            backStack.addWithReuse(QRCodeLoginRoute())
                        }
                    )
                }
                entry<QRCodeLoginRoute> {
                    QRCodeLoginScreen(
                        it,
                        onToBack = onBack,
                        onBackHomePage = {
                            backStack.clear()
                            backStack.add(HomeRoute(isFormLogin = true))
                        },
                        onToCookieLogin = {
                            backStack.add(CookieLoginRoute)
                        }
                    )
                }
                entry<UserRoute> {
                    UserScreen(
                        userRoute = it,
                        viewModelStoreOwner = rootViewModelStoreOwner,
                        onToBack = onBack,
                        onToSettings = {
                            backStack.addWithReuse(SettingRoute)
                        },
                        onToWorkList = { mid ->
                            backStack.add(WorkListRoute(mid = mid))
                        },
                        onToBangumiFollow = { mid ->
                            backStack.add(BangumiFollowRoute(mid = mid))
                        },
                        onToUserFolder = { mid ->
                            backStack.add(UserFolderRoute(mid = mid))
                        },
                        onToLikeVideo = { mid ->
                            backStack.add(LikeVideoRoute(mid = mid, type = LikePageType.LIKE))
                        },
                        onToCoinVide = { mid ->
                            backStack.add(LikeVideoRoute(mid = mid, type = LikePageType.COIN))
                        },
                        onToPlayHistory = {
                            backStack.add(UserPlayHistoryRoute)
                        }
                    )
                }
                entry<AnalysisRoute> {
                    AnalysisScreen(
                        it,
                        this@SharedTransitionLayout,
                        LocalNavAnimatedContentScope.current,
                        onToBack = onBack,
                        goToUser = { mid ->
                            backStack.add(UserRoute(mid = mid, isAnalysisUser = true))
                        },
                        onToVideoCodingInfo = {
                            backStack.addWithReuse(VideoCodingInfoRoute)
                        },
                        onToLogin = {
                            backStack.addWithReuse(QRCodeLoginRoute(isFromAnalysis = true))
                        }
                    )
                }
                entry<DownloadRoute> {
                    DownloadScreen(
                        it,
                        onToBack = onBack
                    )
                }
                entry<SettingRoute>(
                    metadata = ListDetailSceneStrategy.listPane(
                        detailPlaceholder = {
                            Column(
                                Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "请选择右侧选项",
                                )
                            }
                        }
                    )
                ) {
                    SettingScreen(
                        onToRoam = {
                            backStack.addWithReuse(RoamRoute)
                        },
                        onToBack = onBack,
                        onToComplaint = { backStack.addWithReuse(ComplaintRoute) },
                        onToLayoutTypeset = { backStack.addWithReuse(LayoutTypesetRoute) },
                        onToAbout = { backStack.addWithReuse(AboutRoute) },
                        onToVersionInfo = { backStack.addWithReuse(AppVersionInfoRoute) },
                        onToSystemExpand = { backStack.addWithReuse(SystemExpandRoute) },
                        onToStorageManagement = { backStack.addWithReuse(StorageManagementRoute) },
                        onToDownloadConfig = { backStack.addWithReuse(DownloadConfigRoute) },
                        onToNamingConvention = { backStack.addWithReuse(NamingConventionRoute) },
                        onToLineConfig = { backStack.addWithReuse(LineConfigRoute) },
                        onLogoutFinish = {
                            backStack.firstOrNull {
                                it is UserRoute && !it.isAnalysisUser
                            }?.let {
                                backStack.remove(it)
                            }
                        },
                        onToPage = { backStack.addWithReuse(it) }
                    )
                }
                entry<RoamRoute>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) {
                    RoamScreen(
                        onToBack = onBack,
                        onGoToQRCodeLogin = {
                            backStack.addWithReuse(
                                QRCodeLoginRoute(
                                    defaultLoginPlatform = it,
                                    isFromRoam = true
                                )
                            )
                        }
                    )
                }
                entry<PlayVoucherErrorRoute> {
                    PlayVoucherErrorPage(
                        onBlack = {
                            backStack.removeLastOrNullSafe()
                            backStack.add(HomeRoute())
                        }
                    )
                }
                entry<WorkListRoute> {
                    WorkListScreen(
                        workListRoute = it,
                        onToBack = onBack
                    )
                }
                entry<BangumiFollowRoute> {
                    BangumiFollowScreen(
                        bangumiFollowRoute = it,
                        onToBack = onBack
                    )
                }
                entry<UserFolderRoute> {
                    UserFolderScreen(
                        userFolderRoute = it,
                        onToBack = onBack
                    )
                }
                entry<LikeVideoRoute> {
                    LikeVideoScreen(
                        likeVideoRoute = it,
                        onToBack = onBack
                    )
                }
                entry<ComplaintRoute>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) {
                    ComplaintScreen(
                        onToBack = onBack
                    )
                }
                entry<VideoCodingInfoRoute> {
                    VideoCodingInfoScreen(
                        onToBack = onBack
                    )
                }
                entry<LayoutTypesetRoute>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) {
                    LayoutTypesetScreen(
                        layoutTypesetRoute = it,
                        onToBack = onBack
                    )
                }
                entry<UserPlayHistoryRoute> {
                    UserPlayHistoryScreen(
                        userPlayHistoryRoute = it,
                        onToBack = onBack
                    )
                }
                entry<AboutRoute>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) {
                    AboutScreen(
                        aboutRoute = it,
                        onToBack = onBack
                    )
                }
                entry<AppVersionInfoRoute>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) {
                    AppVersionInfoScreen(
                        appVersionInfoRoute = it,
                        onToBack = onBack
                    )
                }
                entry<CookieLoginRoute> {
                    CookieLoginScreen(cookieLoginRoute = it, onToBack = {
                        backStack.removeLastOrNullSafe()
                    }, onFinish = {
                        backStack.clear()
                        backStack.add(HomeRoute(isFormLogin = true))
                    })
                }
                entry<DonateRoute> {
                    DonateScreen(donateRoute = it, onToBack = {
                        backStack.removeLastOrNullSafe()
                    })
                }
                entry<SystemExpandRoute>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) {
                    SystemExpandScreen(systemExpandRoute = it, onToBack = {
                        backStack.removeLastOrNullSafe()
                    })
                }
                entry<StorageManagementRoute>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) {
                    StorageManagementScreen(
                        route = it,
                        onToBack = onBack,
                        onToDownloadList = {
                            backStack.add(DownloadRoute(1))
                        }
                    )
                }
                entry<DownloadConfigRoute>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) {
                    DownloadConfigScreen(
                        route = it,
                        onToBack = onBack
                    )
                }
                entry<NamingConventionRoute>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) {
                    NamingConventionScreen(
                        namingConventionRoute = it,
                        onToBack = onBack
                    )
                }
                entry<RequestFrequentRoute> {
                    RequestFrequentScreen(
                        requestFrequentRoute = it,
                        onToBack = onBack
                    )
                }
                entry<LineConfigRoute>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) {
                    LineConfigScreen(
                        lineConfigRoute = it,
                        onToBack = onBack
                    )
                }
                entry<WebParserRoute> {
                    WebParserScreen(
                        webParserRoute = it,
                        onToBack = onBack
                    )
                }
                entry<ParsePlatformRoute> (
                    metadata = ListDetailSceneStrategy.detailPane()
                ){
                    ParsePlatformScreen(
                        parsePlatformRoute = it,
                        onToBack = onBack
                    )
                }
                entry<CalendarRoute> {
                    CalendarScreen(
                        calendarRoute = it,
                        onToSubjectDetail = {
                            backStack.addWithReuse(SubjectDetailRoute(it))
                        },
                        onToBack = onBack
                    )
                }
                entry<SubjectDetailRoute> {
                    SubjectDetailScreen(
                        subjectDetailRoute = it,
                        onToBack = onBack
                    )
                }
            }
        )
    }

}

/**
 * 栈内复用扩展函数
 * 如果栈中已存在相同类型的路由，则比较参数：
 * - 参数相同：将其之后的所有元素移除（目标及之前的保留）
 * - 参数不同：替换该路由实例并移除其之后的所有元素
 * 否则添加新的路由实例
 */
inline fun <reified T : NavKey> NavBackStack<T>.addWithReuse(route: T) {
    val existingIndex = indexOfFirst { it::class == T::class }

    if (existingIndex != -1) {
        val existingRoute = get(existingIndex)
        // 比较路由对象的完整内容，而不只是类型
        if (existingRoute == route) {
            // 参数相同，只需移除目标之后的所有元素
            repeat(size - existingIndex - 1) { removeAt(existingIndex + 1) }
        } else {
            // 参数不同，替换该路由并移除其之后的所有元素
            set(existingIndex, route)
            repeat(size - existingIndex - 1) { removeAt(existingIndex + 1) }
        }
    } else {
        add(route)
    }
}

/**
 * 运行时类型的栈内复用扩展函数。
 * 适用于事件总线等仅持有 NavKey 基类的场景。
 */
fun NavBackStack<NavKey>.addWithReuseKey(route: NavKey) {
    addWithReuse(route)
}

/**
 * 根据导航模式统一处理回退栈。
 */
fun NavBackStack<NavKey>.navigate(
    route: NavKey,
    mode: NavigatePageMode = NavigatePageMode.MoveToTop
) {
    when (mode) {
        NavigatePageMode.Push -> add(route)
        NavigatePageMode.ReuseInStack -> addWithReuseKey(route)
        NavigatePageMode.MoveToTop -> moveToTopOrAdd(route)
        NavigatePageMode.ReplaceTop -> replaceTop(route)
        NavigatePageMode.ClearAndPush -> {
            clear()
            add(route)
        }
    }
}

/**
 * 将已有页面移动到栈顶，否则直接添加到栈顶。
 * 如果存在多个相同页面实例，仅保留最新一次导航所需的目标实例。
 */
fun <T : NavKey> NavBackStack<T>.moveToTopOrAdd(route: T) {
    if (lastOrNull() == route) return
    removeAll { it == route }
    add(route)
}

/**
 * 使用目标页面替换当前栈顶；如果栈为空则直接添加。
 */
fun <T : NavKey> NavBackStack<T>.replaceTop(route: T) {
    if (isEmpty()) {
        add(route)
        return
    }
    removeLastOrNull()
    add(route)
}

/**
 * 安全移除栈顶元素扩展函数
 * 只要栈中元素大于1时才允许移除，防止最后一页被移除导致异常
 */
fun <T : NavKey> NavBackStack<T>.removeLastOrNullSafe() {
    if (this.size > 1) {
        this.removeLastOrNull()
    }
}

private fun NavBackStackSerializer<NavKey>.saveBackStack(
    json: Json,
    backStack: NavBackStack<NavKey>
): String {
    return json.encodeToString(
        this,
        backStack
    )
}

private fun NavBackStackSerializer<NavKey>.loadBackStack(
    json: Json,
    backStackStr: String
): NavBackStack<NavKey> {
    return json.decodeFromString(
        this,
        backStackStr
    )
}
