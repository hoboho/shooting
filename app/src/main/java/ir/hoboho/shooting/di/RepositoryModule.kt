package ir.hoboho.shooting.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.hoboho.shooting.data.repository.BackupRepositoryImpl
import ir.hoboho.shooting.data.repository.ExpenseRepositoryImpl
import ir.hoboho.shooting.data.repository.ProjectRepositoryImpl
import ir.hoboho.shooting.data.repository.ReferenceRepositoryImpl
import ir.hoboho.shooting.data.repository.SettingsRepositoryImpl
import ir.hoboho.shooting.data.repository.WorkdayRepositoryImpl
import ir.hoboho.shooting.domain.repository.BackupRepository
import ir.hoboho.shooting.domain.repository.ExpenseRepository
import ir.hoboho.shooting.domain.repository.ProjectRepository
import ir.hoboho.shooting.domain.repository.ReferenceRepository
import ir.hoboho.shooting.domain.repository.SettingsRepository
import ir.hoboho.shooting.domain.repository.WorkdayRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton abstract fun bindProjects(impl: ProjectRepositoryImpl): ProjectRepository
    @Binds @Singleton abstract fun bindWorkdays(impl: WorkdayRepositoryImpl): WorkdayRepository
    @Binds @Singleton abstract fun bindExpenses(impl: ExpenseRepositoryImpl): ExpenseRepository
    @Binds @Singleton abstract fun bindReferences(impl: ReferenceRepositoryImpl): ReferenceRepository
    @Binds @Singleton abstract fun bindSettings(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds @Singleton abstract fun bindBackup(impl: BackupRepositoryImpl): BackupRepository
}
