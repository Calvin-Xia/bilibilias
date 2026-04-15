package com.imcys.bilibilias.ui.tools.calendar.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey
import com.imcys.bilibilias.network.ApiStatus
import com.imcys.bilibilias.network.NetWorkResult
import com.imcys.bilibilias.network.model.bgm.next.BgmNextSubject
import com.imcys.bilibilias.ui.weight.ASAsyncImage
import com.imcys.bilibilias.ui.weight.ASTopAppBar
import com.imcys.bilibilias.ui.weight.AsBackIconButton
import com.imcys.bilibilias.ui.weight.BILIBILIASTopAppBarStyle
import com.imcys.bilibilias.ui.weight.shimmer.shimmer
import com.imcys.bilibilias.weight.ASCollapsingToolbar
import com.imcys.bilibilias.weight.ASPrimaryTabRow
import com.imcys.bilibilias.weight.AsAutoError
import com.imcys.bilibilias.weight.animateIndicatorWithPager
import com.imcys.bilibilias.weight.asTabIndicatorClip
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
@Immutable
data class SubjectDetailRoute(
    val subjectId: Long
) : NavKey

@Composable
fun SubjectDetailScreen(subjectDetailRoute: SubjectDetailRoute, onToBack: () -> Unit) {
    val vm = koinViewModel<SubjectDetailViewModel>()
    val subtitleData by vm.subjectData.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    LaunchedEffect(subjectDetailRoute.subjectId) {
        vm.loadSubjectDetail(subjectDetailRoute.subjectId)
    }

    var minHeightPx by remember { mutableFloatStateOf(0f) }
    var appBarAlpha by remember { mutableFloatStateOf(0f) }

    val animatedAppBarColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = appBarAlpha),
        label = "appBarColorAnimation"
    )
    val minHeightDp = remember(minHeightPx) {
        with(density) { minHeightPx.toDp() }
    }

    SubjectDetailScaffold(
        onToBack = onToBack,
        title = subtitleData.data?.nameCN,
        showTitle = appBarAlpha > 0.8,
        appBarColor = animatedAppBarColor,
        onMinHeightCalculated = { minHeightPx = it },
    ) {
        AsAutoError(subtitleData, onSuccessContent = {
            SubjectDetailContent(subtitleData, minHeightDp, onChangeProgress = {
                appBarAlpha = it
            })
        })

    }
}

enum class SubjectDetailPage {
    Detail,
    VideoList;

    companion object {
        val count = entries.size
        fun fromIndex(index: Int): SubjectDetailPage = entries.getOrElse(index) { Detail }
    }
}

@Composable
private fun SubjectDetailContent(
    subtitleData: NetWorkResult<BgmNextSubject>,
    topImageHeight: Dp,
    onChangeProgress: (progress: Float) -> Unit = {},
) {
    val pagerState = rememberPagerState { 2 }
    ASCollapsingToolbar(
        maxHeight = 400.dp,
        minHeight = topImageHeight,
        onChangeProgress = onChangeProgress,
        toolbar = { SubjectDetailToolbar(subtitleData) }
    ) { nestedScrollConnection->
        ASPrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier
                        .animateIndicatorWithPager(pagerState)
                        .asTabIndicatorClip(),
                    width = Dp.Unspecified,
                )
            }
        ) {
            Tab(
                onClick = {},
                text = { Text("详情") },
                selected = false
            )
            Tab(
                onClick = {},
                text = { Text("视频") },
                selected = false
            )
        }
        HorizontalPager(
            modifier = Modifier.weight(1f),
            state = pagerState
        ) {
           Column(Modifier.fillMaxSize()) {
               when (SubjectDetailPage.fromIndex(it)) {
                   SubjectDetailPage.Detail -> SubjectDetailContentPage(subtitleData,nestedScrollConnection)
                   SubjectDetailPage.VideoList -> SubjectDetailContentVideoListPage()
               }
           }
        }
    }
}

@Composable
private fun SubjectDetailContentVideoListPage() {

}

@Composable
private fun SubjectDetailContentPage(
    subtitleData: NetWorkResult<BgmNextSubject>,
    nestedScrollConnection: NestedScrollConnection,
) {
    val isNetLoad by remember(subtitleData.status) { mutableStateOf(subtitleData.status != ApiStatus.SUCCESS) }

    Column(
        Modifier
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
            .nestedScroll(nestedScrollConnection)
            .fillMaxSize()
    ) {
        FlowRow(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            maxItemsInEachRow = 2
        ) {
            StatCard(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .shimmer(isNetLoad),
                title = "评分",
                value = subtitleData.data?.rating?.score?.toString() ?: "暂无",
                unit = if ((subtitleData.data?.rating?.score ?: 0.0) > 0) "/10" else null,
                icon = Icons.Filled.Star,
                iconTint = MaterialTheme.colorScheme.primary,
                titleColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                valueColor = MaterialTheme.colorScheme.onSurface,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )

            StatCard(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .shimmer(isNetLoad),
                title = "排名",
                value = subtitleData.data?.rating?.rank?.toString() ?: "暂无",
                icon = Icons.Filled.EmojiEvents,
                iconTint = MaterialTheme.colorScheme.tertiary,
                titleColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                valueColor = MaterialTheme.colorScheme.onSurface,
                containerColor = MaterialTheme.colorScheme.surface
            )
            StatCard(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .shimmer(isNetLoad),
                title = "参评",
                unit = "人",
                value = subtitleData.data?.rating?.total?.toString() ?: "暂无",
                icon = Icons.Outlined.Edit,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                titleColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                valueColor = MaterialTheme.colorScheme.onSurface,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                border = true
            )

            StatCard(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .shimmer(isNetLoad),
                title = "订阅",
                value = "${subtitleData.data?.collection?.values?.sumOf { it }}",
                icon = Icons.Outlined.Notifications,
                iconTint = MaterialTheme.colorScheme.onPrimary,
                titleColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                valueColor = MaterialTheme.colorScheme.onPrimary,
                containerColor = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(20.dp))
        Text("简介", fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(10.dp))
        Text(
            subtitleData.data?.summary ?: "简而言之，燕儿简直",
            modifier = Modifier.shimmer(isNetLoad)
        )


    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String? = null,
    icon: ImageVector,
    iconTint: Color,
    titleColor: Color,
    valueColor: Color,
    containerColor: Color,
    border: Boolean = false,
) {
    Surface(
        shape = CardDefaults.shape,
        color = containerColor,
        modifier = modifier,
        border = if (border) CardDefaults.outlinedCardBorder() else null
    ) {
        Column(
            Modifier
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    title,
                    fontSize = 11.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
                Icon(
                    icon,
                    contentDescription = "评分", modifier = Modifier.size(20.dp),
                    tint = iconTint
                )
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    value,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = valueColor
                )

                unit?.let {
                    Text(
                        unit,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = valueColor
                    )
                }

            }
        }
    }
}

@Composable
private fun SubjectDetailToolbar(
    subtitleData: NetWorkResult<BgmNextSubject>,
) {
    Box(Modifier.shimmer(subtitleData.status != ApiStatus.SUCCESS)) {
        ASAsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            contentDescription = "封面",
            model = subtitleData.data?.images?.large
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        startY = Float.POSITIVE_INFINITY,
                        endY = 0f
                    )
                )
        )


        Text(
            text = subtitleData.data?.nameCN ?: "",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 40.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            ),
            color = Color(0xff2e2e34),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 20.dp, end = 10.dp)
        )

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectDetailScaffold(
    onToBack: () -> Unit,
    title: String?,
    showTitle: Boolean = false,
    appBarColor: Color,
    onMinHeightCalculated: (Float) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            ASTopAppBar(
                style = BILIBILIASTopAppBarStyle.Small,
                title = {
                    AnimatedVisibility(showTitle) {
                        Text(title ?: "")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appBarColor,
                ),
                navigationIcon = { AsBackIconButton(onClick = onToBack) }
            )
        },
    ) {
        val density = LocalDensity.current
        val minHeight = with(density) { it.calculateTopPadding().toPx() }
        LaunchedEffect(minHeight) {
            onMinHeightCalculated(minHeight)
        }
        content.invoke(it)
    }
}