package com.example.guardiantrackapp.di

import android.content.Context
import androidx.room.Room
import com.example.guardiantrackapp.BuildConfig
import com.example.guardiantrackapp.data.local.datastore.PreferencesManager
import com.example.guardiantrackapp.data.local.db.GuardianDatabase
import com.example.guardiantrackapp.data.local.db.dao.EmergencyContactDao
import com.example.guardiantrackapp.data.local.db.dao.IncidentDao
import com.example.guardiantrackapp.data.remote.api.GuardianApiService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // --- Database ---

    @Provides
    @Singleton
    fun provideGuardianDatabase(
        @ApplicationContext context: Context
    ): GuardianDatabase {
        return Room.databaseBuilder(
            context,
            GuardianDatabase::class.java,
            GuardianDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideIncidentDao(database: GuardianDatabase): IncidentDao {
        return database.incidentDao()
    }

    @Provides
    @Singleton
    fun provideEmergencyContactDao(database: GuardianDatabase): EmergencyContactDao {
        return database.emergencyContactDao()
    }

    // --- Network ---

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideGuardianApiService(retrofit: Retrofit): GuardianApiService {
        return retrofit.create(GuardianApiService::class.java)
    }

    // --- Location ---

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    // --- Preferences ---

    @Provides
    @Singleton
    fun providePreferencesManager(
        @ApplicationContext context: Context
    ): PreferencesManager {
        return PreferencesManager(context)
    }
}
