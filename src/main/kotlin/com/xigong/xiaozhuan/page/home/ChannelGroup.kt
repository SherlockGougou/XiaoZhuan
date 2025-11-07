package com.xigong.xiaozhuan.page.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xigong.xiaozhuan.channel.MarketState
import com.xigong.xiaozhuan.page.upload.UploadParam
import com.xigong.xiaozhuan.style.AppColors
import com.xigong.xiaozhuan.style.AppShapes
import com.xigong.xiaozhuan.widget.ConfirmDialog
import com.xigong.xiaozhuan.widget.ErrorPopup
import com.xigong.xiaozhuan.widget.Toast
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * 渠道页面
 */
@Composable
fun ChannelGroup(viewModel: ApkPageState, startUpload: (UploadParam) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize()
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // 定时刷新开关
                Text(
                    "定时刷新",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                val enabled = viewModel.autoRefreshEnabled.value
                Switch(
                    checked = enabled,
                    onCheckedChange = { checked ->
                        viewModel.updateAutoRefreshEnabled(checked)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AppColors.primary,
                        checkedTrackColor = AppColors.primary.copy(alpha = 0.3f),
                        uncheckedThumbColor = Color.LightGray,
                        uncheckedTrackColor = Color.LightGray
                    ),
                    modifier = Modifier.requiredWidthIn(60.dp).align(Alignment.CenterVertically),
                    enabled = true
                )
                Spacer(Modifier.width(6.dp).align(Alignment.CenterVertically))
                // 间隔下拉
                var showIntervalMenu by remember { mutableStateOf(false) }
                val minutes = viewModel.autoRefreshMinutes.value
                val intervalText = "${minutes}分钟"
                Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                    Text(
                        intervalText,
                        color = if (enabled) AppColors.primary else AppColors.fontGray,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clip(AppShapes.roundButton)
                            .background(if (enabled) AppColors.auxiliary else Color(0xFFF0F0F0))
                            .clickable(enabled = enabled) { showIntervalMenu = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                    if (showIntervalMenu) {
                        DropdownMenu(true, onDismissRequest = { showIntervalMenu = false }) {
                            @Composable
                            fun itemOf(m: Int) {
                                Text(
                                    text = "${m}分钟",
                                    color = AppColors.fontBlack,
                                    fontSize = 14.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.updateAutoRefreshMinutes(m)
                                            showIntervalMenu = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                            itemOf(2)
                            itemOf(5)
                            itemOf(10)
                            itemOf(30)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Box(modifier = Modifier.size(40.dp).align(Alignment.CenterVertically)) {
                    if (viewModel.loadingMarkState) {
                        CircularProgressIndicator(
                            color = AppColors.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    } else {
                        Image(
                            painterResource("refresh.png"),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(AppColors.primary),
                            modifier = Modifier.size(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    tryReloadMarketState(viewModel)
                                }
                                .padding(2.dp)
                        )
                    }
                }
            }
            val scrollState = rememberScrollState()
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.verticalScroll(scrollState)
                        .weight(1f)
                ) {
                    Spacer(modifier = Modifier.height(5.dp))
                    viewModel.channels.withIndex().forEach { (_, chan) ->
                        val selected = viewModel.selectedChannels.contains(chan.channelName)
                        val name = chan.channelName
                        val taskLauncher = viewModel.taskLaunchers.first { it.name == name }
                        val apkFileState = taskLauncher.getApkFileState()
                        val desc = apkFileState.value?.name
                        val marketState = taskLauncher.getMarketState().value
                        ChannelView(selected, name, desc, marketState) { checked ->
                            viewModel.selectChannel(name, checked)
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                    }
                }
                VerticalScrollbar(
                    rememberScrollbarAdapter(scrollState),
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth()
                .padding(vertical = 14.dp)
        ) {
            val uploadState = remember { mutableStateOf<UploadParam?>(null) }
            val uploadParam = uploadState.value
            if (uploadParam != null) {
                showConfirmDialog(
                    viewModel,
                    onConfirm = {
                        startUpload(uploadParam)
                        uploadState.value = null
                    }, onDismiss = {
                        uploadState.value = null
                    }
                )
            }
            val allSelected = viewModel.allChannelSelected()
            Row(
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                    .padding(end = 12.dp)
            ) {
                Checkbox(
                    allSelected,
                    onCheckedChange = { all ->
                        viewModel.selectAll(all)
                    },
                    colors = CheckboxDefaults.colors(checkedColor = AppColors.primary)
                )
                Text("全选")
                Spacer(Modifier.width(20.dp).align(Alignment.CenterVertically))
                Text(
                    "忽略状态检查",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Switch(
                    checked = viewModel.isIgnoreStatus(),
                    onCheckedChange = { checked ->
                        viewModel.updateIgnoreStatus(checked)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AppColors.primary,
                        checkedTrackColor = AppColors.primary.copy(alpha = 0.3f),
                        uncheckedThumbColor = Color.LightGray,
                        uncheckedTrackColor = Color.LightGray
                    ),
                    modifier = Modifier.requiredWidthIn(60.dp).align(Alignment.CenterVertically),
                    enabled = true
                )
                Spacer(Modifier.weight(1f))
                Button(
                    colors = ButtonDefaults.buttonColors(AppColors.primary),
                    onClick = {
                        uploadState.value = viewModel.startDispatch()
                    }
                ) {

                    Text(
                        "发布更新",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    )
                }
            }
        }
    }
}

private fun tryReloadMarketState(viewModel: ApkPageState) {
    // 控制刷新频率，防止应用市场接口限流
    val diff =
        (System.currentTimeMillis() - viewModel.lastUpdateMarketStateTime).milliseconds
    val threshold = 30.seconds
    val leftSeconds = (threshold - diff).inWholeSeconds
    if (leftSeconds > 0) {
        Toast.show("刷新太频繁了，请${leftSeconds}秒后重试")
    } else {
        viewModel.loadMarketState()
    }
}


@Composable
private fun showConfirmDialog(
    viewModel: ApkPageState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val apkInfo = viewModel.getApkInfoState().value ?: return
    val selectedChannels = viewModel.selectedChannels
    val message = buildString {
        append("包名:  ${apkInfo.applicationId}")
        append("\n")
        append("版本:  ${apkInfo.versionName}(${apkInfo.versionCode})")
        append("\n")
        append("渠道:  ${selectedChannels.joinToString(",")}")
    }


    ConfirmDialog(
        message, "确定上传",
        onConfirm = {
            onConfirm()
        }, onDismiss = {
            onDismiss()
        }
    )
}


@Composable
private fun ChannelView(
    selected: Boolean,
    name: String,
    desc: String?,
    marketState: MarketState?,
    onCheckChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(AppColors.cardBackground)
                .clickable {
                    onCheckChange(!selected)
                }
                .padding(16.dp)
        ) {
            Checkbox(
                selected,
                onCheckedChange = onCheckChange,
                colors = CheckboxDefaults.colors(checkedColor = AppColors.primary)
            )
            Text(
                name,
                fontSize = 14.sp,
                color = AppColors.fontBlack,
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                desc ?: "",
                fontSize = 12.sp,
                color = AppColors.fontGray
            )
            Spacer(modifier = Modifier.weight(1.0f))
            val state = when (marketState) {
                null -> ""
                is MarketState.Loading -> "加载中"
                is MarketState.Success -> {
                    val info = marketState.info
                    "v${info.lastVersionName} ${info.reviewState.desc}"
                }

                is MarketState.Error -> {
                    "获取状态失败"
                }
            }
            Text(
                state,
                fontSize = 12.sp,
                color = AppColors.fontBlack
            )
            if (marketState is MarketState.Error) {
                Row {
                    var showError by remember { mutableStateOf(false) }
                    if (showError) {
                        ErrorPopup(marketState.exception) {
                            showError = false
                        }
                    }
                    Spacer(Modifier.width(5.dp))
                    Image(
                        painterResource("error_info.png"),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color.Red),
                        modifier = Modifier.size(22.dp)
                            .clickable {
                                showError = true
                            }
                    )
                }
            }
        }
    }
}
