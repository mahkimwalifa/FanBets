package com.seamhealth.elsrt.ui.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.seamhealth.elsrt.ui.screens.favorites.FavoritesScreen
import com.seamhealth.elsrt.ui.screens.leagues.LeaguesScreen
import com.seamhealth.elsrt.ui.screens.live.LiveScreen
import com.seamhealth.elsrt.ui.screens.matchdetails.MatchDetailsScreen
import com.seamhealth.elsrt.ui.screens.matches.MatchesScreen
import com.seamhealth.elsrt.ui.screens.more.MoreScreen
import com.seamhealth.elsrt.ui.screens.playerdetails.PlayerDetailsScreen
import com.seamhealth.elsrt.ui.screens.standings.StandingsScreen
import com.seamhealth.elsrt.ui.screens.teamdetails.TeamDetailsScreen
import com.seamhealth.elsrt.ui.screens.topscorers.TopScorersScreen
import com.seamhealth.elsrt.ui.theme.DarkBlue
import com.seamhealth.elsrt.ui.theme.PrimaryRed
import com.seamhealth.elsrt.ui.theme.White

@Composable
fun FanBetsNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val bottomItems = listOf(
        BottomNavItem.Live,
        BottomNavItem.Matches,
        BottomNavItem.Leagues,
        BottomNavItem.Favorites,
        BottomNavItem.More
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isBottomBarVisible = bottomItems.any { it.route == currentDestination?.route }

    BackHandler(enabled = isBottomBarVisible) {
        (context as? Activity)?.moveTaskToBack(true)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar(containerColor = PrimaryRed) {
                    bottomItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = stringResource(item.titleResId), tint = if (selected) White else White.copy(alpha = 0.85f)) },
                            label = { Text(stringResource(item.titleResId), color = if (selected) White else White.copy(alpha = 0.85f)) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = White,
                                selectedTextColor = White,
                                unselectedIconColor = White.copy(alpha = 0.85f),
                                unselectedTextColor = White.copy(alpha = 0.85f),
                                indicatorColor = DarkBlue
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Live.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Live.route) {
                LiveScreen(
                    onFixtureClick = { navController.navigate(Routes.matchDetails(it)) }
                )
            }
            composable(BottomNavItem.Matches.route) {
                MatchesScreen(
                    onFixtureClick = { navController.navigate(Routes.matchDetails(it)) }
                )
            }
            composable(BottomNavItem.Leagues.route) {
                LeaguesScreen(
                    onLeagueClick = { leagueId, season ->
                        navController.navigate(Routes.standings(leagueId, season))
                    }
                )
            }
            composable(BottomNavItem.Favorites.route) {
                FavoritesScreen(
                    onTeamClick = { navController.navigate(Routes.teamDetails(it)) },
                    onLeagueClick = { leagueId, season ->
                        navController.navigate(Routes.standings(leagueId, season))
                    }
                )
            }
            composable(BottomNavItem.More.route) {
                MoreScreen(
                    onTopScorersClick = { leagueId, season ->
                        navController.navigate(Routes.topScorers(leagueId, season))
                    }
                )
            }

            composable(
                Routes.MATCH_DETAILS,
                arguments = listOf(navArgument("fixtureId") { type = NavType.IntType })
            ) {
                MatchDetailsScreen(
                    onBack = { navController.popBackStack() },
                    onTeamClick = { navController.navigate(Routes.teamDetails(it)) },
                    onPlayerClick = { playerId, season ->
                        navController.navigate(Routes.playerDetails(playerId, season))
                    },
                    onFixtureClick = { navController.navigate(Routes.matchDetails(it)) }
                )
            }

            composable(
                Routes.TEAM_DETAILS,
                arguments = listOf(navArgument("teamId") { type = NavType.IntType })
            ) {
                TeamDetailsScreen(
                    onBack = { navController.popBackStack() },
                    onPlayerClick = { playerId, season ->
                        navController.navigate(Routes.playerDetails(playerId, season))
                    },
                    onFixtureClick = { navController.navigate(Routes.matchDetails(it)) }
                )
            }

            composable(
                Routes.PLAYER_DETAILS,
                arguments = listOf(
                    navArgument("playerId") { type = NavType.IntType },
                    navArgument("season") { type = NavType.IntType }
                )
            ) {
                PlayerDetailsScreen(onBack = { navController.popBackStack() })
            }

            composable(
                Routes.STANDINGS,
                arguments = listOf(
                    navArgument("leagueId") { type = NavType.IntType },
                    navArgument("season") { type = NavType.IntType }
                )
            ) {
                StandingsScreen(
                    onBack = { navController.popBackStack() },
                    onTeamClick = { navController.navigate(Routes.teamDetails(it)) }
                )
            }

            composable(
                Routes.TOP_SCORERS,
                arguments = listOf(
                    navArgument("leagueId") { type = NavType.IntType },
                    navArgument("season") { type = NavType.IntType }
                )
            ) {
                TopScorersScreen(
                    onBack = { navController.popBackStack() },
                    onPlayerClick = { playerId, season ->
                        navController.navigate(Routes.playerDetails(playerId, season))
                    }
                )
            }
        }
    }
}
