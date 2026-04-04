package com.seamhealth.elsrt.ui.screens.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.seamhealth.elsrt.R
import com.seamhealth.elsrt.activity.AccountRemovalActivity
import com.seamhealth.elsrt.ui.screens.phone.PolicyViewerScreen
import com.seamhealth.elsrt.ui.theme.DarkBlue
import com.seamhealth.elsrt.ui.theme.LightGray
import com.seamhealth.elsrt.ui.theme.PrimaryRed
import com.seamhealth.elsrt.ui.theme.White

private const val PRIVACY_POLICY_URL = "https://appinforules.site/fanbets/privacy-policy/"

private data class TopLeagueOption(
    val leagueId: Int,
    val season: Int,
    val title: String
)

private val TOP_LEAGUE_OPTIONS = listOf(
    TopLeagueOption(39, 2024, "Premier League"),
    TopLeagueOption(140, 2024, "La Liga"),
    TopLeagueOption(135, 2024, "Serie A"),
    TopLeagueOption(78, 2024, "Bundesliga"),
    TopLeagueOption(61, 2024, "Ligue 1")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onTopScorersClick: (leagueId: Int, season: Int) -> Unit,
    viewModel: MoreViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var showTopScorersDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showPolicyViewer by remember { mutableStateOf(false) }

    if (showPolicyViewer) {
        PolicyViewerScreen(
            destination = PRIVACY_POLICY_URL,
            onClose = { showPolicyViewer = false }
        )
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.tab_more), color = White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = PrimaryRed
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(innerPadding)
        ) {
            item {
                MoreMenuItem(
                    title = stringResource(R.string.top_scorers),
                    icon = Icons.Default.Star,
                    onClick = { showTopScorersDialog = true }
                )
            }
            item {
                MoreMenuItem(
                    title = stringResource(R.string.delete_favorites),
                    icon = Icons.Default.DeleteForever,
                    onClick = { showDeleteConfirmation = true }
                )
            }
            item {
                MoreMenuItem(
                    title = stringResource(R.string.privacy_policy),
                    icon = Icons.Default.Policy,
                    onClick = { showPolicyViewer = true }
                )
            }
            item {
                MoreMenuItem(
                    title = stringResource(R.string.delete_account),
                    icon = Icons.Default.PersonRemove,
                    onClick = { AccountRemovalActivity.start(context) }
                )
            }
        }
    }

    if (showTopScorersDialog) {
        AlertDialog(
            onDismissRequest = { showTopScorersDialog = false },
            title = { Text(stringResource(R.string.select_league)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TOP_LEAGUE_OPTIONS.forEach { option ->
                        TextButton(
                            onClick = {
                                showTopScorersDialog = false
                                onTopScorersClick(option.leagueId, option.season)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(option.title, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTopScorersDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_all_favorites)) },
            text = { Text(stringResource(R.string.delete_favorites_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllFavorites()
                    showDeleteConfirmation = false
                }) {
                    Text(stringResource(R.string.delete), color = PrimaryRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun MoreMenuItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = LightGray)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = DarkBlue)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
