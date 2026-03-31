package com.seamhealth.elsrt.ui.screens.matches

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seamhealth.elsrt.ui.components.EmptyScreen
import com.seamhealth.elsrt.ui.components.ErrorScreen
import com.seamhealth.elsrt.ui.components.LoadingScreen
import com.seamhealth.elsrt.ui.components.MatchCard
import com.seamhealth.elsrt.ui.components.SectionHeader
import com.seamhealth.elsrt.ui.theme.DarkBlue
import com.seamhealth.elsrt.ui.theme.LightGray
import com.seamhealth.elsrt.ui.theme.PrimaryRed
import com.seamhealth.elsrt.ui.theme.White
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(
    onFixtureClick: (Int) -> Unit,
    viewModel: MatchesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val apiDateFormat = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    }
    val dayFormatter = remember { SimpleDateFormat("EEE", Locale.ENGLISH) }
    val shortDateFormatter = remember { SimpleDateFormat("dd.MM", Locale.ENGLISH) }

    val weekDates = remember {
        val today = Calendar.getInstance()
        (-3..3).map { offset ->
            val c = Calendar.getInstance()
            c.timeInMillis = today.timeInMillis
            c.add(Calendar.DAY_OF_MONTH, offset)
            apiDateFormat.format(c.time) to c
        }
    }

    val todayApiString = remember {
        apiDateFormat.format(Calendar.getInstance().time)
    }

    val fixturesByLeague = state.fixtures.groupBy { it.league?.name ?: "—" }
    val errorMessage = state.error

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Matches",
                        color = White,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = PrimaryRed,
                    titleContentColor = White,
                    navigationIconContentColor = DarkBlue,
                    actionIconContentColor = DarkBlue
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                weekDates.forEach { (dateStr, cal) ->
                    val isSelected = dateStr == state.selectedDate
                    val isToday = dateStr == todayApiString
                    Box(
                        modifier = Modifier
                            .width(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) DarkBlue else LightGray)
                            .clickable { viewModel.selectDate(dateStr) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isToday) "Today" else dayFormatter.format(cal.time),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) White else DarkBlue,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = shortDateFormatter.format(cal.time),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) White else DarkBlue,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            when {
                state.isLoading && state.fixtures.isEmpty() -> LoadingScreen()
                errorMessage != null && state.fixtures.isEmpty() ->
                    ErrorScreen(errorMessage, onRetry = viewModel::loadFixtures)
                state.fixtures.isEmpty() ->
                    EmptyScreen("No matches for this date")
                else -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = viewModel::refresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            fixturesByLeague.forEach { (leagueName, fixtures) ->
                                item(key = "header_$leagueName") {
                                    SectionHeader(leagueName)
                                }
                                items(
                                    items = fixtures,
                                    key = { it.fixture?.id ?: it.hashCode() }
                                ) { fixture ->
                                    MatchCard(
                                        fixture = fixture,
                                        onClick = {
                                            val id = fixture.fixture?.id ?: return@MatchCard
                                            onFixtureClick(id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
