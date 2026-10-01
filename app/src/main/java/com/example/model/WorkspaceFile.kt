package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspace_files")
data class WorkspaceFile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val content: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val isReadOnly: Boolean = false,
    val isMainEntry: Boolean = false
)
