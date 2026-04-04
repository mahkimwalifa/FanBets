package com.seamhealth.elsrt.ui.screens.leagues

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seamhealth.elsrt.R
import com.seamhealth.elsrt.data.api.models.LeagueResponse
import com.seamhealth.elsrt.ui.components.EmptyScreen
import com.seamhealth.elsrt.ui.components.ErrorScreen
import com.seamhealth.elsrt.ui.components.LoadingScreen
import com.seamhealth.elsrt.ui.components.SectionHeader
import com.seamhealth.elsrt.ui.components.TeamLogo
import com.seamhealth.elsrt.ui.theme.DarkBlue
import com.seamhealth.elsrt.ui.theme.LightGray
import com.seamhealth.elsrt.ui.theme.PrimaryRed
import com.seamhealth.elsrt.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaguesScreen(
    onLeagueClick: (leagueId: Int, season: Int) -> Unit,
    viewModel: LeaguesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.tab_leagues), color = White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = PrimaryRed
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::onSearchQueryChange,
            placeholder = { Text(stringResource(R.string.search_league_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DarkBlue, cursorColor = DarkBlue)
        )

        when {
            state.isLoading -> LoadingScreen()
            state.error != null -> ErrorScreen(state.error!!, onRetry = viewModel::loadLeagues)
            state.leaguesByCountry.isEmpty() -> EmptyScreen(stringResource(R.string.no_leagues_found))
            else -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        state.leaguesByCountry.forEach { (country, leagues) ->
                            item(key = "header_$country") { SectionHeader(country) }
                            items(leagues, key = { "${country}_${it.league?.id}" }) { league ->
                                LeagueItem(
                                    league = league,
                                    onClick = {
                                        val leagueId = league.league?.id ?: return@LeagueItem
                                        val season = league.seasons
                                            ?.firstOrNull { it.current == true }
                                            ?.year
                                            ?: league.seasons?.maxOfOrNull { it.year ?: 0 }
                                            ?: 2024
                                        onLeagueClick(leagueId, season)
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

@Composable
private fun LeagueItem(league: LeagueResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = LightGray)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeamLogo(url = league.league?.logo, size = 36.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = league.league?.name ?: "",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = league.league?.type ?: "",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TeamLogo(url = league.country?.flag, size = 28.dp)
        }
    }
}
