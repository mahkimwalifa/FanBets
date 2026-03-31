package com.seamhealth.elsrt.ui.screens.standings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seamhealth.elsrt.data.api.models.StandingEntry
import com.seamhealth.elsrt.ui.components.ErrorScreen
import com.seamhealth.elsrt.ui.components.LoadingScreen
import com.seamhealth.elsrt.ui.components.TeamLogo
import com.seamhealth.elsrt.ui.theme.DarkBlue
import com.seamhealth.elsrt.ui.theme.LightGray
import com.seamhealth.elsrt.ui.theme.PrimaryRed
import com.seamhealth.elsrt.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandingsScreen(
    onBack: () -> Unit,
    onTeamClick: (Int) -> Unit,
    viewModel: StandingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(state.leagueName ?: "Standings", color = White, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = PrimaryRed)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                state.isLoading -> LoadingScreen()
                state.error != null -> ErrorScreen(
                    message = state.error ?: "",
                    onRetry = { viewModel.loadStandings() }
                )
                else -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() }
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item { TableHeader() }
                            items(state.standings) { entry ->
                                TableRow(entry = entry, onClick = {
                                    entry.team?.id?.let { onTeamClick(it) }
                                })
                                HorizontalDivider(thickness = 0.5.dp, color = LightGray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("#", modifier = Modifier.width(28.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("Team", modifier = Modifier.width(160.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Text("P", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("W", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("D", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("L", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("GD", modifier = Modifier.width(36.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("Pts", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TableRow(entry: StandingEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${entry.rank ?: "-"}",
            modifier = Modifier.width(28.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = DarkBlue
        )

        Row(modifier = Modifier.width(160.dp), verticalAlignment = Alignment.CenterVertically) {
            TeamLogo(url = entry.team?.logo, size = 22.dp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = entry.team?.name ?: "",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
        }

        val stats = entry.all
        Text("${stats?.played ?: 0}", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Text("${stats?.win ?: 0}", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Text("${stats?.draw ?: 0}", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Text("${stats?.lose ?: 0}", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Text(
            text = "${entry.goalsDiff ?: 0}",
            modifier = Modifier.width(36.dp),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = "${entry.points ?: 0}",
            modifier = Modifier.width(32.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = DarkBlue
        )
    }
}
