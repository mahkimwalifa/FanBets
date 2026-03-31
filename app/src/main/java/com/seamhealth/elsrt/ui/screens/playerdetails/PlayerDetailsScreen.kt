package com.seamhealth.elsrt.ui.screens.playerdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.seamhealth.elsrt.data.api.models.PlayerStatistics
import com.seamhealth.elsrt.ui.components.ErrorScreen
import com.seamhealth.elsrt.ui.components.LoadingScreen
import com.seamhealth.elsrt.ui.components.SectionHeader
import com.seamhealth.elsrt.ui.components.TeamLogo
import com.seamhealth.elsrt.ui.theme.DarkBlue
import com.seamhealth.elsrt.ui.theme.LightGray
import com.seamhealth.elsrt.ui.theme.PrimaryRed
import com.seamhealth.elsrt.ui.theme.White
import com.seamhealth.elsrt.ui.theme.YellowCard
import com.seamhealth.elsrt.ui.theme.RedCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerDetailsScreen(
    onBack: () -> Unit,
    viewModel: PlayerDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = state.player?.player?.name ?: "Player",
                        color = White,
                        maxLines = 1
                    )
                },
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
                    onRetry = { viewModel.loadPlayer() }
                )
                state.player != null -> {
                    val player = state.player!!
                    val info = player.player

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = info?.photo,
                            contentDescription = null,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "${info?.firstname ?: ""} ${info?.lastname ?: ""}".trim().ifEmpty { info?.name ?: "" },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = DarkBlue
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            info?.nationality?.let { InfoChip("Nat.", it) }
                            info?.age?.let { InfoChip("Age", it.toString()) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            info?.height?.let { InfoChip("Height", it) }
                            info?.weight?.let { InfoChip("Weight", it) }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        player.statistics?.forEach { stat ->
                            StatCard(stat)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StatCard(stat: PlayerStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LightGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeamLogo(url = stat.team?.logo, size = 24.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stat.team?.name ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    color = DarkBlue
                )
            }
            Text(
                text = "${stat.league?.name ?: ""} • ${stat.league?.season ?: ""}",
                style = MaterialTheme.typography.labelSmall
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Apps", "${stat.games?.appearances ?: 0}")
                StatItem("Goals", "${stat.goals?.total ?: 0}")
                StatItem("Assists", "${stat.goals?.assists ?: 0}")
                StatItem("Rating", stat.games?.rating?.take(4) ?: "-")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Minutes", "${stat.games?.minutes ?: 0}")
                StatItem("Shots", "${stat.shots?.total ?: 0}")
                StatItem("Passes", "${stat.passes?.total ?: 0}")
                StatItem("Dribbles", "${stat.dribbles?.success ?: 0}")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("YC", "${stat.cards?.yellow ?: 0}", YellowCard)
                StatItem("RC", "${stat.cards?.red ?: 0}", RedCard)
                StatItem("Tackles", "${stat.tackles?.total ?: 0}")
                StatItem("Interceptions", "${stat.tackles?.interceptions ?: 0}")
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = DarkBlue
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center
        )
    }
}
