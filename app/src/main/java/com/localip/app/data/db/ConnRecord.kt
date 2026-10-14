package com.localip.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conn_record")
data class ConnRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 当时拿到的 IPv4，例如 192.168.1.108 */
    val ip: String,
    /** 来源枚举名：ETHERNET / WIFI / CELLULAR / USB_TETHER */
    val sourceKey: String,
    /** 详情：网卡型号或 SSID */
    val detail: String?,
    /** 接口名：eth0 / wlan0，用于去重 */
    val ifaceName: String?,
    val timestamp: Long,
)
