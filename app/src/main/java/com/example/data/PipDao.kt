package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.PipPackage
import kotlinx.coroutines.flow.Flow

@Dao
interface PipDao {
    @Query("SELECT * FROM pip_packages ORDER BY name ASC")
    fun getAllPackages(): Flow<List<PipPackage>>

    @Query("SELECT * FROM pip_packages WHERE name = :name LIMIT 1")
    suspend fun getPackageByName(name: String): PipPackage?

    @Query("SELECT * FROM pip_packages WHERE isInstalled = 1 ORDER BY name ASC")
    fun getInstalledPackages(): Flow<List<PipPackage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackage(pkg: PipPackage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackages(pkgs: List<PipPackage>)

    @Update
    suspend fun updatePackage(pkg: PipPackage)

    @Delete
    suspend fun deletePackage(pkg: PipPackage)

    @Query("SELECT COUNT(*) FROM pip_packages")
    suspend fun countPackages(): Int
}
