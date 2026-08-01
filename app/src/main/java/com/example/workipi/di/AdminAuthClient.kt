package com.example.workipi.di

import javax.inject.Qualifier

/**
 * Marcheaza un [io.github.jan.supabase.SupabaseClient] secundar, izolat, folosit DOAR pentru ca
 * adminul sa creeze conturi de angajati. Are propriul session manager in memorie, deci crearea unui
 * cont nou (signUp) NU suprascrie sesiunea persistenta a adminului de pe clientul principal.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AdminAuthClient
