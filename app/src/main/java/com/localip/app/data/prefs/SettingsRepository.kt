package com.localip.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 第 04 屏「设置」对应的全部偏好项 */
data class AppSettings(
    /** 插入网卡自动检测（注意：DHCP 由系统完成，这个开关控制的是"是否自动刷新并记录"） */
    val autoDetect: Boolean = true,
    /** 连接成功时通知 */
    val notifyOnConnect: Boolean = true,
    /** 自动复制 IP 到剪贴板 */
    val autoCopyIp: Boolean = false,
    /** IP 显示格式：dotted / cidr */
    val ipFormat: String = FORMAT_DOTTED,
    /** 记录保留条数 */
    val recordLimit: Int = 20,

    // ---- 测速（iperf3）参数，切换页面后仍然记住上次的填法 ----
    val speedHost: String = "",
    val speedPort: Int = 5201,
    /** TCP / UDP */
    val speedProtocol: String = "TCP",
    /** DOWNLOAD / UPLOAD / BOTH */
    val speedDirection: String = "DOWNLOAD",
    val speedDuration: Int = 10,
    val speedStreams: Int = 4,
) {
    companion object {
        const val FORMAT_DOTTED = "dotted"
        const val FORMAT_CIDR = "cidr"
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val keyAutoDetect = booleanPreferencesKey("auto_detect")
    private val keyNotify = booleanPreferencesKey("notify_on_connect")
    private val keyAutoCopy = booleanPreferencesKey("auto_copy_ip")
    private val keyFormat = stringPreferencesKey("ip_format")
    private val keyLimit = intPreferencesKey("record_limit")

    /**
     * 上一次写入历史的连接标识（`ip|source|iface`）。
     * 持久化它是为了：**重启 App 时不会因为"重新观察到同一个连接"而重复插记录**。
     */
    private val keyLastRecord = stringPreferencesKey("last_record_key")

    // 测速参数
    private val keySpeedHost = stringPreferencesKey("speed_host")
    private val keySpeedPort = intPreferencesKey("speed_port")
    private val keySpeedProto = stringPreferencesKey("speed_protocol")
    private val keySpeedDirection = stringPreferencesKey("speed_direction")
    private val keySpeedDuration = intPreferencesKey("speed_duration")
    private val keySpeedStreams = intPreferencesKey("speed_streams")

    val lastRecordKey: Flow<String?> = context.dataStore.data.map { it[keyLastRecord] }

    val flow: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            autoDetect = p[keyAutoDetect] ?: true,
            notifyOnConnect = p[keyNotify] ?: true,
            autoCopyIp = p[keyAutoCopy] ?: false,
            ipFormat = p[keyFormat] ?: AppSettings.FORMAT_DOTTED,
            recordLimit = p[keyLimit] ?: 20,
            speedHost = p[keySpeedHost] ?: "",
            speedPort = p[keySpeedPort] ?: 5201,
            speedProtocol = p[keySpeedProto] ?: "TCP",
            speedDirection = p[keySpeedDirection] ?: "DOWNLOAD",
            speedDuration = p[keySpeedDuration] ?: 10,
            speedStreams = p[keySpeedStreams] ?: 4,
        )
    }

    suspend fun setAutoDetect(value: Boolean) = edit { it[keyAutoDetect] = value }
    suspend fun setNotifyOnConnect(value: Boolean) = edit { it[keyNotify] = value }
    suspend fun setAutoCopyIp(value: Boolean) = edit { it[keyAutoCopy] = value }
    suspend fun setIpFormat(value: String) = edit { it[keyFormat] = value }
    suspend fun setRecordLimit(value: Int) = edit { it[keyLimit] = value }
    suspend fun setLastRecordKey(value: String) = edit { it[keyLastRecord] = value }

    suspend fun setSpeedHost(value: String) = edit { it[keySpeedHost] = value }
    suspend fun setSpeedPort(value: Int) = edit { it[keySpeedPort] = value }
    suspend fun setSpeedProtocol(value: String) = edit { it[keySpeedProto] = value }
    suspend fun setSpeedDirection(value: String) = edit { it[keySpeedDirection] = value }
    suspend fun setSpeedDuration(value: Int) = edit { it[keySpeedDuration] = value }
    suspend fun setSpeedStreams(value: Int) = edit { it[keySpeedStreams] = value }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
