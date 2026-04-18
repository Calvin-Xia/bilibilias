package com.imcys.bilibilias.ui.tools.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation3.runtime.NavKey
import com.imcys.bilibilias.common.utils.toHttps
import com.imcys.bilibilias.network.ApiStatus
import com.imcys.bilibilias.ui.user.UserViewModel
import com.imcys.bilibilias.ui.weight.ASAsyncImage
import com.imcys.bilibilias.ui.weight.ASTopAppBar
import com.imcys.bilibilias.ui.weight.AsBackIconButton
import com.imcys.bilibilias.ui.weight.BILIBILIASTopAppBarStyle
import com.imcys.bilibilias.ui.weight.shimmer.shimmer
import com.imcys.bilibilias.weight.ASColumnAutoFolding
import com.imcys.bilibilias.weight.ASPrimaryTabRow
import com.imcys.bilibilias.weight.AsAutoError
import com.imcys.bilibilias.weight.animateIndicatorWithPager
import com.imcys.bilibilias.weight.asTabIndicatorClip
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel


@Serializable
object CalendarRoute : NavKey


@Composable
fun CalendarScreen(
    calendarRoute: CalendarRoute,
    onToSubjectDetail: (Long) -> Unit,
    onToBack: () -> Unit
) {
    CalendarScaffold(onToBack = onToBack) {
        CalendarContent(
            modifier = Modifier.padding(it),
            onToSubjectDetail
        )
    }
}

@Composable
private fun CalendarContent(modifier: Modifier, onToSubjectDetail: (Long) -> Unit) {
    var showTabRow by remember { mutableStateOf(true) }
    val vm = koinViewModel<CalendarViewModel>()
    val calendarData by vm.calendarData.collectAsStateWithLifecycle()
    val selectedWeekIndex by vm.selectedWeekIndex.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = selectedWeekIndex, pageCount = { calendarData.data?.size ?: 0 })
    val pagerScope = rememberCoroutineScope()

    LaunchedEffect(calendarData.status, selectedWeekIndex) {
        val calendarSize = calendarData.data?.size ?: return@LaunchedEffect
        if (calendarData.status == ApiStatus.SUCCESS && calendarSize > 0) {
            val targetPage = selectedWeekIndex.coerceIn(0, calendarSize - 1)
            if (pagerState.currentPage != targetPage) {
                pagerState.scrollToPage(targetPage)
            }
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            vm.updateSelectedWeekIndex(page)
        }
    }



    Column(modifier) {
        AsAutoError(
            calendarData,
            onLoadingContent = {
                CalendarLoadingScreen()
            },
            onSuccessContent = {
                ASColumnAutoFolding(Modifier.fillMaxHeight(), onChangeShow = {
                    showTabRow = it
                }) {
                    AnimatedVisibility(visible = showTabRow) {
                        ASPrimaryTabRow(
                            selectedTabIndex = pagerState.currentPage,
                            indicator = {
                                TabRowDefaults.PrimaryIndicator(
                                    modifier = Modifier
                                        .animateIndicatorWithPager(pagerState)
                                        .asTabIndicatorClip(),
                                    width = Dp.Unspecified,
                                )
                            }
                        ) {
                            calendarData.data?.forEachIndexed { index, calendar ->
                                Tab(
                                    onClick = {
                                        pagerScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    },
                                    text = { Text(calendar.weekDayCn.takeLast(1)) },
                                    selected = pagerState.currentPage == index
                                )
                            }
                        }
                    }
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .weight(1f),
                    ) { page ->
                        LazyVerticalGrid(
                            modifier = Modifier
                                .padding(top = 10.dp, start = 10.dp, end = 10.dp)
                                .fillMaxSize(),
                            columns = GridCells.Fixed(1),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            val items = calendarData.data?.get(page)?.items ?: emptyList()
                            items(
                                items,
                                key = { it.subject.id }) {
                                CalendarDonghua(
                                    title = it.subject.nameCN.ifEmpty { it.subject.name },
                                    detail = it.subject.info,
                                    rank = it.subject.rating?.rank ?: 0,
                                    scoreNumber = it.subject.rating?.total ?: 0,
                                    imgUrl = it.subject.images?.large?.toHttps() ?: "",
                                    score = it.subject.rating?.score ?: .0,
                                    onClick = {
                                        onToSubjectDetail(it.subject.id)
                                    }
                                )
                            }
                        }
                    }
                }
            })
    }
}


@Preview
@Composable
private fun CalendarLoadingScreen() {
    Column {
        ASPrimaryTabRow(
            selectedTabIndex = 0,
            ) {
            val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")
            weekDays.forEachIndexed { index, day ->
                Tab(
                    selected = false,
                    onClick = {},
                    text = { Text(day) }
                )
            }
        }

        LazyVerticalGrid(
            modifier = Modifier
                .padding(top = 10.dp, start = 10.dp, end = 10.dp)
                .weight(1f),
            columns = GridCells.Fixed(1),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(10) {
                CalendarDonghua(isLoading = true)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarScaffold(
    onToBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            Column {
                ASTopAppBar(
                    style = BILIBILIASTopAppBarStyle.Small,
                    title = { Text(text = "放送列表") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    navigationIcon = { AsBackIconButton(onClick = onToBack) }
                )
            }
        },
    ) {
        content.invoke(it)
    }
}


@Preview
@Composable
private fun CalendarDonghua(
    modifier: Modifier = Modifier,
    title: String = "番剧捏",
    score: Double = 10.0,
    scoreNumber: Int = 10,
    rank: Int = 1,
    imgUrl: String = "",
    detail: String = "没有未来的未来，不是我想要的未来",
    onClick: () -> Unit = {},
    isLoading: Boolean = false,
) {

    Surface(
        modifier = modifier.height(200.dp),
        shape = CardDefaults.shape,
        color = MaterialTheme.colorScheme.surface,
        onClick = onClick
    ) {
        Row(Modifier.padding(12.dp)) {

            ASAsyncImage(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.3f)
                    .shimmer(isLoading),
                model = imgUrl, shape = CardDefaults.shape, contentDescription = "封面"
            )

            Spacer(modifier = Modifier.width(12.dp))
            Column(
                Modifier
                    .fillMaxHeight()
                    .weight(0.6f)
            ) {
                Text(
                    title,
                    fontSize = 20.sp, maxLines = 3, fontWeight = FontWeight.Bold,
                    modifier = Modifier.shimmer(isLoading),
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    detail,
                    fontSize = 14.sp, maxLines = 3, fontWeight = FontWeight.Medium,
                    modifier = Modifier.shimmer(isLoading),
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.weight(1f))

                Row(
                    verticalAlignment = Alignment.Bottom,
                ) {
                    if (score > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.shimmer(isLoading)
                        ) {
                            Icon(
                                Icons.Outlined.Star,
                                contentDescription = "评分",
                                Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.width(5.dp))

                            Text(
                                score.toString(), fontSize = 12.sp,
                                lineHeight = 12.sp,
                                modifier = Modifier
                            )
                        }
                    }
                    Spacer(Modifier.width(5.dp))
                    if (rank > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.shimmer(isLoading)
                        ) {
                            Image(
                                imageVector = Icons.Outlined.Leaderboard,
                                contentDescription = "排名",
                                modifier = Modifier.size(14.dp),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onTertiaryContainer)
                            )

                            Spacer(Modifier.width(5.dp))

                            Text(
                                text = "Rank $rank",
                                fontSize = 12.sp,
                                lineHeight = 12.sp,
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (scoreNumber > 0) {
                        Text(
                            text = "${scoreNumber}人评分",
                            fontSize = 13.sp,
                            lineHeight = 13.sp,
                            modifier = Modifier.shimmer(isLoading)
                        )
                    }
                }
            }
        }
    }

}
