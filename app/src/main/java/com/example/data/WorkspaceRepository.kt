package com.example.data

import com.example.model.WorkspaceFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WorkspaceRepository(private val workspaceDao: WorkspaceDao) {

    val allFiles: Flow<List<WorkspaceFile>> = workspaceDao.getAllFiles()

    suspend fun getFileById(id: Long): WorkspaceFile? = withContext(Dispatchers.IO) {
        workspaceDao.getFileById(id)
    }

    suspend fun saveFile(file: WorkspaceFile) = withContext(Dispatchers.IO) {
        if (file.id == 0L) {
            workspaceDao.insertFile(file)
        } else {
            workspaceDao.updateFile(file.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun createFile(name: String, content: String = ""): Long = withContext(Dispatchers.IO) {
        val safeName = if (name.endsWith(".py")) name else "$name.py"
        workspaceDao.insertFile(
            WorkspaceFile(
                name = safeName,
                content = content.ifEmpty { "# $safeName\n\nprint(\"Hello from $safeName!\")\n" }
            )
        )
    }

    suspend fun deleteFile(file: WorkspaceFile) = withContext(Dispatchers.IO) {
        workspaceDao.deleteFile(file)
    }

    suspend fun ensureDefaultTemplates() = withContext(Dispatchers.IO) {
        if (workspaceDao.countFiles() == 0) {
            workspaceDao.insertFiles(SampleTemplates.getInitialWorkspace())
        }
    }
}
