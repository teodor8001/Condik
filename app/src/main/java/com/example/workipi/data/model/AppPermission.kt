package com.example.workipi.data.model

import kotlinx.serialization.Serializable

enum class AppPermission(val dbValue: String) {
    DASHBOARD_VIEW("dashboard.view"),
    PROJECTS_VIEW("projects.view"),
    PROJECTS_CREATE("projects.create"),
    PROJECTS_MANAGE("projects.manage"),
    SITE_VIEW("site.view"),
    SITE_MANAGE("site.manage"),
    TEAM_VIEW("team.view"),
    TEAM_MANAGE("team.manage"),
    PERFORMANCE_VIEW("performance.view"),
    PERFORMANCE_MANAGE("performance.manage"),
    RESOURCES_VIEW("resources.view"),
    RESOURCES_MANAGE("resources.manage"),
    OFFERS_VIEW("offers.view"),
    OFFERS_MANAGE("offers.manage"),
    ADMINISTRATION_VIEW("administration.view"),
    ADMINISTRATION_MANAGE("administration.manage"),
    FINANCIALS_VIEW("financials.view"),
    TIME_ENTRIES_CREATE("time_entries.create"),
    TIME_ENTRIES_REVIEW("time_entries.review"),
    SETTINGS_VIEW("settings.view");

    companion object {
        fun fromDbValue(value: String): AppPermission? = entries.firstOrNull { it.dbValue == value }
    }
}

@Serializable
data class PermissionRow(val permission: String)
