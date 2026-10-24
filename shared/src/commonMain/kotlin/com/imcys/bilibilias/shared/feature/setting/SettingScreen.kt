package com.imcys.bilibilias.shared.feature.setting

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import bilibilias.shared.generated.resources.Res
import bilibilias.shared.generated.resources.common_cancel
import bilibilias.shared.generated.resources.ic_github_24px
import bilibilias.shared.generated.resources.ic_save_24px
import bilibilias.shared.generated.resources.setting_logging_out
import bilibilias.shared.generated.resources.setting_logout
import bilibilias.shared.generated.resources.setting_logout_confirm
import com.imcys.bilibilias.datastore.AppSettings
import com.imcys.bilibilias.datastore.AppSettings.AgreePrivacyPolicyState.Agreed
import com.imcys.bilibilias.datastore.AppSettings.AgreePrivacyPolicyState.Refuse
import com.imcys.bilibilias.datastore.agreePrivacyPolicy
import com.imcys.bilibilias.datastore.enabledClipboardAutoHandling
import com.imcys.bilibilias.datastore.enabledDynamicColor
import com.imcys.bilibilias.datastore.enabledNavAnimation
import com.imcys.bilibilias.datastore.enabledNavOnBackInvokedCallback
import com.imcys.bilibilias.datastore.getDefaultInstance
import com.imcys.bilibilias.network.ApiStatus
import com.imcys.bilibilias.shared.platform.firebase.FirebaseExt
import com.imcys.bilibilias.shared.platform.runtime.openLink
import com.imcys.bilibilias.shared.platform.permission.rememberNotificationPermissionController
import com.imcys.bilibilias.shared.app.PrivacyPolicyDialog
import com.imcys.bilibilias.shared.app.PrivacyPolicyRefuseDialog
import com.imcys.bilibilias.shared.feature.setting.download.DownloadConfigRoute
import com.imcys.bilibilias.shared.feature.setting.platform.ParsePlatformRoute
import com.imcys.bilibilias.ui.utils.switchHapticFeedback
import com.imcys.bilibilias.ui.component.ASAlertDialog
import com.imcys.bilibilias.ui.component.ASTextButton
import com.imcys.bilibilias.ui.component.ASTopAppBar
import com.imcys.bilibilias.ui.component.AsBackIconButton
import com.imcys.bilibilias.ui.component.BILIBILIASTopAppBarStyle
import com.imcys.bilibilias.ui.component.BaseSettingsItem
import com.imcys.bilibilias.ui.component.CategorySettingsItem
import com.imcys.bilibilias.ui.component.SwitchSettingsItem
import com.imcys.bilibilias.ui.component.dialog.PermissionRequestTipDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingScreenPreview() {
    SettingScreen(
        onToBack = {},
        onToComplaint = {},
        onToLayoutTypeset = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingScreen(
    onToComplaint: () -> Unit,
    onToLayoutTypeset: () -> Unit,
    onToBack: () -> Unit,
    onToAbout: () -> Unit = {},
    onToVersionInfo: () -> Unit = {},
    onToSystemExpand: () -> Unit = {},
    onToStorageManagement: () -> Unit = {},
    onToDownloadConfig: () -> Unit = {},
    onToNamingConvention: () -> Unit = {},
    onToLineConfig: () -> Unit = {},
    onToPage: (navKey: NavKey) -> Unit = {},
    onLogoutFinish: (Long) -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val vm = koinViewModel<SettingViewModel>()
    val lastGitCommitInfo by vm.lastGitCommitInfo.collectAsStateWithLifecycle()
    val appSettings by vm.appSettings.collectAsStateWithLifecycle(
        initialValue = AppSettings.getDefaultInstance()
    )
    val haptics = LocalHapticFeedback.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var showLogoutLoading by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showPrivacyPolicyRefuseTip by remember { mutableStateOf(false) }

    SettingScaffold(scrollBehavior, onToBack) {
        LazyColumn(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        ) {
            item {
                CategorySettingsItem(text = "缓存配置")
            }

            item {
                BaseSettingsItem(
                    painter = painterResource(Res.drawable.ic_save_24px),
                    text = "存储管理",
                    descriptionText = "管理APP内存占用",
                    onClick = onToStorageManagement
                )
            }

            item {
                BaseSettingsItem(
                    painter = rememberVectorPainter(Icons.Outlined.Edit),
                    text = "命名规则",
                    descriptionText = "自定义下载文件名称",
                    onClick = onToNamingConvention
                )
            }

            item {
                BaseSettingsItem(
                    painter = rememberVectorPainter(Icons.Outlined.Download),
                    text = "下载配置",
                    descriptionText = "存储路径和下载设置",
                    onClick = { onToPage(DownloadConfigRoute) }
                )
            }

            item {
                CategorySettingsItem(text = "个性设置")
            }

            item {
                SwitchSettingsItem(
                    imageVector = Icons.Outlined.Palette,
                    text = "动态主题",
                    description = "使用桌面壁纸颜色作为主题",
                    checked = appSettings.enabledDynamicColor,
                ) { check ->
                    haptics.switchHapticFeedback(check)
                    vm.updateEnabledDynamicColor(check)
                }
            }

            item {
                SwitchSettingsItem(
                    enabled = appSettings.enabledNavAnimation,
                    imageVector = Icons.Outlined.Gesture,
                    text = "预测性返回手势",
                    checked = appSettings.enabledNavOnBackInvokedCallback,
                ) { check ->
                    haptics.switchHapticFeedback(check)
                    vm.updateEnabledOnBackInvokedCallback(check)
                }
            }

            item {
                SwitchSettingsItem(
                    imageVector = Icons.Outlined.Animation,
                    text = "导航动画",
                    checked = appSettings.enabledNavAnimation,
                ) { check ->
                    haptics.switchHapticFeedback(check)
                    vm.updateEnabledNavAnimation(check)
                }
            }

            item {
                DownloadPostNotifications()
            }

            item {
                CategorySettingsItem(text = "布局配置")
            }

            item {
                BaseSettingsItem(
                    painter = rememberVectorPainter(Icons.AutoMirrored.Outlined.ListAlt),
                    text = "首页排版",
                    description = {},
                    onClick = onToLayoutTypeset
                )
            }

            item {
                CategorySettingsItem(text = "解析配置")
            }

            item {
                BaseSettingsItem(
                    painter = rememberVectorPainter(Icons.Outlined.Hub),
                    text = "解析平台",
                    descriptionText = "使用不同的平台标识来解析视频",
                    onClick = { onToPage(ParsePlatformRoute) }
                )
            }

            item {
                SwitchSettingsItem(
                    imageVector = Icons.Default.ContentPaste,
                    text = "自动解析",
                    description = "恢复前台时自动提取剪切板进行解析",
                    checked = appSettings.enabledClipboardAutoHandling,
                ) { check ->
                    haptics.switchHapticFeedback(check)
                    vm.updateClipboardAutoHandling(check)
                }
            }

            item {
                CategorySettingsItem(text = "关于程序")
            }

            item {
                BaseSettingsItem(
                    painter = rememberVectorPainter(Icons.Outlined.Group),
                    text = "关于",
                    descriptionText = "作为依赖平台的程序，我们有责任和义务维护平台生态的健康发展！",
                    onClick = onToAbout
                )
            }

            item {
                BaseSettingsItem(
                    painter = rememberVectorPainter(Icons.Outlined.Update),
                    text = "版本追踪",
                    description = {
                        when (lastGitCommitInfo.status) {
                            ApiStatus.SUCCESS -> {
                                Text(
                                    "${lastGitCommitInfo.data?.tipMsg}",
                                    maxLines = 2
                                )
                            }

                            ApiStatus.ERROR -> {
                                Text("无法获取版本信息")
                            }

                            else -> {
                                Text("正在获取版本信息")
                            }
                        }
                    },
                    onClick = {
                        openLink("https://github.com/1250422131/bilibilias/commits/master")
                    }
                )
            }

            item {
                BaseSettingsItem(
                    painter = painterResource(Res.drawable.ic_github_24px),
                    text = "Github仓库",
                    description = {},
                    onClick = {
                        openLink("https://github.com/1250422131/bilibilias")
                    }
                )
            }

            item {
                CategorySettingsItem(text = "账户")
            }

            item {
                BaseSettingsItem(
                    painter = rememberVectorPainter(Icons.Outlined.Android),
                    text = "设备信息",
                    descriptionText = "提交反馈时记得带上这个！",
                    onClick = onToVersionInfo
                )
            }

            item {
                BaseSettingsItem(
                    painter = rememberVectorPainter(Icons.Outlined.Policy),
                    text = "隐私政策",
                    descriptionText = "当前状态：${
                        when (appSettings.agreePrivacyPolicy) {
                            Agreed -> "已同意"
                            Refuse -> "已拒绝"
                            else -> "未选择"
                        }
                    }，可在这里拒绝或同意我们的隐私政策。",
                    onClick = { showPrivacyPolicy = true }
                )
            }

            if (uiState.isLogin) {
                item {
                    BaseSettingsItem(
                        painter = rememberVectorPainter(Icons.AutoMirrored.Default.Logout),
                        text = "退出登录",
                        descriptionText = "清空登录信息，解除登录占用。",
                        onClick = { showLogoutDialog = true }
                    )
                }
            }

            item {
                CategorySettingsItem(text = "高级")
            }

            item {
                BaseSettingsItem(
                    painter = rememberVectorPainter(Icons.Outlined.Cloud),
                    text = "线路配置",
                    descriptionText = "试着改进你的下载体验。",
                    onClick = onToLineConfig
                )
            }
        }

        PrivacyPolicyDialog(
            showState = showPrivacyPolicy,
            onClickConfirm = {
                showPrivacyPolicy = false
                vm.updatePrivacyPolicyAgreement(Agreed)
            },
            onClickDismiss = {
                showPrivacyPolicy = false
                showPrivacyPolicyRefuseTip = true
                FirebaseExt.setDataCollectionEnabled(false)
                vm.updatePrivacyPolicyAgreement(Refuse)
            }
        )

        PrivacyPolicyRefuseDialog(
            showState = showPrivacyPolicyRefuseTip,
            onClickConfirm = {
                showPrivacyPolicyRefuseTip = false
            }
        )

        ASAlertDialog(
            showState = showLogoutDialog,
            title = { Text(stringResource(Res.string.setting_logout)) },
            text = {
                Column(
                    Modifier
                        .animateContentSize()
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (showLogoutLoading) {
                        ContainedLoadingIndicator()
                        Text(stringResource(Res.string.setting_logging_out))
                    } else {
                        Text(stringResource(Res.string.setting_logout_confirm))
                    }
                }
            },
            onDismiss = {
                showLogoutDialog = false
            },
            confirmButton = {
                ASTextButton(onClick = {
                    showLogoutLoading = true
                    coroutineScope.launch(Dispatchers.IO) {
                        vm.logout()
                        showLogoutLoading = false
                        showLogoutDialog = false
                        onLogoutFinish(uiState.currentMid)
                    }
                }) {
                    Text(stringResource(Res.string.setting_logout))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        )
    }
}

@Composable
fun DownloadPostNotifications() {
    val haptics = LocalHapticFeedback.current
    var hasForegroundServicePermission by remember { mutableStateOf(false) }
    var showRequestForegroundServiceTip by remember { mutableStateOf(false) }
    val permissionController = rememberNotificationPermissionController(
        onGranted = {
            hasForegroundServicePermission = true
            showRequestForegroundServiceTip = false
        },
        onDenied = {
            hasForegroundServicePermission = false
            showRequestForegroundServiceTip = false
        }
    )

    hasForegroundServicePermission = permissionController.hasPermission
    if (!permissionController.shouldShow) return

    SwitchSettingsItem(
        imageVector = Icons.Outlined.Notifications,
        text = "前台通知",
        description = "开启后可以使得在后台的下载任务不会被系统回收",
        checked = hasForegroundServicePermission,
    ) {
        haptics.switchHapticFeedback(it)
        if (!permissionController.hasPermission) {
            showRequestForegroundServiceTip = true
        }
    }

    if (showRequestForegroundServiceTip) {
        DownloadServicePermissionRequestTipDialog(
            onDismiss = {
                showRequestForegroundServiceTip = false
            },
            onRequest = {
                permissionController.request()
            }
        )
    }
}

@Composable
fun DownloadServicePermissionRequestTipDialog(
    onDismiss: () -> Unit,
    onRequest: () -> Unit
) {
    PermissionRequestTipDialog(
        show = true,
        message = "为了我们可以在后台缓存较长视频，接下来将向您申请通知服务权限。",
        onConfirm = onRequest,
        onDismiss = onDismiss
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScaffold(
    scrollBehavior: TopAppBarScrollBehavior,
    onToBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            ASTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                scrollBehavior = scrollBehavior,
                style = BILIBILIASTopAppBarStyle.Large,
                title = { Text(text = "设置") },
                navigationIcon = {
                    AsBackIconButton(onClick = {
                        onToBack.invoke()
                    })
                },
            )
        },
    ) {
        content(it)
    }
}
