package com.seamhealth.elsrt.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sports
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Live : BottomNavItem("live", "Live", Icons.Filled.Sports)
    data object Matches : BottomNavItem("matches", "Matches", Icons.Filled.Schedule)
    data object Leagues : BottomNavItem("leagues", "Leagues", Icons.Filled.EmojiEvents)
    data object Favorites : BottomNavItem("favorites", "Favorites", Icons.Filled.Favorite)
    data object More : BottomNavItem("more", "More", Icons.Filled.MoreHoriz)
}

object Routes {
    const val MATCH_DETAILS = "match_details/{fixtureId}"
    const val TEAM_DETAILS = "team_details/{teamId}"
    const val PLAYER_DETAILS = "player_details/{playerId}/{season}"
    const val STANDINGS = "standings/{leagueId}/{season}"
    const val TOP_SCORERS = "top_scorers/{leagueId}/{season}"

    fun matchDetails(fixtureId: Int) = "match_details/$fixtureId"
    fun teamDetails(teamId: Int) = "team_details/$teamId"
    fun playerDetails(playerId: Int, season: Int) = "player_details/$playerId/$season"
    fun standings(leagueId: Int, season: Int) = "standings/$leagueId/$season"
    fun topScorers(leagueId: Int, season: Int) = "top_scorers/$leagueId/$season"
}
