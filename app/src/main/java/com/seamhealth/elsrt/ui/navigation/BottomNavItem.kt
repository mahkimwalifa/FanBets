package com.seamhealth.elsrt.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sports
import androidx.compose.ui.graphics.vector.ImageVector
import com.seamhealth.elsrt.R

sealed class BottomNavItem(
    val route: String,
    @StringRes val titleResId: Int,
    val icon: ImageVector
) {
    data object Live : BottomNavItem("live", R.string.tab_live, Icons.Filled.Sports)
    data object Matches : BottomNavItem("matches", R.string.tab_matches, Icons.Filled.Schedule)
    data object Leagues : BottomNavItem("leagues", R.string.tab_leagues, Icons.Filled.EmojiEvents)
    data object Favorites : BottomNavItem("favorites", R.string.tab_favorites, Icons.Filled.Favorite)
    data object More : BottomNavItem("more", R.string.tab_more, Icons.Filled.MoreHoriz)
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
