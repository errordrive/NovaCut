package com.procut.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAtUs: Long,
    val updatedAtUs: Long,
    val durationUs: Long,
    val coverImagePath: String?,
    val serializedJson: String
)
