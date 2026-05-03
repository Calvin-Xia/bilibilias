package com.imcys.bilibilias.ui.setting.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.imcys.bilibilias.ui.weight.ASTopAppBar
import com.imcys.bilibilias.ui.weight.AsBackIconButton
import com.imcys.bilibilias.ui.weight.BILIBILIASTopAppBarStyle
import com.imcys.bilibilias.weight.maybeNestedScroll
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

@Serializable
data object DownloadConfigRoute : NavKey

@Composable
fun DownloadConfigScreen(
    route: DownloadConfigRoute,
    onToBack: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    DownloadConfigScaffold(scrollBehavior, onToBack = onToBack) {
        DownloadConfigContent(
            Modifier
                .padding(it)
                .maybeNestedScroll(scrollBehavior)
        )
    }
}

@Composable
private fun DownloadConfigContent(modifier: Modifier = Modifier) {
    val vm = koinViewModel<DownloadConfigViewModel>()
    val appSettings by vm.appSettings.collectAsStateWithLifecycle(initialValue = null)
    val maxSupported by vm.maxSupportedConcurrentDownloads.collectAsStateWithLifecycle()
    val concurrentMergeEnabled = appSettings?.enabledConcurrentMerge == true
    val sliderMax = if (concurrentMergeEnabled) {
        maxSupported
    } else {
        DownloadConfigViewModel.MAX_CONCURRENT_DOWNLOADS_WITH_SERIAL_MERGE
    }
    val currentValue = appSettings?.maxConcurrentDownloads?.coerceIn(1, sliderMax) ?: 1
    val canEnableConcurrentMerge = currentValue > 1
    var sliderValue by remember(currentValue, sliderMax) {
        mutableFloatStateOf(currentValue.toFloat())
    }
    LaunchedEffect(currentValue, sliderMax) {
        sliderValue = currentValue.coerceIn(1, sliderMax).toFloat()
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface (shape = CardDefaults.shape){
            Column {
                DownloadConfigSummaryContent(
                    currentValue = currentValue,
                    maxSupported = maxSupported,
                    sliderMax = sliderMax,
                    concurrentMergeEnabled = concurrentMergeEnabled
                )

                DownloadConfigSliderContent(
                    sliderValue = sliderValue,
                    maxSupported = sliderMax,
                    concurrentMergeEnabled = concurrentMergeEnabled,
                    onValueChange = {
                        sliderValue = it.roundToInt().coerceIn(1, sliderMax).toFloat()
                    },
                    onValueChangeFinished = {
                        vm.updateMaxConcurrentDownloads(sliderValue.roundToInt())
                    }
                )

                DownloadConcurrentMergeCard(
                    checked = concurrentMergeEnabled,
                    enabled = canEnableConcurrentMerge,
                    currentValue = currentValue,
                    maxSupported = maxSupported,
                    onCheckedChange = vm::updateEnabledConcurrentMerge
                )
            }
        }
        DownloadDirectoryCard()
    }
}

@Composable
private fun DownloadConfigSummaryContent(
    currentValue: Int,
    maxSupported: Int,
    sliderMax: Int,
    concurrentMergeEnabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "同时下载任务数",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "根据设备性能动态限制上限，尽量在速度和稳定性之间保持平衡。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DownloadStatPill(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Cloud,
                label = "当前值",
                value = "$currentValue 个"
            )
            DownloadStatPill(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Memory,
                label = if (concurrentMergeEnabled) "建议上限" else "当前可调上限",
                value = "${if (concurrentMergeEnabled) maxSupported else sliderMax} 个"
            )
        }
    }
}

@Composable
private fun DownloadConfigSliderContent(
    sliderValue: Float,
    maxSupported: Int,
    concurrentMergeEnabled: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "并行数量调节",
                style = MaterialTheme.typography.titleMedium
            )

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "当前选择",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "${sliderValue.roundToInt()} 个任务",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Slider(
                        value = sliderValue,
                        valueRange = 1f..maxSupported.toFloat(),
                        steps = (maxSupported - 2).coerceAtLeast(0),
                        onValueChange = onValueChange,
                        onValueChangeFinished = onValueChangeFinished
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "1",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = maxSupported.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadConcurrentMergeCard(
    checked: Boolean,
    enabled: Boolean,
    currentValue: Int,
    maxSupported: Int,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = onCheckedChange
                )
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (enabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountTree,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(22.dp),
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "并发合并",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (enabled) {
                        if (checked) {
                            "关闭后，将支持更高的并发下载数。"
                        } else {
                            "开启后，FFmpeg 并发数会跟随当前并发下载数。"
                        }
                    } else {
                        "仅当并发下时可开启。"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = null
            )
        }
    }
}

@Composable
private fun DownloadDirectoryCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(22.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "缓存目录",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Download/BILIBILIAS",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}


@Composable
private fun DownloadStatPill(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadConfigScaffold(
    scrollBehavior: TopAppBarScrollBehavior,
    onToBack: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
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
                title = { Text(text = "下载配置") },
                navigationIcon = {
                    AsBackIconButton(onClick = {
                        onToBack.invoke()
                    })
                },
                alwaysDisplay = false
            )
        },
    ) {
        content(it)
    }
}
