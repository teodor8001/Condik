package com.example.workipi.di

import com.example.workipi.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import com.example.workipi.di.AdminAuthClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
        install(Storage)
    }

    /**
     * Client secundar izolat pentru crearea conturilor de catre admin. Foloseste un session manager
     * in memorie si nu incarca/salveaza sesiunea pe disc, ca sesiunea adminului (de pe clientul
     * principal) sa ramana neatinsa cand facem signUp pentru un angajat nou.
     */
    @Provides
    @Singleton
    @AdminAuthClient
    fun provideAdminAuthClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
    ) {
        install(Auth) {
            autoLoadFromStorage = false
            autoSaveToStorage = false
            alwaysAutoRefresh = false
            sessionManager = MemorySessionManager()
        }
        install(Postgrest)
    }
}
