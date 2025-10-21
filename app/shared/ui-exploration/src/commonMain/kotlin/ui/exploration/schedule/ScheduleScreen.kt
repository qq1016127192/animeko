/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.exploration.schedule

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.number
import me.him188.ani.app.ui.adaptive.AniTopAppBar
import me.him188.ani.app.ui.adaptive.HorizontalScrollControlScaffoldOnDesktop
import me.him188.ani.app.ui.foundation.HorizontalScrollControlState
import me.him188.ani.app.ui.foundation.LocalPlatform
import me.him188.ani.app.ui.foundation.layout.AniWindowInsets
import me.him188.ani.app.ui.foundation.layout.currentWindowAdaptiveInfo1
import me.him188.ani.app.ui.foundation.layout.isWidthAtLeastMedium
import me.him188.ani.app.ui.foundation.pagerTabIndicatorOffset
import me.him188.ani.app.ui.foundation.rememberHorizontalScrollControlState
import me.him188.ani.app.ui.foundation.theme.AniThemeDefaults
import me.him188.ani.app.ui.search.LoadErrorCard
import me.him188.ani.utils.platform.isDesktop

fun ScheduleScreenState(
    daysProvider: () -> List<ScheduleDay>,
): ScheduleScreenState {
    return ScheduleScreenState(
        initialSelected = daysProvider().firstOrNull { it.kind == ScheduleDay.Kind.TODAY },
        daysProvider = daysProvider,
    )
}

@Stable
class ScheduleScreenState(
    initialSelected: ScheduleDay?,
    daysProvider: () -> List<ScheduleDay>,
) {
    val days by derivedStateOf(daysProvider)

    // on mobile
    internal val pagerState = PagerState(
        currentPage = days.indexOf(initialSelected).coerceAtLeast(0),
    ) { days.size }

    // on desktop jvm
    val lazyListState = LazyListState(firstVisibleItemIndex = pagerState.currentPage)

    val selectedDay: ScheduleDay? by derivedStateOf {
        days.getOrNull(pagerState.currentPage)
    }

    val scheduleColumnLazyListStates by derivedStateOf {
        days.associateWith {
            LazyListState()
        }
    }

    suspend fun scrollTo(day: ScheduleDay) {
        pagerState.scrollToPage(days.indexOf(day).coerceAtLeast(0))
    }

    suspend fun animateScrollTo(day: ScheduleDay) {
        pagerState.animateScrollToPage(days.indexOf(day).coerceAtLeast(0))
    }
}


@Composable
fun ScheduleScreen(
    presentation: SchedulePagePresentation,
    onRetry: () -> Unit,
    onClickItem: (item: AiringScheduleItemPresentation) -> Unit,
    modifier: Modifier = Modifier,
    layoutParams: ScheduleScreenLayoutParams = ScheduleScreenLayoutParams.calculate(),
    colors: ScheduleScreenColors = ScheduleScreenDefaults.colors(),
    navigationIcon: @Composable () -> Unit = {},
    state: ScheduleScreenState = remember { ScheduleScreenState { presentation.days } },
    windowInsets: WindowInsets = AniWindowInsets.forPageContent(),
) {
    Scaffold(
        modifier,
        topBar = {
            AniTopAppBar(
                title = { Text("新番时间表") },
                Modifier.fillMaxWidth(),
                navigationIcon = navigationIcon,
                windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            )
        },
        containerColor = AniThemeDefaults.pageContentBackgroundColor,
        contentWindowInsets = windowInsets.only(WindowInsetsSides.Horizontal),
    ) { paddingValues ->
        if (presentation.error != null) {
            LoadErrorCard(
                presentation.error,
                onRetry = onRetry,
                modifier = Modifier.padding(paddingValues)
                    .padding(layoutParams.pageContentPadding)
                    .padding(all = 16.dp),
            )
        } else {
            ScheduleScreenContent(
                state = state,
                modifier = Modifier.padding(paddingValues),
                layoutParams = layoutParams,
                colors = colors,
            ) { day ->
                ScheduleDayColumn(
                    onClickItem = onClickItem,
                    dayOfWeek = {
                        if (layoutParams.showDayOfWeekHeadline) {
                            DayOfWeekHeadline(day)
                        }
                    },
                    items = presentation.airingSchedules.firstOrNull { it.date == day.date }?.episodes.orEmpty(),
                    layoutParams = layoutParams.columnLayoutParams,
                    state = state.scheduleColumnLazyListStates[day] ?: rememberLazyListState(),
                    itemColors = colors.itemColors,
                )
            }
        }
    }
}

@Composable
private fun DayOfWeekHeadline(
    day: ScheduleDay,
    modifier: Modifier = Modifier
) {
    Column(modifier.width(IntrinsicSize.Min)) {
        Text(
            renderScheduleDay(day), Modifier.width(IntrinsicSize.Max),
            softWrap = false, textAlign = TextAlign.Start,
            color = if (day.kind == ScheduleDay.Kind.TODAY) MaterialTheme.colorScheme.primary else Color.Unspecified,
        )

        // Rounded horizontal divider
        val thickness = 2.dp
        val color = MaterialTheme.colorScheme.outlineVariant
        Canvas(
            Modifier.padding(top = 2.dp)
                .fillMaxWidth()
                .height(thickness),
        ) {
            drawLine(
                color = color,
                strokeWidth = thickness.toPx(),
                start = Offset(0f, thickness.toPx() / 2),
                end = Offset(size.width, thickness.toPx() / 2),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun ScheduleScreenContent(
    state: ScheduleScreenState,
    modifier: Modifier = Modifier,
    layoutParams: ScheduleScreenLayoutParams = ScheduleScreenLayoutParams.calculate(),
    colors: ScheduleScreenColors = ScheduleScreenDefaults.colors(),
    pageContent: @Composable (page: ScheduleDay) -> Unit,
) {
    Column(modifier) {
        val uiScope = rememberCoroutineScope()
        if (layoutParams.showTabRow) {
            ScrollableTabRow(
                selectedTabIndex = state.pagerState.currentPage,
                containerColor = colors.tabRowContainerColor,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.pagerTabIndicatorOffset(state.pagerState, tabPositions),
                    )
                },
            ) {
                state.days.forEach { day ->
                    Tab(
                        selected = state.selectedDay == day,
                        onClick = {
                            uiScope.launch {
                                state.animateScrollTo(day)
                            }
                        },
                        text = {
                            Text(
                                renderScheduleDay(day), softWrap = false, textAlign = TextAlign.Center,
                                color = if (day.kind == ScheduleDay.Kind.TODAY) MaterialTheme.colorScheme.primary else Color.Unspecified,
                            )
                        },
                        selectedContentColor = colors.tabSelectedContentColor,
                        unselectedContentColor = colors.tabUnselectedContentColor,
                    )
                }
            }
        }

        val density = LocalDensity.current

        if (LocalPlatform.current.isDesktop() && !layoutParams.isSinglePage) {
            // CMP bug, HorizontalPager 在 PC 上滚动到末尾后, 内嵌的 LazyColumn 无法纵向滚动
            HorizontalScrollControlScaffoldOnDesktop(
                rememberHorizontalScrollControlState(
                    state.lazyListState,
                    onClickScroll = { direction ->
                        uiScope.launch {
                            state.lazyListState.animateScrollBy(
                                with(density) { (300.dp).toPx() } *
                                        if (direction == HorizontalScrollControlState.Direction.BACKWARD) -1 else 1,
                            )
                        }
                    },
                ),
            ) {
                LazyRow(
                    Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(layoutParams.pageSpacing),
                    state = state.lazyListState,
                ) {
                    items(state.days) { day ->
                        val widthModifier = when (val pageSize = layoutParams.pageSize) {
                            PageSize.Fill -> Modifier.fillMaxWidth()
                            is PageSize.Fixed -> Modifier.width(pageSize.pageSize)
                            else -> Modifier
                        }
                        Box(widthModifier.fillParentMaxHeight().padding(layoutParams.pageContentPadding)) {
                            pageContent(day)
                        }
                    }
                }
            }
        } else {
            HorizontalPager(
                state.pagerState,
                Modifier.fillMaxSize(),
                pageSize = layoutParams.pageSize,
                pageSpacing = layoutParams.pageSpacing,
                contentPadding = layoutParams.pageContentPadding,
                verticalAlignment = Alignment.Top,
                key = { it },
            ) { index ->
                Box(Modifier.fillMaxSize()) { // ensure the page is scrollable
                    state.days.getOrNull(index)?.let {
                        pageContent(it)
                    }
                }
            }
        }
    }
}

@Immutable
@ExposedCopyVisibility
data class ScheduleScreenLayoutParams private constructor(
    val pageSize: PageSize,
    val pageSpacing: Dp,
    val pageContentPadding: PaddingValues,
    val showTabRow: Boolean,
    val showDayOfWeekHeadline: Boolean,
    val columnLayoutParams: ScheduleDayColumnLayoutParams,
    val isSinglePage: Boolean, // Workaround for CMP bug
) {
    @Stable
    companion object {
        @Stable
        val Compact = ScheduleScreenLayoutParams(
            pageSize = PageSize.Fill,
            pageSpacing = 8.dp,
            pageContentPadding = PaddingValues(0.dp),
            showTabRow = true,
            showDayOfWeekHeadline = false,
            columnLayoutParams = ScheduleDayColumnLayoutParams.Default,
            isSinglePage = true,
        )

        @Stable
        val Medium = ScheduleScreenLayoutParams(
            pageSize = PageSize.Fixed(360.dp),
            pageSpacing = 16.dp,
            pageContentPadding = PaddingValues(horizontal = 8.dp),
            showTabRow = false,
            showDayOfWeekHeadline = true,
            columnLayoutParams = ScheduleDayColumnLayoutParams.Default,
            isSinglePage = false,
        )

        @Composable
        fun calculate(windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo1().windowSizeClass): ScheduleScreenLayoutParams {
            return if (windowSizeClass.isWidthAtLeastMedium) {
                Medium
            } else {
                Compact
            }
        }
    }
}

@Immutable
data class ScheduleScreenColors(
    val tabRowContainerColor: Color,
    val tabSelectedContentColor: Color,
    val tabUnselectedContentColor: Color,
    val itemColors: ListItemColors,
)

@Stable
object ScheduleScreenDefaults {
    @Composable
    fun colors(
        tabRowColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tabSelectedContentColor: Color = contentColorFor(tabRowColor),
        tabUnselectedContentColor: Color = contentColorFor(tabRowColor),
        itemColors: ListItemColors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ): ScheduleScreenColors = ScheduleScreenColors(
        tabRowContainerColor = tabRowColor,
        tabSelectedContentColor = tabSelectedContentColor,
        tabUnselectedContentColor = tabUnselectedContentColor,
        itemColors = itemColors,
    )
}


@Stable
private fun renderScheduleDay(day: ScheduleDay): String {
    val date = day.date
    return """
        ${date.month.number}/${date.day}
        ${renderDayOfWeek(day.dayOfWeek, day.kind)}
    """.trimIndent()
}

@Stable
@Suppress("REDUNDANT_ELSE_IN_WHEN") // Compiler works fine, but IDE complains about this, so we suppress it.
private fun renderDayOfWeek(day: DayOfWeek, kind: ScheduleDay.Kind): String = when (kind) {
    // we manually permute them to make them real constants to avoid runtime allocations.
    ScheduleDay.Kind.LAST_WEEK -> when (day) {
        DayOfWeek.MONDAY -> "上周一"
        DayOfWeek.TUESDAY -> "上周二"
        DayOfWeek.WEDNESDAY -> "上周三"
        DayOfWeek.THURSDAY -> "上周四"
        DayOfWeek.FRIDAY -> "上周五"
        DayOfWeek.SATURDAY -> "上周六"
        DayOfWeek.SUNDAY -> "上周日"
        else -> day.toString()
    }

    ScheduleDay.Kind.THIS_WEEK,
    ScheduleDay.Kind.TODAY -> when (day) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
        else -> day.toString()
    }

    ScheduleDay.Kind.NEXT_WEEK -> when (day) {
        DayOfWeek.MONDAY -> "下周一"
        DayOfWeek.TUESDAY -> "下周二"
        DayOfWeek.WEDNESDAY -> "下周三"
        DayOfWeek.THURSDAY -> "下周四"
        DayOfWeek.FRIDAY -> "下周五"
        DayOfWeek.SATURDAY -> "下周六"
        DayOfWeek.SUNDAY -> "下周日"
        else -> day.toString()
    }
}
