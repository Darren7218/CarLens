package com.carlens.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.carlens.domain.model.AppSettings
import com.carlens.domain.model.ThemeMode
import com.carlens.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "carlens_settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HIDE_PLATE = booleanPreferencesKey("hide_plate")
        val HIDE_AMOUNTS = booleanPreferencesKey("hide_amounts")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val HIDE_NOTIFICATION_DETAILS = booleanPreferencesKey("hide_notif_details")
        val MONTHLY_BUDGET = longPreferencesKey("monthly_budget_sen")
        val SELECTED_VEHICLE = stringPreferencesKey("selected_vehicle_id")
        val LAST_STATION = stringPreferencesKey("last_station")
        val LAST_FUEL_TYPE = stringPreferencesKey("last_fuel_type")
        fun draftKey(name: String) = stringPreferencesKey("draft_$name")
    }

    override val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            themeMode = p[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            hidePlate = p[Keys.HIDE_PLATE] ?: false,
            hideAmounts = p[Keys.HIDE_AMOUNTS] ?: false,
            biometricLock = p[Keys.BIOMETRIC_LOCK] ?: false,
            hideNotificationDetails = p[Keys.HIDE_NOTIFICATION_DETAILS] ?: false,
            monthlyBudgetSen = p[Keys.MONTHLY_BUDGET],
            selectedVehicleId = p[Keys.SELECTED_VEHICLE],
            lastStation = p[Keys.LAST_STATION] ?: "",
            lastFuelType = p[Keys.LAST_FUEL_TYPE] ?: "RON95"
        )
    }

    override suspend fun current(): AppSettings = settings.first()

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun setHidePlate(v: Boolean) {
        context.dataStore.edit { it[Keys.HIDE_PLATE] = v }
    }

    override suspend fun setHideAmounts(v: Boolean) {
        context.dataStore.edit { it[Keys.HIDE_AMOUNTS] = v }
    }

    override suspend fun setBiometricLock(v: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_LOCK] = v }
    }

    override suspend fun setHideNotificationDetails(v: Boolean) {
        context.dataStore.edit { it[Keys.HIDE_NOTIFICATION_DETAILS] = v }
    }

    override suspend fun setMonthlyBudgetSen(v: Long?) {
        context.dataStore.edit {
            if (v != null) it[Keys.MONTHLY_BUDGET] = v
            else it.remove(Keys.MONTHLY_BUDGET)
        }
    }

    override suspend fun setSelectedVehicle(id: String?) {
        context.dataStore.edit {
            if (id != null) it[Keys.SELECTED_VEHICLE] = id
            else it.remove(Keys.SELECTED_VEHICLE)
        }
    }

    override suspend fun rememberFuelChoice(station: String, fuelType: String) {
        context.dataStore.edit {
            it[Keys.LAST_STATION] = station
            it[Keys.LAST_FUEL_TYPE] = fuelType
        }
    }

    override suspend fun saveDraft(key: String, json: String?) {
        context.dataStore.edit {
            val k = Keys.draftKey(key)
            if (json != null) it[k] = json else it.remove(k)
        }
    }

    override suspend fun loadDraft(key: String): String? {
        val p = context.dataStore.data.first()
        return p[Keys.draftKey(key)]
    }
}
