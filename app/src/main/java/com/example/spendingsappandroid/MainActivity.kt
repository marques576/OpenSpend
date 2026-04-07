package com.example.spendingsappandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.spendingsappandroid.data.repository.MonitoredAppsRepository
import com.example.spendingsappandroid.ui.dashboard.DashboardScreen
import com.example.spendingsappandroid.ui.dashboard.DashboardViewModel
import com.example.spendingsappandroid.ui.settings.SettingsScreen
import com.example.spendingsappandroid.ui.theme.SpendingsAppAndroidTheme
import com.example.spendingsappandroid.ui.transactions.TransactionListScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var monitoredAppsRepository: MonitoredAppsRepository

    @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by monitoredAppsRepository.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                MonitoredAppsRepository.THEME_LIGHT -> false
                MonitoredAppsRepository.THEME_DARK -> true
                else -> isSystemInDarkTheme()
            }

            SpendingsAppAndroidTheme(darkTheme = darkTheme) {
                var showSettings by remember { mutableStateOf(false) }

                if (showSettings) {
                    SettingsScreen(onBack = { showSettings = false })
                } else {
                    val viewModel: DashboardViewModel = hiltViewModel()
                    val pagerState = rememberPagerState(pageCount = { 2 })
                    val scope = rememberCoroutineScope()
                    val tabs = listOf("Dashboard", "Transactions")

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("OpenSpend") },
                                actions = {
                                    IconButton(onClick = { showSettings = true }) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Settings"
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            PrimaryTabRow(
                                selectedTabIndex = pagerState.currentPage,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                tabs.forEachIndexed { index, title ->
                                    Tab(
                                        selected = pagerState.currentPage == index,
                                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                        text = { Text(title) }
                                    )
                                }
                            }

                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { page ->
                                when (page) {
                                    0 -> DashboardScreen(viewModel = viewModel)
                                    1 -> TransactionListScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}