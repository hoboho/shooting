package ir.hoboho.shooting.data.repository

import ir.hoboho.shooting.data.local.AppSettingDao
import ir.hoboho.shooting.data.local.AppSettingEntity
import ir.hoboho.shooting.data.mapper.SettingKeys
import ir.hoboho.shooting.data.mapper.toDomain
import ir.hoboho.shooting.domain.model.AppSettings
import ir.hoboho.shooting.domain.model.ThemeMode
import ir.hoboho.shooting.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dao: AppSettingDao,
) : SettingsRepository {

    override fun observeSettings(): Flow<AppSettings> =
        dao.observeAll().map { rows -> rows.toDomain() }

    override suspend fun getSettings(): AppSettings = dao.getAll().toDomain()

    override suspend fun setThemeMode(mode: ThemeMode) {
        put(SettingKeys.THEME_MODE, mode.name)
    }

    override suspend fun setDefaultProjectId(id: Long?) {
        put(SettingKeys.DEFAULT_PROJECT_ID, id?.toString() ?: "")
    }

    override suspend fun setLastBackupEpochMilli(value: Long) {
        put(SettingKeys.LAST_BACKUP_EPOCH_MILLI, value.toString())
    }

    private suspend fun put(key: String, value: String) {
        dao.upsert(AppSettingEntity(key, value))
    }
}
