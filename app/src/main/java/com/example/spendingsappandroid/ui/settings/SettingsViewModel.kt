package com.example.spendingsappandroid.ui.settings

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import com.example.spendingsappandroid.data.repository.MonitoredAppsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class InstalledApp(
    val packageName: String,
    val label: String
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val monitoredAppsRepository: MonitoredAppsRepository
) : ViewModel() {

    val monitoredPackages: StateFlow<Set<String>> =
        monitoredAppsRepository.monitoredPackages

    val currency: StateFlow<String> =
        monitoredAppsRepository.currency

    val supportedCurrencies: List<String> =
        MonitoredAppsRepository.SUPPORTED_CURRENCIES

    fun setCurrency(code: String) {
        monitoredAppsRepository.setCurrency(code)
    }

    fun setMonitored(packageName: String, monitored: Boolean) {
        monitoredAppsRepository.setMonitored(packageName, monitored)
    }

    /**
     * Returns all user-visible installed apps sorted alphabetically,
     * excluding this app itself.
     */
    fun getInstalledApps(packageManager: PackageManager): List<InstalledApp> {
        val selfPackage = "com.example.spendingsappandroid"
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 || isKnownFinancialApp(it.packageName) }
            .filter { it.packageName != selfPackage }
            .map { appInfo ->
                InstalledApp(
                    packageName = appInfo.packageName,
                    label = packageManager.getApplicationLabel(appInfo).toString()
                )
            }
            .sortedWith(compareByDescending<InstalledApp> {
                it.packageName in monitoredPackages.value
            }.thenBy { it.label.lowercase() })
    }

    private fun isKnownFinancialApp(packageName: String): Boolean =
        packageName in MonitoredAppsRepository.DEFAULT_PACKAGES
}
