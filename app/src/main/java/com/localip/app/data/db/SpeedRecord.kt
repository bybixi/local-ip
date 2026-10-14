package com.localip.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speed_record")
data class SpeedRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val host: String,
    val port: Int,
    /** TCP / UDP */
    val protocol: String,
    /** DOWNLOAD / UPLOAD / BOTH */
    val direction: String,
    val streams: Int,
    /** 实测带宽（Mbps） */
    val mbps: Double,
    val retransmits: Int,
    val jitterMs: Double?,
    val lostPackets: Int,
    val timestamp: Long,
)
