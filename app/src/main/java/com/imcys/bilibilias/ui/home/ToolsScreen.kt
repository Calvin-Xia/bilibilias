package com.imcys.bilibilias.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CopyAll
import androidx.compose.material.icons.outlined.VideoCameraBack
import androidx.compose.material.icons.outlined.WebAsset
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.imcys.bilibilias.BuildConfig
import com.imcys.bilibilias.R
import com.imcys.bilibilias.common.event.sendToastEvent
import com.imcys.bilibilias.common.utils.ASConstant.QQ_CHANNEL_URL
import com.imcys.bilibilias.common.utils.ASConstant.QQ_GROUP_URL
import com.imcys.bilibilias.common.utils.DeviceInfoUtils
import com.imcys.bilibilias.common.utils.openLink
import com.imcys.bilibilias.ui.home.navigation.HomeRoute
import com.imcys.bilibilias.ui.tools.calendar.CalendarRoute
import com.imcys.bilibilias.ui.tools.donate.DonateRoute
import com.imcys.bilibilias.ui.tools.frame.FrameExtractorRoute
import com.imcys.bilibilias.ui.tools.parser.WebParserRoute
import com.imcys.bilibilias.ui.weight.ASAlertDialog
import com.imcys.bilibilias.ui.weight.ASIconButton
import com.imcys.bilibilias.ui.weight.ASTextButton
import com.imcys.bilibilias.ui.weight.tip.ASInfoTip
import kotlinx.coroutines.launch

@Composable
fun ToolsScreen(
    onToPage: (NavKey) -> Unit,
    onUpdateUseToolRecord: (ToolInfo) -> Unit
) {
    Column(
        Modifier
            .padding(horizontal = 15.dp)
            .padding(top = 10.dp),
    ) {
        ToolsContent(
            onToPage = onToPage,
            onUpdateUseToolRecord = onUpdateUseToolRecord
        )
    }
}


enum class ToolInfo(
    val title: String,
    val desc: String,
    val icon: ImageVector? = null,
    val iconRes: Int? = null,
    val navKey: NavKey = HomeRoute(),
    val isScreen: Boolean = true,
) {
    // 逐帧提取
    FrameExtractor(
        title = "逐帧提取",
        desc = "从视频中逐帧提取图片，画手书的好帮手！",
        icon = Icons.Outlined.VideoCameraBack,
        navKey = FrameExtractorRoute
    ),
    WebParser(
        title = "网页解析",
        desc = "直接在网页找到你需要的视频，可自动解析视频。",
        icon = Icons.Outlined.WebAsset,
        navKey = WebParserRoute
    ),
    Calendar(
        title = "追番日历",
        desc = "快速找到本周更新的番剧视频。",
        icon = Icons.Outlined.CalendarToday,
        navKey = CalendarRoute
    ),
    // 反馈问题
    Feedback(
        title = "反馈问题",
        desc = "🐞帮助我们改进程序，这对本项目的发展有重大意义！",
        icon = Icons.Outlined.BugReport,
        isScreen = false,
    ),
    // 捐助我们
    Donate(
        title = "捐助我们",
        desc = "☕请我们喝一杯奶茶吧！",
        iconRes = R.drawable.ic_credit_card_heart_24px,
        navKey = DonateRoute
    ),
}

@Composable
private fun ToolsContent(
    onToPage: (NavKey) -> Unit,
    onUpdateUseToolRecord: (ToolInfo) -> Unit
) {

    var showFeedbackDialog by remember { mutableStateOf(false) }

    val videoTools = listOf(
        ToolInfo.FrameExtractor
    )
    val parserTools = listOf(
        ToolInfo.Calendar,
        ToolInfo.WebParser,
    )
    val otherTools = mutableListOf(
        ToolInfo.Feedback
    ).apply {
        if (!BuildConfig.ENABLED_PLAY_APP_MODE) {
            add(
                ToolInfo.Donate
            )
        }
    }

    // 点击工具处理
    fun clickTool(toolInfo: ToolInfo) {
        onUpdateUseToolRecord(toolInfo)
        when (toolInfo) {
            ToolInfo.Feedback -> {
                showFeedbackDialog = true
            }
            else -> {
                onToPage.invoke(toolInfo.navKey)
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 180.dp),
        modifier = Modifier.padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(
            span = { GridItemSpan(maxLineSpan) }
        ) {
            Text(stringResource(R.string.tools_video_processing))
        }
        items(videoTools, key = { it.name }) {
            ToolCard(it, onClick = {
                clickTool(it)
            })
        }

        item(
            span = { GridItemSpan(maxLineSpan) }
        ) {
            Text(stringResource(R.string.tools_parser_tools))
        }
        items(parserTools, key = { it.name }) {
            ToolCard(it, onClick = {
                clickTool(it)
            })
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(stringResource(R.string.tools_other))
        }

        items(otherTools, key = { it.name }) {
            ToolCard(it, onClick = {
                clickTool(it)
            })
        }

    }

    FeedbackDialog(showFeedbackDialog, onDismiss = {
        showFeedbackDialog = false
    })

}

@Composable
fun FeedbackDialog(showFeedbackDialog: Boolean, onDismiss: () -> Unit) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    ASAlertDialog(
        showState = showFeedbackDialog,
        title = {
            Text(stringResource(R.string.tools_feedback))
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Spacer(Modifier)

                ASInfoTip {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "反馈时需要带上你的设备信息，点击可一键复制。",
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        ASIconButton(onClick = {
                            scope.launch {
                                val copyText = DeviceInfoUtils.getDeviceInfoCopyString(context)
                                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                val clipboard =
                                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("版本信息", copyText)
                                clipboard.setPrimaryClip(clip)
                             sendToastEvent("已复制到剪贴板")
                            }
                        }) {
                            Icon(Icons.Outlined.CopyAll, contentDescription = "复制按钮")
                        }
                    }
                }

                BadgedBox(badge = {
                    Badge { Text(stringResource(R.string.common_recommend)) }
                }) {
                    Surface(
                        shape = CardDefaults.shape,
                        onClick = {
                            context.openLink("https://github.com/1250422131/bilibilias/issues")
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_github_24px),
                                contentDescription = "图标",
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "前往Github反馈，由开发者和社区贡献者处理你的问题。",
                            )
                        }
                    }
                }

                if (!BuildConfig.ENABLED_PLAY_APP_MODE) {
                    Surface(
                        shape = CardDefaults.shape,
                        onClick = {
                            context.openLink(QQ_CHANNEL_URL)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_qq_channel_2px),
                                contentDescription = "图标",
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "前往QQ频道反馈，由用户和开发者解答你的问题，并获得最新的通知。",
                            )
                        }
                    }


                    Surface(
                        shape = CardDefaults.shape,
                        onClick = {
                            context.openLink(QQ_GROUP_URL)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_qq_24px),
                                contentDescription = "图标",
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "二次元爱好者交流群，欢迎加入讨论与交流！",
                            )
                        }
                    }
                }


            }
        },
        onDismiss = onDismiss,
        confirmButton = {
            ASTextButton(onClick = {
                onDismiss.invoke()
            }) {
                Text(stringResource(R.string.common_ok))
            }
        }

    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun ToolCard(
    toolInfo: ToolInfo = ToolInfo.Feedback,
    onClick: () -> Unit = { }
) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = CardDefaults.shape, onClick = onClick) {
        Column(
            Modifier
                .padding(10.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialShapes.Circle.toShape()
            ) {
                toolInfo.icon?.let {
                    Icon(
                        it,
                        contentDescription = "图标",
                        modifier = Modifier
                            .padding(8.dp)
                            .size(22.dp)
                    )
                } ?: run {
                    Icon(
                        painter = painterResource(toolInfo.iconRes!!),
                        contentDescription = "图标",
                        modifier = Modifier
                            .padding(8.dp)
                            .size(22.dp)
                    )
                }

            }
            Spacer(Modifier.height(2.dp))
            Text(toolInfo.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                toolInfo.desc,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                minLines = 2,
            )
        }
    }
}