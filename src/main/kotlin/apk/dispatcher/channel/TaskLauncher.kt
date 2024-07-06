package apk.dispatcher.channel

import androidx.compose.runtime.*
import apk.dispatcher.config.ApkConfig
import apk.dispatcher.AppPath
import apk.dispatcher.log.AppLogger
import apk.dispatcher.log.action
import java.io.File

class TaskLauncher(
    private val task: ChannelTask
) {

    val name = task.channelName

    private val stateListener = SubmitStateAdapter(::updateState)

    private var channelParams: List<ApkConfig.Channel> = emptyList()

    private val apkFileState: MutableState<File?> = mutableStateOf(null)

    private val submitState: MutableState<SubmitState?> = mutableStateOf(null)


    private val marketState: MutableState<Result<MarketState>?> = mutableStateOf(null)

    fun setChannelParam(channelParams: List<ApkConfig.Channel>) {
        this.channelParams = channelParams

    }

    fun selectFile(apkDir: File) {
        val apkFile = if (apkDir.isDirectory) findApkFile(apkDir) else apkDir
        apkFileState.value = apkFile
    }

    fun prepare() {
        updateState(SubmitState.Waiting)
    }

    suspend fun startSubmit(updateDesc: String) {
        val apkFile = requireNotNull(apkFileState.value)
//        AppLogger.debug(task.channelName, "参数:${getParams()}")
        task.init(getParams())
        task.setSubmitStateListener(stateListener)
        task.startUpload(apkFile, updateDesc)
    }

    suspend fun loadMarketState(applicationId: String) {
//        AppLogger.debug(task.channelName, "参数:${getParams()}")
        task.init(getParams())
        marketState.value = runCatching {
            AppLogger.action(task.channelName, "获取应用市场状态:$applicationId") {
                task.getMarketState(applicationId)
            }
        }
    }


    fun getApkFileState(): State<File?> = apkFileState

    fun getSubmitState(): State<SubmitState?> = submitState

    fun getMarketState(): State<Result<MarketState>?> = marketState

    private fun getParams(): Map<ChannelTask.Param, String?> {
        val channelParams = channelParams.associateBy { it.name }
        val saveParams = channelParams[task.channelName]?.params
        val getParam = { p: ChannelTask.Param -> saveParams?.firstOrNull { it.name == p.name }?.value }
        return task.getParams().associateWith { p -> getParam(p) }
    }

    private fun findApkFile(apkDir: File): File {
        val apks = AppPath.listApk(apkDir)
        val fileId = channelParams.firstOrNull() { it.name == name }
            ?.getParam(ChannelTask.FILE_NAME_IDENTIFY)
            ?.value
            ?: task.fileNameIdentify
        val file = apks.firstOrNull { it.name.contains(fileId, true) }
        return checkNotNull(file) { "找不到文件名中包含:${fileId}的文件" }
    }

    private fun updateState(newState: SubmitState) {
        submitState.value = newState
    }

}

private class SubmitStateAdapter(private val updateState: (SubmitState) -> Unit) : ChannelTask.SubmitStateListener {
    override fun onStart() {
        updateState(SubmitState.Uploading(0))
    }

    override fun onProcessing(action: String) {
        updateState(SubmitState.Processing(action))
    }

    override fun onProgress(progress: Int) {
        updateState(SubmitState.Uploading(progress))
    }

    override fun onSuccess() {
        updateState(SubmitState.Success)
    }

    override fun onError(exception: Exception) {
        updateState(SubmitState.Error("上传失败，请重试"))
    }
}