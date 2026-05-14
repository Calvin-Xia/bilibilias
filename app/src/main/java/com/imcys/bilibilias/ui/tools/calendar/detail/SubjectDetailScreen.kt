package com.imcys.bilibilias.ui.tools.calendar.detail

import android.annotation.SuppressLint
import androidx.annotation.IdRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.imcys.bilibilias.common.uimodel.DonghuaPlayPlatform
import com.imcys.bilibilias.common.uimodel.DonghuaPlayTV
import com.imcys.bilibilias.common.uimodel.playProgramList
import com.imcys.bilibilias.common.uimodel.playTVList
import com.imcys.bilibilias.common.utils.firebase.FirebaseExt.logOpenSubjectDetail
import com.imcys.bilibilias.network.ApiStatus
import com.imcys.bilibilias.network.NetWorkResult
import com.imcys.bilibilias.network.model.bgm.BgmEpisodeList
import com.imcys.bilibilias.network.model.bgm.next.BgmNextEpisodesComment
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
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
@Immutable
data class SubjectDetailRoute(
    val subjectId: Long
) : NavKey

@Composable
fun SubjectDetailScreen(
    subjectDetailRoute: SubjectDetailRoute,
    onToBack: () -> Unit,
) {
    val vm = koinViewModel<SubjectDetailViewModel>()
    val subtitleData by vm.subjectData.collectAsStateWithLifecycle()
    val episodesData by vm.episodesData.collectAsStateWithLifecycle()
    val episodeComments by vm.episodeComments.collectAsStateWithLifecycle()
    val selectedEpId by vm.selectedEpId.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    LaunchedEffect(subjectDetailRoute.subjectId) {
        vm.loadSubjectDetail(subjectDetailRoute.subjectId)
        logOpenSubjectDetail(subjectDetailRoute.subjectId)
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
        AsAutoError(
            netWorkResult = subtitleData,
            onSuccessContent = {
                SubjectDetailContent(
                    subtitleData = subtitleData,
                    episodesData = episodesData,
                    episodeComments = episodeComments,
                    selectedEpId = selectedEpId,
                    subjectId = subjectDetailRoute.subjectId,
                    topImageHeight = minHeightDp,
                    onRefresh = { vm.loadSubjectDetail(subjectDetailRoute.subjectId) },
                    onSelectEpisode = vm::selectEpisode,
                    onChangeProgress = {
                        appBarAlpha = it
                    }
                )
            }
        )

    }
}

enum class SubjectDetailPage {
    Detail,
    Episodes,
    VideoList;

    companion object {
        val count = entries.size
        fun fromIndex(index: Int): SubjectDetailPage = entries.getOrElse(index) { Detail }
    }
}

@Composable
private fun SubjectDetailContent(
    subtitleData: NetWorkResult<BgmNextSubject>,
    episodesData: NetWorkResult<BgmEpisodeList>,
    episodeComments: NetWorkResult<List<BgmNextEpisodesComment>>,
    selectedEpId: Long,
    subjectId: Long,
    topImageHeight: Dp,
    onRefresh: () -> Unit,
    onSelectEpisode: (Long) -> Unit,
    onChangeProgress: (progress: Float) -> Unit = {},
) {
    val pagerState = rememberPagerState { 3 }
    val pagerScope = rememberCoroutineScope()
    val detailPageScrollState = rememberSaveable(subjectId, saver = ScrollState.Saver) {
        ScrollState(initial = 0)
    }
    var isRefreshing by remember(subtitleData.status) {
        mutableStateOf(
            subtitleData.status == ApiStatus.LOADING
        )
    }
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
    ) {
        ASCollapsingToolbar(
            maxHeight = 400.dp,
            minHeight = topImageHeight,
            onChangeProgress = onChangeProgress,
            toolbar = { SubjectDetailToolbar(subtitleData) }
        ) { nestedScrollConnection ->
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
                    onClick = {
                        pagerScope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    text = { Text("详情") },
                    selected = false
                )
                Tab(
                    onClick = {
                        pagerScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    text = { Text("评论") },
                    selected = false
                )
                Tab(
                    onClick = {
                        pagerScope.launch {
                            pagerState.animateScrollToPage(2)
                        }
                    },
                    text = { Text("视频") },
                    selected = false
                )
            }
            HorizontalPager(
                modifier = Modifier.weight(1f),
                state = pagerState,
            ) {
                Column(Modifier.fillMaxSize()) {
                    when (SubjectDetailPage.fromIndex(it)) {
                        SubjectDetailPage.Detail -> SubjectDetailContentPage(
                            subtitleData = subtitleData,
                            nestedScrollConnection = nestedScrollConnection,
                            scrollState = detailPageScrollState
                        )

                        SubjectDetailPage.VideoList -> SubjectDetailContentVideoListPage()
                        SubjectDetailPage.Episodes -> SubjectEpisodesPage(
                            episodesData = episodesData,
                            episodeComments = episodeComments,
                            selectedEpId = selectedEpId,
                            onSelectEpisode = onSelectEpisode,
                            nestedScrollConnection = nestedScrollConnection,
                        )
                    }
                }
            }
        }
    }

}

@Composable
private fun ColumnScope.SubjectEpisodesPage(
    episodesData: NetWorkResult<BgmEpisodeList>,
    episodeComments: NetWorkResult<List<BgmNextEpisodesComment>>,
    selectedEpId: Long,
    onSelectEpisode: (Long) -> Unit,
    nestedScrollConnection: NestedScrollConnection
) {
    val haptics = LocalHapticFeedback.current

    AsAutoError(episodesData, onSuccessContent = {
        val episodes = episodesData.data?.data.orEmpty()
        val selectedEpisode = episodes.firstOrNull { it.id == selectedEpId } ?: episodes.firstOrNull()

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .nestedScroll(nestedScrollConnection),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "章节",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "共 ${episodes.size} 章",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    selectedEpisode?.let { episode ->
                        Text(
                            text = "当前：第${episode.ep}章 ${episode.nameCn.ifBlank { episode.name }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        episodes.forEach { episode ->
                            FilterChip(
                                selected = episode.id == selectedEpisode?.id,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                    onSelectEpisode(episode.id)
                                },
                                label = {
                                    Text("第${episode.ep}章")
                                }
                            )
                        }
                    }
                }
            }
            item {
                EpisodeCommentsHeader(
                    title = selectedEpisode?.let { "第${it.ep}章评论" } ?: "评论",
                    count = episodeComments.data?.size
                )
            }
            episodeCommentsItems(episodeComments)
        }
    })
}

private fun LazyListScope.episodeCommentsItems(
    episodeComments: NetWorkResult<List<BgmNextEpisodesComment>>
) {
    when (episodeComments.status) {
        ApiStatus.LOADING -> {
            items(3, key = { "comment-placeholder-$it" }) {
                CommentPlaceholder()
            }
        }

        ApiStatus.SUCCESS -> {
            val comments = episodeComments.data.orEmpty()
            if (comments.isEmpty()) {
                item(key = "empty-comments") {
                    EmptyComments()
                }
            } else {
                items(
                    items = comments,
                    key = { comment -> comment.id }
                ) { comment ->
                    EpisodeCommentItem(comment = comment)
                }
            }
        }

        ApiStatus.ERROR,
        ApiStatus.DEFAULT -> {
            item(key = "comments-state") {
                AsAutoError(
                    netWorkResult = episodeComments,
                    onSuccessContent = {
                        CommentPlaceholder()
                    }
                )
            }
        }
    }
}

@Composable
private fun EpisodeCommentsHeader(title: String, count: Int?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.ChatBubbleOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        count?.let {
            Text(
                text = "$it 条",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EpisodeCommentItem(comment: BgmNextEpisodesComment) {
    var repliesExpanded by rememberSaveable(comment.id) { mutableStateOf(false) }
    val visibleReplies = if (repliesExpanded) comment.replies else comment.replies.take(2)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CardDefaults.shape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .animateContentSize()
        ) {
            CommentAuthorRow(
                avatar = comment.user.avatar.medium.ifBlank { comment.user.avatar.small },
                nickname = comment.user.nickname.ifBlank { comment.user.username },
                content = comment.content,
                avatarSize = 42.dp
            )
            AnimatedVisibility(comment.replies.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .padding(start = 50.dp, top = 10.dp)
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = CardDefaults.shape
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    visibleReplies.forEach { reply ->
                        ReplyCommentItem(reply = reply)
                    }
                    if (comment.replies.size > 2) {
                        TextButton(
                            onClick = { repliesExpanded = !repliesExpanded },
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (repliesExpanded) {
                                    Icons.Filled.ExpandLess
                                } else {
                                    Icons.Filled.ExpandMore
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(if (repliesExpanded) "收起回复" else "展开 ${comment.replies.size - 2} 条回复")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplyCommentItem(reply: BgmNextEpisodesComment.Reply) {
    CommentAuthorRow(
        avatar = reply.user.avatar.medium.ifBlank { reply.user.avatar.small },
        nickname = reply.user.nickname.ifBlank { reply.user.username },
        content = reply.content,
        avatarSize = 30.dp
    )
}

@Composable
private fun CommentAuthorRow(
    avatar: String,
    nickname: String,
    content: String,
    avatarSize: Dp,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        ASAsyncImage(
            model = avatar,
            contentDescription = nickname,
            shape = CircleShape,
            modifier = Modifier.size(avatarSize)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = nickname,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            CommentContent(content = content)
        }
    }
}

@Composable
private fun CommentContent(content: String) {
    val segments = remember(content) {
        BgmCommentContentParser.parse(content)
    }
    val density = LocalDensity.current

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        var segmentIndex = 0
        while (segmentIndex < segments.size) {
            when (val segment = segments[segmentIndex]) {
                is BgmCommentContentSegment.Text -> {
                    val textSegments = mutableListOf<BgmCommentContentSegment.Text>()
                    while (segmentIndex < segments.size) {
                        val textSegment = segments[segmentIndex] as? BgmCommentContentSegment.Text
                            ?: break
                        textSegments += textSegment
                        segmentIndex++
                    }
                    CommentTextContent(textSegments)
                }

                is BgmCommentContentSegment.Image -> {
                    val imageModifier = remember(segment, density) {
                        if (segment.widthPx != null && segment.heightPx != null) {
                            with(density) {
                                Modifier
                                    .width(segment.widthPx.toDp())
                                    .height(segment.heightPx.toDp())
                            }
                        } else {
                            Modifier
                                .fillMaxWidth()
                        }
                    }

                    ASAsyncImage(
                        model = segment.url,
                        contentDescription = "评论图片",
                        shape = CardDefaults.shape,
                        modifier = imageModifier
                    )
                    segmentIndex++
                }
            }
        }
    }
}

@Composable
private fun CommentTextContent(segments: List<BgmCommentContentSegment.Text>) {
    val defaultTextColor = MaterialTheme.colorScheme.onSurface
    val hiddenBackgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest
    var revealedMaskIds by rememberSaveable(segments) { mutableStateOf(emptyList<Int>()) }
    val revealedMaskSet = remember(revealedMaskIds) { revealedMaskIds.toSet() }
    val annotatedText = remember(
        segments,
        defaultTextColor,
        hiddenBackgroundColor,
        revealedMaskSet
    ) {
        buildAnnotatedString {
            var maskId = 0
            segments.forEach { segment ->
                val textColor = segment.color ?: defaultTextColor
                val baseStyle = SpanStyle(
                    color = textColor,
                    fontStyle = if (segment.italic) FontStyle.Italic else FontStyle.Normal
                )
                if (!segment.masked) {
                    withStyle(baseStyle) {
                        append(segment.value)
                    }
                } else {
                    val currentMaskId = maskId++
                    if (currentMaskId in revealedMaskSet) {
                        withStyle(baseStyle) {
                            append(segment.value)
                        }
                    } else {
                        val hiddenStyle = baseStyle.copy(
                            color = Color.Transparent,
                            background = hiddenBackgroundColor
                        )
                        val revealedStyle = baseStyle.copy(
                            background = Color.Transparent
                        )
                        withLink(
                            LinkAnnotation.Clickable(
                                tag = "comment-mask-$currentMaskId",
                                styles = TextLinkStyles(
                                    style = hiddenStyle,
                                    hoveredStyle = revealedStyle,
                                    pressedStyle = revealedStyle
                                ),
                                linkInteractionListener = {
                                    revealedMaskIds = if (currentMaskId in revealedMaskSet) {
                                        revealedMaskIds.filterNot { id -> id == currentMaskId }
                                    } else {
                                        revealedMaskIds + currentMaskId
                                    }
                                }
                            )
                        ) {
                            append(segment.value)
                        }
                    }
                }
            }
        }
    }

    BasicText(
        text = annotatedText,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CommentPlaceholder() {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CardDefaults.shape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shimmer(true)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = CircleShape,
                modifier = Modifier.size(42.dp)
            ) {}
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = CardDefaults.shape,
                    modifier = Modifier
                        .width(120.dp)
                        .height(16.dp)
                ) {}
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = CardDefaults.shape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {}
            }
        }
    }
}

@Composable
private fun EmptyComments() {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CardDefaults.shape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "这一章还没有评论",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(18.dp)
        )
    }
}

@Composable
private fun SubjectDetailContentVideoListPage() {

}

@Composable
private fun SubjectDetailContentPage(
    subtitleData: NetWorkResult<BgmNextSubject>,
    nestedScrollConnection: NestedScrollConnection,
    scrollState: ScrollState,
) {
    val isNetLoad = subtitleData.status != ApiStatus.SUCCESS
    var expandedSummary by rememberSaveable { mutableStateOf(false) }
    val playPlatform by remember(subtitleData.data) {
        mutableStateOf(
            subtitleData.data?.infobox?.firstOrNull { it.key == "在线播放平台" }?.values?.mapNotNull { platformStr ->
                DonghuaPlayPlatform.entries.firstOrNull { platform ->
                    platformStr.v.contains(platform.name, ignoreCase = true)
                }
            }
        )
    }
    val playTv by remember(subtitleData.data) {
        mutableStateOf(
            subtitleData.data?.infobox?.asSequence()
                ?.filter { it.key == "播放电视台" || it.key == "其他电视台" }?.flatMap { tvList ->
                    tvList.values
                }?.flatMap {
                    playTVList.filter { platform ->
                        it.v.contains(platform.name, ignoreCase = true)
                    }
                }?.toList()
        )
    }
    val playProgram by remember(subtitleData.data) {
        mutableStateOf(
            subtitleData.data?.infobox?.asSequence()
                ?.filter { it.key == "播放电视台" || it.key == "其他电视台" }?.flatMap { tvList ->
                    tvList.values
                }?.flatMap {
                    playProgramList.filter { platform ->
                        it.v.contains(platform.name, ignoreCase = true)
                    }
                }?.toList()
        )
    }

    Column(
        Modifier
            .padding(20.dp)
            .verticalScroll(scrollState)
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
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = CardDefaults.shape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Box(Modifier) {
                    Text(
                        subtitleData.data?.summary ?: "简而言之，燕儿简直",
                        modifier = Modifier
                            .fillMaxWidth()
                            .shimmer(isNetLoad)
                            .animateContentSize(),
                        maxLines = if (expandedSummary) Int.MAX_VALUE else 5,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!isNetLoad) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .run {
                                    if (expandedSummary) this else {
                                        background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    MaterialTheme.colorScheme.surfaceContainerLow.copy(
                                                        alpha = 0.85f
                                                    ),
                                                    MaterialTheme.colorScheme.surfaceContainerLow
                                                )
                                            ),
                                            shape = CardDefaults.shape
                                        )
                                    }
                                }
                                .padding(start = 32.dp)
                        ) {
                            TextButton(
                                onClick = { expandedSummary = !expandedSummary },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (!expandedSummary) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                                    contentDescription = null
                                )
                                Text(if (!expandedSummary) "更多" else "收起")
                            }
                        }
                    }
                }
            }
        }
        FlowRow(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            subtitleData.data?.metaTags?.forEach {
                SuggestionChip(onClick = {}, label = {
                    Text(it)
                })
            }
        }
        Spacer(Modifier.height(16.dp))
        PlayTvSection(title = "频道", items = playTv ?: emptyList())
        PlayTvSection(title = "档期", items = playProgram ?: emptyList())
        PlayPlatformSection(title = "平台", items = playPlatform ?: emptyList())


    }
}

@Composable
private fun PlayTvSection(
    title: String,
    items: List<DonghuaPlayTV>,
) {
    AnimatedVisibility(items.isNotEmpty()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 4.dp)
            ) {
                items(items) { item ->
                    if (item.iconResId != null) {
                        LogoBadge(
                            name = item.name,
                            iconResId = item.iconResId
                        )
                    } else {
                        SuggestionChip(onClick = {}, label = { Text(item.name) })
                    }
                }
            }
        }
    }
    if (items.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun PlayPlatformSection(
    title: String,
    items: List<DonghuaPlayPlatform>,
) {
    AnimatedVisibility(items.isNotEmpty()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 4.dp)
            ) {
                items(items) { item ->
                    if (item.iconResId != null) {
                        PlatformBadge(
                            name = item.name,
                            iconResId = item.iconResId
                        )
                    } else {
                        SuggestionChip(onClick = {}, label = { Text(item.name) })
                    }
                }
            }
        }
    }
    if (items.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
    }
}

@SuppressLint("ResourceType")
@Composable
private fun LogoBadge(
    name: String,
    @IdRes iconResId: Int,
) {
    Surface(
        onClick = {},
        shape = CardDefaults.shape,
        color = MaterialTheme.colorScheme.surface,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Image(
            painter = painterResource(iconResId),
            contentDescription = name,
            modifier = Modifier
                .height(52.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@SuppressLint("ResourceType")
@Composable
private fun PlatformBadge(
    name: String,
    @IdRes iconResId: Int,
) {
    IconButton(
        onClick = {},
        modifier = Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Image(
            painter = painterResource(iconResId),
            contentDescription = name,
            modifier = Modifier.size(20.dp)
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
