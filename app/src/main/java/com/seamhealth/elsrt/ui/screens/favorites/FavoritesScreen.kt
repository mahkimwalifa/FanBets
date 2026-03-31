package com.seamhealth.elsrt.ui.screens.favorites

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seamhealth.elsrt.data.local.entity.FavoriteLeagueEntity
import com.seamhealth.elsrt.data.local.entity.FavoriteTeamEntity
import com.seamhealth.elsrt.ui.components.EmptyScreen
import com.seamhealth.elsrt.ui.components.SectionHeader
import com.seamhealth.elsrt.ui.components.TeamLogo
import com.seamhealth.elsrt.ui.theme.LightGray
import com.seamhealth.elsrt.ui.theme.PrimaryRed
import com.seamhealth.elsrt.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onTeamClick: (Int) -> Unit,
    onLeagueClick: (leagueId: Int, season: Int) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val teams by viewModel.favoriteTeams.collectAsStateWithLifecycle()
    val leagues by viewModel.favoriteLeagues.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Favorites", color = White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = PrimaryRed
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (teams.isEmpty() && leagues.isEmpty()) {
            EmptyScreen("Add teams or leagues to favorites", modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(innerPadding)
            ) {
                if (teams.isNotEmpty()) {
                    item { SectionHeader("Teams") }
                    items(teams, key = { "team_${it.teamId}" }) { team ->
                        SwipeFavoriteTeamRow(
                            team = team,
                            onOpen = { onTeamClick(team.teamId) },
                            onRemove = { viewModel.removeTeam(team.teamId) }
                        )
                    }
                }
                if (leagues.isNotEmpty()) {
                    item { SectionHeader("Leagues") }
                    items(leagues, key = { "league_${it.leagueId}" }) { league ->
                        SwipeFavoriteLeagueRow(
                            league = league,
                            onOpen = { onLeagueClick(league.leagueId, league.currentSeason) },
                            onRemove = { viewModel.removeLeague(league.leagueId) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeFavoriteTeamRow(
    team: FavoriteTeamEntity,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRemove()
                true
            } else false
        }
    )
    LaunchedEffect(team.teamId) {
        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
    }
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val scale by animateFloatAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1.2f else 1f,
                label = "iconScale"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PrimaryRed, RoundedCornerShape(8.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = White,
                    modifier = Modifier.scale(scale)
                )
            }
        },
        enableDismissFromEndToStart = true,
        enableDismissFromStartToEnd = false
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = LightGray)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamLogo(url = team.logo, size = 36.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = team.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = PrimaryRed)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeFavoriteLeagueRow(
    league: FavoriteLeagueEntity,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRemove()
                true
            } else false
        }
    )
    LaunchedEffect(league.leagueId) {
        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
    }
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val scale by animateFloatAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1.2f else 1f,
                label = "iconScaleLeague"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PrimaryRed, RoundedCornerShape(8.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = White,
                    modifier = Modifier.scale(scale)
                )
            }
        },
        enableDismissFromEndToStart = true,
        enableDismissFromStartToEnd = false
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = LightGray)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamLogo(url = league.logo, size = 36.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = league.name, style = MaterialTheme.typography.bodyLarge)
                    Text(text = league.country, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = PrimaryRed)
                }
            }
        }
    }
}
