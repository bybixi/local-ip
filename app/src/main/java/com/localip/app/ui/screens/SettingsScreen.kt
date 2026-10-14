package com.localip.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localip.app.BuildConfig
import com.localip.app.R
import com.localip.app.data.prefs.AppSettings
import com.localip.app.domain.AppViewModel
import com.localip.app.ui.components.AppSwitch
import com.localip.app.ui.components.InfoDialog
import com.localip.app.ui.components.PageHeader
import com.localip.app.ui.components.ScreenScaffold
import com.localip.app.ui.components.SectionCard
import com.localip.app.ui.components.SectionHeader
import com.localip.app.ui.components.SingleChoiceDialog
import com.localip.app.ui.components.ThinDivider
import com.localip.app.ui.theme.AppColors
import com.localip.app.ui.theme.AppTypo
import com.localip.app.ui.theme.D

private enum class SettingDialog { None, Format, Limit, About }

/** 04 屏：设置（状态容器 —— 弹窗属于浮层，留在这一层） */
@Composable
fun SettingsScreen(
    vm: AppViewModel,
    modifier: Modifier = Modifier,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var dialog by remember { mutableStateOf(SettingDialog.None) }

    val notifyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        vm.setNotifyOnConnect(granted)
        if (!granted) vm.toast(context.getString(R.string.toast_notify_denied))
    }

    SettingsContent(
        settings = settings,
        versionName = BuildConfig.VERSION_NAME,
        onAutoDetect = vm::setAutoDetect,
        onNotify = { enabled ->
            if (enabled && needsNotificationPermission(context)) {
                notifyPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                vm.setNotifyOnConnect(enabled)
            }
        },
        onAutoCopy = vm::setAutoCopyIp,
        onOpenFormat = { dialog = SettingDialog.Format },
        onOpenLimit = { dialog = SettingDialog.Limit },
        onOpenAbout = { dialog = SettingDialog.About },
        onExport = {
            vm.exportCsv { uri ->
                if (uri == null) {
                    vm.toast(context.getString(R.string.toast_export_failed))
                } else {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(
                            intent,
                            context.getString(R.string.export_share_title),
                        )
                    )
                }
            }
        },
        modifier = modifier,
    )

    when (dialog) {
        SettingDialog.None -> Unit

        SettingDialog.Format -> SingleChoiceDialog(
            title = stringResource(R.string.pick_format_title),
            options = listOf(
                AppSettings.FORMAT_DOTTED to stringResource(R.string.val_dotted),
                AppSettings.FORMAT_CIDR to stringResource(R.string.val_cidr),
            ),
            selected = settings.ipFormat,
            onPick = {
                vm.setIpFormat(it)
                dialog = SettingDialog.None
            },
            onDismiss = { dialog = SettingDialog.None },
        )

        SettingDialog.Limit -> SingleChoiceDialog(
            title = stringResource(R.string.pick_limit_title),
            options = listOf(20, 50, 100).map {
                it.toString() to stringResource(R.string.limit_fmt, it)
            },
            selected = settings.recordLimit.toString(),
            onPick = {
                vm.setRecordLimit(it.toIntOrNull() ?: 20)
                dialog = SettingDialog.None
            },
            onDismiss = { dialog = SettingDialog.None },
        )

        SettingDialog.About -> InfoDialog(
            title = stringResource(R.string.about_title),
            body = stringResource(R.string.about_body, BuildConfig.VERSION_NAME),
            onDismiss = { dialog = SettingDialog.None },
        )
    }
}

/** 04 屏：设置（无状态内容） */
@Composable
fun SettingsContent(
    settings: AppSettings,
    versionName: String,
    onAutoDetect: (Boolean) -> Unit,
    onNotify: (Boolean) -> Unit,
    onAutoCopy: (Boolean) -> Unit,
    onOpenFormat: () -> Unit,
    onOpenLimit: () -> Unit,
    onOpenAbout: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(gap = D.GapSection, modifier = modifier) {
        PageHeader(
            title = stringResource(R.string.title_settings),
            subtitle = stringResource(R.string.settings_subtitle),
        )

        // ---------------- 自动检测 ----------------
        SectionHeader(title = stringResource(R.string.sec_auto))
        SectionCard {
            SwitchRow(
                title = stringResource(R.string.sw_auto_detect),
                desc = stringResource(R.string.sw_auto_detect_desc),
                checked = settings.autoDetect,
                onChange = onAutoDetect,
            )
            ThinDivider()
            SwitchRow(
                title = stringResource(R.string.sw_notify),
                desc = stringResource(R.string.sw_notify_desc),
                checked = settings.notifyOnConnect,
                onChange = onNotify,
            )
            ThinDivider()
            SwitchRow(
                title = stringResource(R.string.sw_autocopy),
                desc = stringResource(R.string.sw_autocopy_desc),
                checked = settings.autoCopyIp,
                onChange = onAutoCopy,
            )
        }

        // ---------------- 通用 ----------------
        SectionHeader(title = stringResource(R.string.sec_common))
        SectionCard {
            NavRow(
                label = stringResource(R.string.row_format),
                value = if (settings.ipFormat == AppSettings.FORMAT_CIDR) {
                    stringResource(R.string.val_cidr)
                } else {
                    stringResource(R.string.val_dotted)
                },
                onClick = onOpenFormat,
            )
            ThinDivider()
            NavRow(
                label = stringResource(R.string.row_limit),
                value = stringResource(R.string.limit_fmt, settings.recordLimit),
                onClick = onOpenLimit,
            )
            ThinDivider()
            NavRow(
                label = stringResource(R.string.row_export),
                value = stringResource(R.string.val_csv),
                onClick = onExport,
            )
            ThinDivider()
            NavRow(
                label = stringResource(R.string.row_about),
                value = stringResource(R.string.val_version, versionName),
                onClick = onOpenAbout,
            )
        }

        Text(
            text = stringResource(R.string.footer_note),
            style = AppTypo.caption,
            color = AppColors.TextFooter,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

/** 开关行：标题 + 说明 + 右侧 46×28 开关 */
@Composable
private fun SwitchRow(
    title: String,
    desc: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = D.RowPadVSwitch),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(D.RowGapWide),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(text = title, style = AppTypo.rowLabelMedium, color = AppColors.TextPrimary)
            Text(text = desc, style = AppTypo.caption, color = AppColors.TextTertiary)
        }
        AppSwitch(checked = checked, onCheckedChange = onChange)
    }
}

/** 导航行：标签 + 值 + chevron */
@Composable
private fun NavRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = D.RowPadVNav),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(D.RowGap),
    ) {
        Text(
            text = label,
            style = AppTypo.rowLabel,
            color = AppColors.TextSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = AppTypo.monoValue, color = AppColors.TextPrimary)
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = AppColors.Chevron,
            modifier = Modifier.size(D.Ic16),
        )
    }
}
