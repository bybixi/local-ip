package com.localip.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localip.app.R
import com.localip.app.core.iperf.IperfConfig
import com.localip.app.core.iperf.IperfDirection
import com.localip.app.core.iperf.IperfOutput
import com.localip.app.core.iperf.IperfProgress
import com.localip.app.core.iperf.IperfProtocol
import com.localip.app.core.iperf.IperfResult
import com.localip.app.core.net.NetState
import com.localip.app.data.db.SpeedRecord
import com.localip.app.data.prefs.AppSettings
import com.localip.app.domain.ServerUiState
import com.localip.app.domain.SpeedFailure
import com.localip.app.domain.SpeedRole
import com.localip.app.domain.SpeedUiState
import com.localip.app.domain.SpeedViewModel
import com.localip.app.ui.components.AppButton
import com.localip.app.ui.components.HintBar
import com.localip.app.ui.components.HintTone
import com.localip.app.ui.components.InfoRow
import com.localip.app.ui.components.PageHeader
import com.localip.app.ui.components.PulseDot
import com.localip.app.ui.components.ScreenScaffold
import com.localip.app.ui.components.SectionCard
import com.localip.app.ui.components.SectionHeader
import com.localip.app.ui.components.SingleChoiceDialog
import com.localip.app.ui.components.TextInputDialog
import com.localip.app.ui.components.ThinDivider
import com.localip.app.ui.components.ThinProgress
import com.localip.app.ui.PhoneFrame
import com.localip.app.ui.nav.Dest
import com.localip.app.ui.screens.speed.SpeedHeroCard
import com.localip.app.ui.screens.speed.SpeedSegment
import com.localip.app.ui.screens.speed.speedText
import com.localip.app.ui.sharedAxisZ
import com.localip.app.ui.srcLabel
import com.localip.app.ui.theme.AppColors
import com.localip.app.ui.theme.AppTypo
import com.localip.app.ui.theme.D
import com.localip.app.ui.theme.LocalIpTheme
import java.util.Calendar
import java.util.Locale

private enum class SpeedDialog { None, Host, Port, Protocol, Duration, Direction, Streams }

/** 测速页当前的"身体"，驱动 AnimatedContent 的层级切换 */
private sealed interface SpeedBody {
    data object Idle : SpeedBody
    data class Running(val progress: IperfProgress, val config: IperfConfig) : SpeedBody
    data class Done(val result: IperfResult) : SpeedBody
    data class Failed(val reason: SpeedFailure, val detail: String?) : SpeedBody
    data class Server(val state: ServerUiState) : SpeedBody
}

@Composable
fun SpeedScreen(
    vm: SpeedViewModel,
    modifier: Modifier = Modifier,
) {
    val role by vm.role.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val clientState by vm.client.collectAsStateWithLifecycle()
    val serverState by vm.server.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val net by vm.net.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var dialog by remember { mutableStateOf(SpeedDialog.None) }

    LaunchedEffect(Unit) {
        vm.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    // 离开本页（切走 Tab 或退出）即停止 iperf3 —— 底部导航用了 saveState/restoreState，
    // ViewModel 会存活，不显式停的话服务端会一直在后台占着 5201 端口。
    DisposableEffect(Unit) {
        onDispose {
            vm.stopTest()
            vm.stopServer()
        }
    }

    val body: SpeedBody = when {
        role == SpeedRole.SERVER -> SpeedBody.Server(serverState)
        clientState is SpeedUiState.Running ->
            SpeedBody.Running((clientState as SpeedUiState.Running).progress,
                (clientState as SpeedUiState.Running).config)
        clientState is SpeedUiState.Done -> SpeedBody.Done((clientState as SpeedUiState.Done).result)
        clientState is SpeedUiState.Failed ->
            SpeedBody.Failed((clientState as SpeedUiState.Failed).reason,
                (clientState as SpeedUiState.Failed).detail)
        else -> SpeedBody.Idle
    }

    ScreenScaffold(gap = D.GapSection, modifier = modifier) {
        PageHeader(
            title = stringResource(R.string.tab_speed),
            subtitle = when (val b = body) {
                is SpeedBody.Running -> stringResource(
                    R.string.speed_running_subtitle,
                    stringResource(b.config.protocol.labelRes()),
                    stringResource(b.config.direction.labelRes()),
                )
                is SpeedBody.Server -> stringResource(R.string.speed_subtitle_server)
                else -> stringResource(R.string.speed_subtitle)
            },
        )

        SpeedSegment(role = role, onSelect = vm::setRole)

        AnimatedContent(
            targetState = body,
            transitionSpec = { sharedAxisZ() },
            label = "speed-body",
        ) { target ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(D.GapSection),
            ) {
                when (target) {
                    is SpeedBody.Server -> ServerContent(
                        state = target.state,
                        settings = settings,
                        net = net,
                        onStart = vm::startServer,
                        onStop = vm::stopServer,
                    )

                    is SpeedBody.Running -> RunningContent(
                        progress = target.progress,
                        config = target.config,
                        onStop = vm::stopTest,
                    )

                    is SpeedBody.Done -> DoneContent(
                        result = target.result,
                        onAgain = vm::resetTest,
                    )

                    is SpeedBody.Failed -> FailedContent(
                        reason = target.reason,
                        detail = target.detail,
                        onRetry = vm::resetTest,
                    )

                    is SpeedBody.Idle -> IdleContent(
                        settings = settings,
                        available = vm.binaryAvailable,
                        onOpen = { dialog = it },
                        onStart = vm::startTest,
                    )
                }
            }
        }

        if (body is SpeedBody.Idle || body is SpeedBody.Done) {
            SectionHeader(title = stringResource(R.string.sec_speed_history))
            SectionCard {
                if (history.isEmpty()) {
                    Text(
                        text = stringResource(R.string.speed_history_empty),
                        style = AppTypo.caption,
                        color = AppColors.TextTertiary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                    )
                } else {
                    history.forEachIndexed { index, record ->
                        if (index > 0) ThinDivider()
                        SpeedHistoryRow(record)
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------- 参数弹窗
    when (dialog) {
        SpeedDialog.None -> Unit

        SpeedDialog.Host -> TextInputDialog(
            title = stringResource(R.string.pick_host_title),
            initial = settings.speedHost,
            placeholder = "192.168.1.100",
            keyboardType = KeyboardType.Uri,
            onConfirm = {
                vm.setSpeedHost(it)
                dialog = SpeedDialog.None
            },
            onDismiss = { dialog = SpeedDialog.None },
        )

        SpeedDialog.Port -> TextInputDialog(
            title = stringResource(R.string.pick_port_title),
            initial = settings.speedPort.toString(),
            keyboardType = KeyboardType.Number,
            onConfirm = {
                it.toIntOrNull()?.takeIf { p -> p in 1..65535 }?.let(vm::setSpeedPort)
                dialog = SpeedDialog.None
            },
            onDismiss = { dialog = SpeedDialog.None },
        )

        SpeedDialog.Protocol -> SingleChoiceDialog(
            title = stringResource(R.string.pick_proto_title),
            options = IperfProtocol.entries.map { it.name to it.name },
            selected = settings.speedProtocol,
            onPick = { vm.setSpeedProtocol(it); dialog = SpeedDialog.None },
            onDismiss = { dialog = SpeedDialog.None },
        )

        SpeedDialog.Duration -> SingleChoiceDialog(
            title = stringResource(R.string.pick_duration_title),
            options = listOf(5, 10, 30, 60).map {
                it.toString() to stringResource(R.string.duration_fmt, it)
            },
            selected = settings.speedDuration.toString(),
            onPick = {
                it.toIntOrNull()?.let(vm::setSpeedDuration)
                dialog = SpeedDialog.None
            },
            onDismiss = { dialog = SpeedDialog.None },
        )

        SpeedDialog.Direction -> SingleChoiceDialog(
            title = stringResource(R.string.pick_direction_title),
            options = IperfDirection.entries.map {
                it.name to stringResource(it.labelRes())
            },
            selected = settings.speedDirection,
            onPick = { vm.setSpeedDirection(it); dialog = SpeedDialog.None },
            onDismiss = { dialog = SpeedDialog.None },
        )

        SpeedDialog.Streams -> SingleChoiceDialog(
            title = stringResource(R.string.pick_streams_title),
            options = listOf(1, 2, 4, 8).map {
                it.toString() to stringResource(R.string.streams_fmt, it)
            },
            selected = settings.speedStreams.toString(),
            onPick = {
                it.toIntOrNull()?.let(vm::setSpeedStreams)
                dialog = SpeedDialog.None
            },
            onDismiss = { dialog = SpeedDialog.None },
        )
    }
}

// ---------------------------------------------------------------------------
// 各状态的内容
// ---------------------------------------------------------------------------

@Composable
private fun IdleContent(
    settings: AppSettings,
    available: Boolean,
    onOpen: (SpeedDialog) -> Unit,
    onStart: () -> Unit,
) {
    SectionHeader(title = stringResource(R.string.sec_server))
    SectionCard {
        InfoRow(
            label = stringResource(R.string.row_host),
            value = settings.speedHost.ifBlank { "—" },
            chevron = true,
            valueStyle = AppTypo.monoValue,
            modifier = Modifier.clickable { onOpen(SpeedDialog.Host) },
        )
        ThinDivider()
        InfoRow(
            label = stringResource(R.string.row_port),
            value = settings.speedPort.toString(),
            chevron = true,
            modifier = Modifier.clickable { onOpen(SpeedDialog.Port) },
        )
    }

    SectionHeader(title = stringResource(R.string.sec_params))
    SectionCard {
        InfoRow(
            label = stringResource(R.string.row_proto),
            value = settings.speedProtocol,
            chevron = true,
            modifier = Modifier.clickable { onOpen(SpeedDialog.Protocol) },
        )
        ThinDivider()
        InfoRow(
            label = stringResource(R.string.row_duration),
            value = stringResource(R.string.duration_fmt, settings.speedDuration),
            chevron = true,
            modifier = Modifier.clickable { onOpen(SpeedDialog.Duration) },
        )
        ThinDivider()
        InfoRow(
            label = stringResource(R.string.row_direction),
            value = stringResource(
                runCatching { IperfDirection.valueOf(settings.speedDirection) }
                    .getOrDefault(IperfDirection.DOWNLOAD).labelRes()
            ),
            chevron = true,
            modifier = Modifier.clickable { onOpen(SpeedDialog.Direction) },
        )
        ThinDivider()
        InfoRow(
            label = stringResource(R.string.row_streams),
            value = stringResource(R.string.streams_fmt, settings.speedStreams),
            chevron = true,
            modifier = Modifier.clickable { onOpen(SpeedDialog.Streams) },
        )
    }

    if (!available) {
        HintBar(
            text = stringResource(R.string.speed_error_no_binary),
            tone = HintTone.Danger,
            icon = R.drawable.ic_warning,
        )
    }

    AppButton(
        text = stringResource(R.string.btn_start_speed),
        onClick = onStart,
        modifier = Modifier.fillMaxWidth(),
        height = 50.dp,
        enabled = available,
    )
}

@Composable
private fun RunningContent(
    progress: IperfProgress,
    config: IperfConfig,
    onStop: () -> Unit,
) {
    SpeedHeroCard(
        chipText = stringResource(R.string.speed_chip_running),
        tone = AppColors.AccentCyan,
        chipLeading = { PulseDot(AppColors.AccentCyan) },
        trailingText = stringResource(R.string.speed_remaining_fmt, progress.remainingSec),
        value = speedText(progress.mbps),
        unit = "Mbps",
        caption = stringResource(
            R.string.speed_running_subtitle,
            stringResource(config.protocol.labelRes()),
            stringResource(config.direction.labelRes()),
        ),
        extra = { ThinProgress(fraction = progress.fraction) },
        buttonText = stringResource(R.string.btn_stop_speed),
        buttonContainer = AppColors.DisabledTrack,
        buttonContent = AppColors.TextPrimary,
        onButton = onStop,
    )

    SectionHeader(title = stringResource(R.string.sec_live))
    SectionCard {
        InfoRow(
            label = stringResource(R.string.row_transfer),
            value = IperfOutput.formatBytes(progress.transferBytes),
        )
        ThinDivider()
        InfoRow(
            label = stringResource(R.string.row_retr),
            value = progress.retransmits.toString(),
        )
        ThinDivider()
        InfoRow(
            label = stringResource(R.string.row_jitter),
            value = progress.jitterMs?.let { String.format(Locale.US, "%.3f ms", it) } ?: "—",
        )
        ThinDivider()
        InfoRow(
            label = stringResource(R.string.row_loss),
            value = if (progress.totalPackets > 0) {
                "${progress.lostPackets}/${progress.totalPackets}"
            } else {
                "—"
            },
        )
    }
}

@Composable
private fun DoneContent(
    result: IperfResult,
    onAgain: () -> Unit,
) {
    SpeedHeroCard(
        chipText = stringResource(R.string.speed_chip_done),
        tone = AppColors.Success,
        trailingText = result.host,
        value = speedText(result.mbps),
        unit = "Mbps",
        caption = stringResource(
            R.string.speed_history_meta_fmt,
            result.protocol.name,
            stringResource(result.direction.labelRes()),
            result.streams,
        ),
        buttonText = stringResource(R.string.btn_test_again),
        buttonContainer = AppColors.AccentCyan,
        buttonContent = AppColors.OnAccent,
        onButton = onAgain,
    )

    SectionHeader(title = stringResource(R.string.sec_result))
    SectionCard {
        InfoRow(
            label = stringResource(R.string.row_transfer),
            value = IperfOutput.formatBytes(
                result.mbps * 1e6 / 8.0 * result.durationSec
            ),
        )
        ThinDivider()
        InfoRow(stringResource(R.string.row_retr), result.retransmits.toString())
        ThinDivider()
        InfoRow(
            stringResource(R.string.row_jitter),
            result.jitterMs?.let { String.format(Locale.US, "%.3f ms", it) } ?: "—",
        )
        ThinDivider()
        InfoRow(
            stringResource(R.string.row_loss),
            if (result.totalPackets > 0) "${result.lostPackets}/${result.totalPackets}" else "—",
        )
    }
}

@Composable
private fun FailedContent(
    reason: SpeedFailure,
    detail: String?,
    onRetry: () -> Unit,
) {
    HintBar(
        text = when (reason) {
            SpeedFailure.NO_BINARY -> stringResource(R.string.speed_error_no_binary)
            SpeedFailure.NEED_HOST -> stringResource(R.string.speed_error_host)
            SpeedFailure.RUNNER_ERROR ->
                stringResource(R.string.speed_error_fmt, detail.orEmpty())
        },
        tone = HintTone.Danger,
        icon = R.drawable.ic_warning,
    )
    // 失败态必须给一条出路，否则用户会卡在这一屏
    AppButton(
        text = stringResource(R.string.btn_test_again),
        onClick = onRetry,
        modifier = Modifier.fillMaxWidth(),
        container = AppColors.BtnNeutral,
        content = AppColors.TextPrimary,
        height = 50.dp,
    )
}

@Composable
private fun ServerContent(
    state: ServerUiState,
    settings: AppSettings,
    net: NetState,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val listening = state is ServerUiState.Listening
    val ip = net.ipv4 ?: "—"

    SpeedHeroCard(
        chipText = if (listening) {
            stringResource(R.string.server_chip_fmt, settings.speedPort.toString())
        } else {
            stringResource(R.string.speed_role_server)
        },
        tone = AppColors.Success,
        trailingText = "TCP",
        value = ip,
        unit = null,
        caption = when {
            state is ServerUiState.Listening && state.clients > 0 ->
                stringResource(R.string.server_connected_fmt, state.clients)
            listening -> stringResource(R.string.server_waiting)
            else -> null
        },
        buttonText = stringResource(
            if (listening) R.string.btn_stop_server else R.string.btn_start_server
        ),
        buttonContainer = if (listening) AppColors.DisabledTrack else AppColors.AccentCyan,
        buttonContent = if (listening) AppColors.TextPrimary else AppColors.OnAccent,
        onButton = { if (listening) onStop() else onStart() },
    )

    if (state is ServerUiState.Failed) {
        HintBar(
            text = when (state.reason) {
                SpeedFailure.NO_BINARY -> stringResource(R.string.speed_error_no_binary)
                else -> stringResource(R.string.speed_error_fmt, state.detail.orEmpty())
            },
            tone = HintTone.Danger,
            icon = R.drawable.ic_warning,
        )
    }

    SectionHeader(title = stringResource(R.string.sec_cmd))
    SectionCard {
        InfoRow(
            label = stringResource(R.string.row_command),
            value = stringResource(R.string.server_cmd_fmt, ip),
            valueStyle = AppTypo.monoValue.copy(color = AppColors.AccentCyan),
        )
    }

    HintBar(text = stringResource(R.string.hint_server), tone = HintTone.Cyan)
}

@Composable
private fun SpeedHistoryRow(record: SpeedRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = "${speedText(record.mbps)} Mbps",
                style = AppTypo.speedValue,
                color = AppColors.TextPrimary,
            )
            Text(
                text = if (record.direction == "SERVER") {
                    stringResource(R.string.speed_role_server)
                } else {
                    stringResource(
                        R.string.speed_history_meta_fmt,
                        record.protocol,
                        stringResource(
                            runCatching { IperfDirection.valueOf(record.direction) }
                                .getOrDefault(IperfDirection.DOWNLOAD).labelRes()
                        ),
                        record.streams,
                    )
                },
                style = AppTypo.caption,
                color = AppColors.TextTertiary,
            )
        }
        Text(
            text = speedTimeLabel(record.timestamp),
            style = AppTypo.monoTag,
            color = AppColors.TextTertiary,
        )
    }
}

private fun IperfProtocol.labelRes(): Int = when (this) {
    IperfProtocol.TCP -> R.string.proto_tcp
    IperfProtocol.UDP -> R.string.proto_udp
}

private fun IperfDirection.labelRes(): Int = when (this) {
    IperfDirection.DOWNLOAD -> R.string.direction_download
    IperfDirection.UPLOAD -> R.string.direction_upload
    IperfDirection.BOTH -> R.string.direction_both
}

@Composable
private fun speedTimeLabel(timestamp: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = timestamp }
    val hhmm = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
        .format(java.util.Date(timestamp))
    val sameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    if (sameDay) return stringResource(R.string.time_today, hhmm)
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    if (yesterday.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        yesterday.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    ) {
        return stringResource(R.string.time_yesterday, hhmm)
    }
    return stringResource(
        R.string.time_date,
        then.get(Calendar.MONTH) + 1,
        then.get(Calendar.DAY_OF_MONTH),
        hhmm,
    )
}

// ---------------------------------------------------------------------------
// 预览（放在本文件内，才能访问上面那些 private 的内容 composable）
// ---------------------------------------------------------------------------

private val previewSettings = AppSettings(
    speedHost = "192.168.1.100",
    speedPort = 5201,
    speedDuration = 10,
    speedStreams = 4,
)

@Preview(name = "测速 · 客户端待机", widthDp = 390, heightDp = 844)
@Composable
private fun PSpeedIdle() {
    LocalIpTheme {
        PhoneFrame(Dest.Speed) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(D.GapSection),
            ) {
                IdleContent(
                    settings = previewSettings,
                    available = true,
                    onOpen = {},
                    onStart = {},
                )
            }
        }
    }
}

@Preview(name = "测速 · 测速中", widthDp = 390, heightDp = 844)
@Composable
private fun PSpeedRunning() {
    LocalIpTheme {
        PhoneFrame(Dest.Speed) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(D.GapSection),
            ) {
                RunningContent(
                    progress = IperfProgress(
                        elapsedSec = 3.0,
                        totalSec = 10,
                        mbps = 942.0,
                        transferBytes = 3.42 * 1024 * 1024 * 1024,
                        retransmits = 12,
                    ),
                    config = IperfConfig(
                        host = "192.168.1.100",
                        protocol = IperfProtocol.TCP,
                        direction = IperfDirection.DOWNLOAD,
                        durationSec = 10,
                        streams = 4,
                    ),
                    onStop = {},
                )
            }
        }
    }
}

@Preview(name = "测速 · 服务端监听中", widthDp = 390, heightDp = 844)
@Composable
private fun PSpeedServer() {
    LocalIpTheme {
        PhoneFrame(Dest.Speed) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(D.GapSection),
            ) {
                ServerContent(
                    state = ServerUiState.Listening(5201, clients = 1, lastMbps = 499.0),
                    settings = previewSettings,
                    net = com.localip.app.core.net.NetState(
                        ipv4 = "192.168.10.51",
                        source = com.localip.app.core.net.NetSource.ETHERNET,
                    ),
                    onStart = {},
                    onStop = {},
                )
            }
        }
    }
}
