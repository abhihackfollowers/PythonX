package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.WorkspaceFile
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM workspace_files ORDER BY isMainEntry DESC, name ASC")
    fun getAllFiles(): Flow<List<WorkspaceFile>>

    @Query("SELECT * FROM workspace_files WHERE id = :id LIMIT 1")
    suspend fun getFileById(id: Long): WorkspaceFile?

    @Query("SELECT * FROM workspace_files WHERE name = :name LIMIT 1")
    suspend fun getFileByName(name: String): WorkspaceFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: WorkspaceFile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<WorkspaceFile>)

    @Update
    suspend fun updateFile(file: WorkspaceFile)

    @Delete
    suspend fun deleteFile(file: WorkspaceFile)

    @Query("DELETE FROM workspace_files WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM workspace_files")
    suspend fun countFiles(): Int
}
