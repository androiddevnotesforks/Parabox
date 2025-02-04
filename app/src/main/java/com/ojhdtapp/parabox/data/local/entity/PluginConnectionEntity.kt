package com.ojhdtapp.parabox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ojhdtapp.parabox.domain.model.PluginConnection

@Entity(tableName = "plugin_connection_entity")
data class PluginConnectionEntity(
    val connectionType: Int,
    @PrimaryKey val objectId: Long,
    val id: Long,
) {
    fun toPluginConnection(): PluginConnection {
        return PluginConnection(
            connectionType = connectionType,
            objectId = objectId,
            id = id
        )
    }
    fun toKitPluginConnection(sendTargetType: Int): com.ojhdtapp.paraboxdevelopmentkit.messagedto.PluginConnection{
        return com.ojhdtapp.paraboxdevelopmentkit.messagedto.PluginConnection(
            connectionType = connectionType,
            sendTargetType = sendTargetType,
            id = id
        )
    }
}
