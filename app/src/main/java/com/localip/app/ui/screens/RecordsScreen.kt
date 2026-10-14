package com.localip.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localip.app.R
import com.localip.app.core.net.NetSource
import com.localip.app.data.db.ConnRecord
import com.localip.app.domain.AppViewModel
import com.localip.app.ui.components.EmptyRecords
import com.localip.app.ui.components.RecordCard
import com.localip.app.ui.components.ScreenScaffold
import com.localip.app.ui.theme.AppColors
import com.localip.app.ui.theme.AppTypo
import com.localip.app.ui.theme.D
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 03 屏：连接记录（状态容器） */
@Composable
fun RecordsScreen(
    vm: AppViewModel,
    modifier: Modifier = Modifier,
) {
    val records by vm.records.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val net by vm.net.collectAsStateWithLifecycle()

    RecordsContent(
        records = records,
        recordLimit = settings.recordLimit,
        currentIp = net.ipv4,
        currentSource = net.source,
        sourceLabel = vm::sourceLabel,
        onClear = vm::openClearDialog,
        modifier = modifier,
    )
}

/** 03 屏：连接记录（无状态内容） */
@Composable
fun RecordsContent(
    records: List<ConnRecord>,
    recordLimit: Int,
    currentIp: String?,
    currentSource: NetSource,
    sourceLabel: (NetSource) -> String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(gap = D.GapRecords, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.title_records),
                    style = AppTypo.pageTitle22,
                    color = AppColors.TextPrimary,
                )
                Text(
                    text = stringResource(R.string.records_subtitle, recordLimit),
                    style = AppTypo.caption,
                    color = AppColors.TextTertiary,
                )
            }
            ClearMiniButton(onClick = onClear)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(D.GapRecords),
        ) {
            if (records.isEmpty()) {
                item {
                    EmptyRecords(
                        title = stringResource(R.string.records_empty_title),
                        desc = stringResource(R.string.records_empty_desc),
                    )
                }
            }
            items(records, key = { it.id }) { record ->
                val current = record.ip == currentIp && record.sourceKey == currentSource.name
                RecordCard(
                    ip = record.ip,
                    statusText = stringResource(
                        if (current) R.string.status_current else R.string.status_disconnected
                    ),
                    metaText = metaOf(record, sourceLabel),
                    current = current,
                )
            }
        }
    }
}

@Composable
private fun metaOf(record: ConnRecord, sourceLabel: (NetSource) -> String): String {
    val parts = ArrayList<String>(3)
    val source = runCatching { NetSource.valueOf(record.sourceKey) }.getOrDefault(NetSource.NONE)
    parts += sourceLabel(source)
    if (!record.detail.isNullOrBlank()) parts += record.detail
    parts += timeLabel(record.timestamp)
    return parts.joinToString(" · ")
}

/** 今天 14:22 / 昨天 09:41 / 10月3日 16:05 —— 与设计稿的记录时间格式一致 */
@Composable
private fun timeLabel(timestamp: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = timestamp }
    val hhmm = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

    if (sameDay(now, then)) return stringResource(R.string.time_today, hhmm)

    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    if (sameDay(yesterday, then)) return stringResource(R.string.time_yesterday, hhmm)

    return stringResource(
        R.string.time_date,
        then.get(Calendar.MONTH) + 1,
        then.get(Calendar.DAY_OF_MONTH),
        hhmm,
    )
}

private fun sameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

/** 标题右侧的「清空」小按钮（带描边，破坏性操作不做成纯文字） */
@Composable
private fun ClearMiniButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(D.MiniBtnR)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(AppColors.Card)
            .border(1.dp, AppColors.StrokeNeutral, shape)
            .clickable { onClick() }
            .padding(horizontal = D.MiniBtnPadH, vertical = D.MiniBtnPadV),
    ) {
        Text(
            text = stringResource(R.string.btn_clear),
            style = AppTypo.captionMedium,
            color = AppColors.TextSecondary,
        )
    }
}
