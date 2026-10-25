package com.carlens.data.di

import android.content.Context
import androidx.room.Room
import com.carlens.data.db.CarLensDatabase
import com.carlens.data.repository.AttachmentRepositoryImpl
import com.carlens.data.repository.BackupRepositoryImpl
import com.carlens.data.repository.DocumentRepositoryImpl
import com.carlens.data.repository.ExpenseRepositoryImpl
import com.carlens.data.repository.FuelRepositoryImpl
import com.carlens.data.repository.MaintenanceRepositoryImpl
import com.carlens.data.repository.OdometerRepositoryImpl
import com.carlens.data.repository.ReminderRepositoryImpl
import com.carlens.data.repository.ServiceRepositoryImpl
import com.carlens.data.repository.SettingsRepositoryImpl
import com.carlens.data.repository.VehicleRepositoryImpl
import com.carlens.domain.repository.AttachmentRepository
import com.carlens.domain.repository.BackupRepository
import com.carlens.domain.repository.DocumentRepository
import com.carlens.domain.repository.ExpenseRepository
import com.carlens.domain.repository.FuelRepository
import com.carlens.domain.repository.MaintenanceRepository
import com.carlens.domain.repository.OdometerRepository
import com.carlens.domain.repository.ReminderRepository
import com.carlens.domain.repository.ServiceRepository
import com.carlens.domain.repository.SettingsRepository
import com.carlens.domain.repository.VehicleRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindsModule {
    @Binds @Singleton abstract fun bindVehicle(impl: VehicleRepositoryImpl): VehicleRepository
    @Binds @Singleton abstract fun bindOdometer(impl: OdometerRepositoryImpl): OdometerRepository
    @Binds @Singleton abstract fun bindFuel(impl: FuelRepositoryImpl): FuelRepository
    @Binds @Singleton abstract fun bindService(impl: ServiceRepositoryImpl): ServiceRepository
    @Binds @Singleton abstract fun bindExpense(impl: ExpenseRepositoryImpl): ExpenseRepository
    @Binds @Singleton abstract fun bindMaintenance(impl: MaintenanceRepositoryImpl): MaintenanceRepository
    @Binds @Singleton abstract fun bindDocument(impl: DocumentRepositoryImpl): DocumentRepository
    @Binds @Singleton abstract fun bindAttachment(impl: AttachmentRepositoryImpl): AttachmentRepository
    @Binds @Singleton abstract fun bindSettings(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds @Singleton abstract fun bindReminder(impl: ReminderRepositoryImpl): ReminderRepository
    @Binds @Singleton abstract fun bindBackup(impl: BackupRepositoryImpl): BackupRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataProvidersModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CarLensDatabase =
        Room.databaseBuilder(context, CarLensDatabase::class.java, CarLensDatabase.NAME)
            .build()

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
