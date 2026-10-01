package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pip_packages")
data class PipPackage(
    @PrimaryKey
    val name: String,
    val summary: String,
    val latestVersion: String,
    val installedVersion: String? = null,
    val author: String = "",
    val license: String = "MIT / Apache 2.0",
    val homePage: String = "",
    val category: String = "General",
    val isInstalled: Boolean = false,
    val isInstalling: Boolean = false,
    val installProgress: Float = 0f,
    val sampleUsage: String = ""
)
