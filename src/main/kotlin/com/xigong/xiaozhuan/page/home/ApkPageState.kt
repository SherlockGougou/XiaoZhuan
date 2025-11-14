package com.xigong.xiaozhuan.page.home

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AtomicReference
import com.xigong.xiaozhuan.AppPath
import com.xigong.xiaozhuan.channel.ChannelRegistry
import com.xigong.xiaozhuan.channel.ChannelTask
import com.xigong.xiaozhuan.channel.MarketState
import com.xigong.xiaozhuan.channel.ReviewState
import com.xigong.xiaozhuan.channel.TaskLauncher
import com.xigong.xiaozhuan.config.ApkConfig
import com.xigong.xiaozhuan.config.ApkConfigDao
import com.xigong.xiaozhuan.log.AppLogger
import com.xigong.xiaozhuan.notify.FeishuWebhook
import com.xigong.xiaozhuan.page.upload.UploadParam
import com.xigong.xiaozhuan.util.ApkInfo
import com.xigong.xiaozhuan.util.FileSelector
import com.xigong.xiaozhuan.util.FileUtil
import com.xigong.xiaozhuan.util.getApkInfo
import com.xigong.xiaozhuan.widget.Toast
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.io.File
import kotlin.math.max

class ApkPageState(val apkConfig: ApkConfig) {

    private val scope = MainScope()

    private val apkDirState = mutableStateOf<File?>(null)

    private val apkInfoState = mutableStateOf<ApkInfo?>(null)

    val updateDesc = mutableStateOf(apkConfig.extension.updateDesc ?: "")

    val ignoreVersionCheck = mutableStateOf(apkConfig.extension.ignoreVersion)

    val ignoreStatusCheck = mutableStateOf(apkConfig.extension.ignoreStatus)

    // 自动刷新设置
    val autoRefreshEnabled = mutableStateOf(apkConfig.extension.autoRefreshEnabled)
    val autoRefreshMinutes =
        mutableStateOf(normalizeInterval(apkConfig.extension.autoRefreshMinutes))

    private var autoRefreshJob: Job? = null

    val channels: List<ChannelTask> =
        ChannelRegistry.channels.filter { apkConfig.channelEnable(it.channelName) }

    /**
     * 选中的Channel
     */
    val selectedChannels = mutableStateListOf<String>()

    /**
     * 应用市场信息加载状态
     */
    var loadingMarkState by mutableStateOf(false)

    val taskLaunchers: List<TaskLauncher> = channels.map(::TaskLauncher)


    var lastUpdateMarketStateTime = 0L


    init {
        AppLogger.info(LOG_TAG, "init")
        loadMarketState()
        // 根据配置启动自动刷新
        startOrStopAutoRefresh()
    }


    fun getApkDirState(): State<File?> = apkDirState

    fun getApkInfoState(): State<ApkInfo?> = apkInfoState

    private suspend fun parseApkFile(dir: File): Boolean {
        return try {
            val apkFile = if (dir.isDirectory) AppPath.listApk(dir).first() else dir
            taskLaunchers.forEach {
                it.setChannelParam(apkConfig.channels)
                it.selectFile(dir)
            }
            apkInfoState.value = getApkInfo(apkFile)
            apkDirState.value = dir
            updateSelectChannel()
            true
        } catch (e: Exception) {
            AppLogger.error(LOG_TAG, "解析选择Apk失败", e)
            e.printStackTrace()
            false
        }
    }


    /**
     * 获取应用市场状态
     */
    fun loadMarketState() {
        AppLogger.info(LOG_TAG, "更新应用市场审核状态")
        lastUpdateMarketStateTime = System.currentTimeMillis()
        val apkConfig = requireNotNull(apkConfig)
        scope.launch {
            loadingMarkState = true
            // 刷新前记录所有渠道当前审核状态
            val oldReviewStates: Map<String, ReviewState?> = taskLaunchers.associate { launcher ->
                val state = (launcher.getMarketState().value as? MarketState.Success)?.info?.reviewState
                launcher.name to state
            }
            supervisorScope {
                taskLaunchers.forEach {
                    it.setChannelParam(apkConfig.channels)
                    launch { it.loadMarketState(apkConfig.applicationId) }
                }
            }
            loadingMarkState = false
            updateSelectChannel()
            // 刷新后检查所有渠道审核状态变化
            taskLaunchers.forEach { launcher ->
                val newState = (launcher.getMarketState().value as? MarketState.Success)?.info?.reviewState
                val oldState = oldReviewStates[launcher.name]
                if (newState != null && oldState != newState) {
                    // 发送飞书通知
                    val text = if (oldState == null) {
                        "${launcher.name}应用市场首次获取审核结果：${newState.desc}"
                    } else {
                        "${launcher.name}应用市场审核结果从${oldState.desc}变更为：${newState.desc}"
                    }
                    scope.launch { FeishuWebhook.sendText(FEISHU_WEBHOOK_URL, text) }
                }
            }
        }
    }

    /**
     * 自动刷新触发，带节流控制
     */
    private fun tryAutoRefresh() {
        val now = System.currentTimeMillis()
        val diff = now - lastUpdateMarketStateTime
        if (!loadingMarkState && diff >= THROTTLE_MILLIS) {
            loadMarketState()
        } else {
            AppLogger.info(LOG_TAG, "跳过自动刷新：loading=$loadingMarkState, 距上次=${diff}ms")
        }
    }

    private fun startOrStopAutoRefresh() {
        autoRefreshJob?.cancel()
        if (autoRefreshEnabled.value) {
            val interval = autoRefreshMinutes.value
            autoRefreshJob = scope.launch {
                // 首次尝试：尊重节流
                tryAutoRefresh()
                while (isActive && autoRefreshEnabled.value) {
                    val delayMillis = max(1, interval * 60_000)
                    delay(delayMillis.toLong())
                    tryAutoRefresh()
                }
            }
            AppLogger.info(LOG_TAG, "已启动自动刷新，间隔=${interval}分钟")
        } else {
            AppLogger.info(LOG_TAG, "已关闭自动刷新")
        }
    }

    fun updateAutoRefreshEnabled(enabled: Boolean) {
        autoRefreshEnabled.value = enabled
        persistExtension(apkConfig.extension.copy(autoRefreshEnabled = enabled))
        startOrStopAutoRefresh()
        Toast.show(if (enabled) "已开启自动刷新" else "已关闭自动刷新")
    }

    fun updateAutoRefreshMinutes(minutes: Int) {
        val m = normalizeInterval(minutes)
        autoRefreshMinutes.value = m
        persistExtension(apkConfig.extension.copy(autoRefreshMinutes = m))
        // 变更间隔需要重启任务
        startOrStopAutoRefresh()
        Toast.show("自动刷新间隔：${m}分钟")
    }

    private fun normalizeInterval(m: Int): Int {
        // 仅允许 2/5/10/30
        return when (m) {
            2, 5, 10, 30 -> m
            else -> 5
        }
    }

    private fun persistExtension(newExt: ApkConfig.Extension) {
        val newConfig = apkConfig.copy(extension = newExt)
        scope.launch {
            val configDao = ApkConfigDao()
            try {
                configDao.saveConfig(newConfig)
            } catch (e: Exception) {
                AppLogger.error(LOG_TAG, "更新Apk配置失败", e)
            }
        }
    }

    private fun updateSelectChannel() {
        if (selectedChannels.isEmpty()) {
            selectAll(true)
        } else {
            // 删除失效的
            selectedChannels
                .filterNot { checkChannelEnableSubmit(it) }
                .forEach {
                    selectChannel(it, false)
                }
        }
    }


    private fun updateApkConfig() {
        val updateDesc = updateDesc.value.trim()
        val apkDir = apkDirState.value ?: return
        val newExtension = apkConfig.extension.copy(
            apkDir = apkDir.absolutePath,
            updateDesc = updateDesc,
            autoRefreshEnabled = autoRefreshEnabled.value,
            autoRefreshMinutes = autoRefreshMinutes.value
        )
        scope.launch {
            val configDao = ApkConfigDao()
            try {
                configDao.saveConfig(apkConfig.copy(extension = newExtension))
            } catch (e: Exception) {
                AppLogger.error(LOG_TAG, "更新Apk配置失败", e)
            }
        }
    }

    /**
     * 获取上一次选择的Apk文件或目录
     */
    private fun getLastApkDir(): File? {
        val apkFile = apkConfig.extension.apkDir ?: return null
        return File(apkFile).takeIf { it.exists() }
    }


    /**
     * 全部渠道已选择
     */
    fun allChannelSelected(): Boolean {
        return selectedChannels.containsAll(channels.map { it.channelName })
    }

    /**
     * 选择全部渠道
     */
    fun selectAll(selectedAll: Boolean) {
        if (selectedAll) {
            selectedChannels.clear()
            selectedChannels.addAll(getEnableSubmitChannel())
        } else {
            selectedChannels.clear()
        }
    }

    /**
     * 获取可以上传的渠道
     */
    private fun getEnableSubmitChannel(): List<String> {
        return taskLaunchers.filter { checkChannelEnableSubmit(it.name) }.map { it.name }
    }

    /**
     * 是否忽略版本号
     */
    fun isIgnoreVersion(): Boolean {
        return ignoreVersionCheck.value
    }

    /**
     * 是否忽略状态检查
     */
    fun isIgnoreStatus(): Boolean {
        return ignoreStatusCheck.value
    }

    /**
     * 设置忽略版本号
     */
    fun updateIgnoreVersion(checked: Boolean) {
        ignoreVersionCheck.value = checked
        val newExtension = apkConfig.extension.copy(ignoreVersion = checked)
        scope.launch {
            val configDao = ApkConfigDao()
            try {
                configDao.saveConfig(apkConfig.copy(extension = newExtension))
            } catch (e: Exception) {
                AppLogger.error(LOG_TAG, "更新Apk配置失败", e)
            }
        }
    }

    /**
     * 设置忽略状态
     */
    fun updateIgnoreStatus(checked: Boolean) {
        ignoreStatusCheck.value = checked
        val newExtension = apkConfig.extension.copy(ignoreStatus = checked)
        scope.launch {
            val configDao = ApkConfigDao()
            try {
                configDao.saveConfig(apkConfig.copy(extension = newExtension))
            } catch (e: Exception) {
                AppLogger.error(LOG_TAG, "更新Apk配置失败", e)
            }
        }
    }

    /**
     * 检查当前渠道是否支持提交
     */
    private fun checkChannelEnableSubmit(
        channelName: String,
        message: AtomicReference<String>? = null
    ): Boolean {
        val task = taskLaunchers.firstOrNull { it.name == channelName } ?: return false
        val marketInfo = (task.getMarketState().value as? MarketState.Success)?.info
        val apkInfo = getApkInfoState().value
        if (isIgnoreStatus()) {
            return true
        }
        if (marketInfo != null && !marketInfo.enableSubmit) {
            message?.set("应用市场审核中，或状态异常，无法上传新版本")
            return false
        }
        if (!isIgnoreVersion()) {
            if (apkInfo != null && marketInfo != null && apkInfo.versionCode <= marketInfo.lastVersionCode) {
                message?.set("要提交的Apk版本号需大于线上最新版本号")
                return false
            }
        }
        return true
    }

    /**
     * 选择某个渠道
     */
    fun selectChannel(name: String, selected: Boolean) {
        if (selected) {
            val message = AtomicReference<String>()
            if (!checkChannelEnableSubmit(name, message)) {
                message.get()?.let { Toast.show(it) }
            } else {
                selectedChannels.remove(name)
                selectedChannels.add(name)
            }
        } else {
            selectedChannels.remove(name)
        }
    }

    fun startDispatch(): UploadParam? {
        val file = getApkDirState().value
        if (file == null) {
            Toast.show("请选择Apk文件")
            return null
        }
        val updateDesc = updateDesc.value.trim()
        if (updateDesc.isEmpty()) {
            Toast.show("请输入更新描述")
            return null
        }
        if (updateDesc.length > 300) {
            Toast.show("更新描述不可超过300字")
            return null

        }
        val channels = selectedChannels
        if (channels.isEmpty()) {
            Toast.show("请选择渠道")
            return null
        }
        updateApkConfig()
        return UploadParam(
            appId = apkConfig.applicationId,
            updateDesc = updateDesc,
            channels = channels,
            apkFile = file.absolutePath
        )
    }

    fun getFileSize(): String {
        val apkInfo = getApkInfoState().value ?: return ""
        val file = File(apkInfo.path)
        return FileUtil.getFileSize(file)
    }


    fun selectedApkDir() {
        scope.launch {
            val dir = FileSelector.selectedDir(getLastApkDir())
            if (dir != null && !parseApkFile(dir)) {
                Toast.show("无效目录,未包含有效的Apk文件")
            }
        }
    }


    fun selectApkFile() {
        scope.launch {
            val file = FileSelector.selectedFile(getLastApkDir(), "*.apk", listOf("apk"))
            if (file != null && !parseApkFile(file)) {
                Toast.show("无效的Apk文件")
            }
        }
    }


    fun clear() {
        if (scope.isActive) scope.cancel()
        AppLogger.info(LOG_TAG, "clear ${apkConfig.applicationId}")
    }


    companion object {
        private const val LOG_TAG = "应用界面"
        private const val THROTTLE_MILLIS = 30_000L
        private const val FEISHU_WEBHOOK_URL = "https://open.feishu.cn/open-apis/bot/v2/hook/0319beba-4423-4c25-89f9-0ca46fe79f65"
    }


}
