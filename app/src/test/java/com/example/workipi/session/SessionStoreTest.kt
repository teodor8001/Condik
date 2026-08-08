package com.example.workipi.session

import com.example.workipi.data.model.AppPermission
import com.example.workipi.data.model.User
import com.example.workipi.data.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionStoreTest {
    private val profile = User(
        idUser = 42,
        fullName = "Test User",
        email = "test@example.com",
        phoneNumber = 700000000,
        role = "sef_echipa",
        idCompany = 7,
        needsPasswordChange = true,
    )

    @Test
    fun openPublishesProfileAndServerPermissions() {
        val store = SessionStore()

        store.open(profile, setOf(AppPermission.SITE_VIEW, AppPermission.TIME_ENTRIES_REVIEW))

        val state = store.state.value
        assertTrue(state.isAuthenticated)
        assertEquals(42L, state.user?.id)
        assertEquals(7L, state.user?.companyId)
        assertEquals(UserRole.SEF_ECHIPA, state.user?.role)
        assertTrue(state.hasPermission(AppPermission.SITE_VIEW))
        assertFalse(state.hasPermission(AppPermission.FINANCIALS_VIEW))
    }

    @Test
    fun passwordChangeUpdatesThePublishedSession() {
        val store = SessionStore()
        store.open(profile, setOf(AppPermission.SETTINGS_VIEW))

        store.markPasswordChanged()

        assertFalse(store.state.value.user?.needsPasswordChange ?: true)
    }

    @Test
    fun clearRemovesUserAndPermissions() {
        val store = SessionStore()
        store.open(profile, setOf(AppPermission.SITE_VIEW))

        store.clear()

        assertNull(store.state.value.user)
        assertTrue(store.state.value.permissions.isEmpty())
        assertFalse(store.state.value.isAuthenticated)
    }
}
