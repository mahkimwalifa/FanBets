package com.seamhealth.elsrt.ui.screens.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seamhealth.elsrt.ui.components.EmptyScreen
import com.seamhealth.elsrt.ui.components.ErrorScreen
import com.seamhealth.elsrt.ui.components.LoadingScreen
import com.seamhealth.elsrt.ui.components.MatchCard
import com.seamhealth.elsrt.ui.components.SectionHeader
import com.seamhealth.elsrt.ui.theme.DarkBlue
import com.seamhealth.elsrt.ui.theme.PrimaryRed
import com.seamhealth.elsrt.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScreen(
    onFixtureClick: (Int) -> Unit,
    viewModel: LiveViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val fixturesByLeague = state.fixtures.groupBy { it.league?.name ?: "—" }
    val errorMessage = state.error

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Live",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
        when {
            state.isLoading && state.fixtures.isEmpty() -> LoadingScreen(Modifier.padding(innerPadding))
            errorMessage != null && state.fixtures.isEmpty() ->
                ErrorScreen(errorMessage, onRetry = viewModel::loadLiveFixtures, modifier = Modifier.padding(innerPadding))
            state.fixtures.isEmpty() ->
                EmptyScreen("No live matches at the moment", modifier = Modifier.padding(innerPadding))
            else -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
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
