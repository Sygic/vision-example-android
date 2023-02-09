package com.sygic.adas.visiontestapp.core.camera

import android.annotation.SuppressLint
import android.app.Activity
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.Surface
import androidx.annotation.MainThread
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.sygic.adas.visiontestapp.core.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

typealias CameraImageCallback = (image: Image, rotation: Int) -> Unit

@MainThread
class CameraManager(
    private val activity: Activity,
    private val cameraPreview: PreviewView,
    private val viewLifecycleOwner: LifecycleOwner,
    private val imageCallback: CameraImageCallback
) {

    private var cameraProvider: ProcessCameraProvider? = null

    private var videoCapture: VideoCapture<Recorder>? = null
    private val cameraExecutor: ExecutorService by lazy {
        Executors.newSingleThreadExecutor()
    }
    private var recording: Recording? = null

    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private val _aspectRatio: MutableStateFlow<Float> = MutableStateFlow(-1.0f)

    private var videoCaptureStarted = false

    val aspectRatio = _aspectRatio.asStateFlow()

    private val _shouldAnalyzePreview = MutableStateFlow(false)
    val shouldAnalyzePreview = _shouldAnalyzePreview.asStateFlow()

    private val _recordingActive = MutableStateFlow(false)
    val recordingActive = _recordingActive.asStateFlow()

    var videoDurationMin: Int? = null

    private val recordingsFolder = File(activity.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "recordings")

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    private val imageAnalyser = ImageAnalysis.Analyzer { imageProxy ->
        imageProxy.image?.let { image ->
            val ratio = image.width.toFloat() / image.height.toFloat()
            _aspectRatio.value =
                if(imageProxy.imageInfo.rotationDegrees == 90 || imageProxy.imageInfo.rotationDegrees == 270)
                    1f/ratio
                else
                    ratio

            imageCallback(image, imageProxy.imageInfo.rotationDegrees)
        }
        imageProxy.close()
    }

    suspend fun prepare() = suspendCancellableCoroutine { continuation ->
        cleanRecordings()
        if(cameraProvider == null) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
            cameraProviderFuture.addListener({
                cameraProvider = cameraProviderFuture.get()
                continuation.resume(Unit)
            }, ContextCompat.getMainExecutor(activity))
        }
        else
            continuation.resume(Unit)
    }

    fun start() {
        restartUseCases()
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun restartUseCases() {
        requireNotNull(cameraProvider) {
            "CameraProvider not set up. Call prepare first."
        }
        val cameraParamProvider = CameraParamsProvider(activity)

        // Preview
        val previewBuilder = Preview.Builder()
            .apply {
            Camera2Interop.Extender(this)
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    cameraParamProvider.frameRate
                )
        }
        val preview = previewBuilder.build().also {
            it.setSurfaceProvider(cameraPreview.surfaceProvider)
        }

        // Image analysis
        val imageAnalysisBuilder = ImageAnalysis.Builder().apply {
            Camera2Interop.Extender(this)
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    cameraParamProvider.frameRate
                )
        }

        val displayRotation =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                activity.display?.rotation ?: Surface.ROTATION_0
            else
                activity.windowManager.defaultDisplay.rotation

        val imageAnalyzer = imageAnalysisBuilder.build().also {
            it.targetRotation = displayRotation
            it.setAnalyzer(cameraExecutor, imageAnalyser)
        }

        // Video capture
        val cameraInfo = cameraProvider?.availableCameraInfos?.filter {
            Camera2CameraInfo
                .from(it)
                .getCameraCharacteristic(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK
        }

        videoCapture = if(videoCaptureStarted && cameraInfo != null) {
            val supportedQualities = QualitySelector.getSupportedQualities(cameraInfo[0])
            val filteredQualities = arrayListOf(Quality.SD, Quality.HD)
                .filter { supportedQualities.contains(it) }

            val qualitySelector = QualitySelector.fromOrderedList(
                filteredQualities,
                FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
            )

            val recorder = Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .setExecutor(cameraExecutor)
                .build()

            VideoCapture.withOutput(recorder)
        }
        else null

        _shouldAnalyzePreview.value = false
        _recordingActive.value = false
        try {
            cameraProvider?.let {
                // Unbind use cases before rebinding
                it.unbindAll()

                // Bind use cases to camera
                if(videoCapture != null) {
                    it.bindToLifecycle(
                        viewLifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer,
                        videoCapture
                    )
                    _recordingActive.value = true
                    startNewRecording()
                }
                else {
                    it.bindToLifecycle(
                        viewLifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                }
            }
        } catch (e: Exception) {
            if(videoCapture != null) {
                Log.d(Constants.LOG_TAG, "Failed to bind preview + analysis + video capture use cases. Use fallback mechanism.")
                try {
                    // we can't make working preview + analysis + video capture, so we start just
                    // preview + video capture and for analysis we will use images from preview
                    cameraProvider?.bindToLifecycle(
                        viewLifecycleOwner,
                        cameraSelector,
                        preview,
                        videoCapture
                    )
                    _recordingActive.value = true
                    _shouldAnalyzePreview.value = true
                    startNewRecording()
                }
                catch (e: Exception) {
                    Log.e(Constants.LOG_TAG, "Failed to bind preview + video capture use cases.", e)
                }
            } else {
                Log.e(Constants.LOG_TAG, "Failed to bind preview + analysis use cases.", e)
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun startVideoRecording() {
        if(!videoCaptureStarted) {
            videoCaptureStarted = true
            restartUseCases()
        }
    }

    fun stopVideoRecording() {
        if(videoCaptureStarted) {
            videoCaptureStarted = false
            recording?.stop()
            recording = null
            restartUseCases()
        }
    }

    fun stopAll() {
        videoCaptureStarted = false
        recording?.stop()
        recording = null
        cameraProvider?.unbindAll()
    }

    fun hasRecordings(): Boolean = recordingsFolder.listFiles()?.isNotEmpty() == true

    fun getRecordings(): List<File> {
        cleanRecordings()
        return recordingsFolder.listFiles()?.toList().orEmpty()
    }

    private fun startNewRecording() {
        if(!videoCaptureStarted)
            return

        val capture = videoCapture ?: return
        val duration = videoDurationMin ?: return

        cleanRecordings()
        recordingsFolder.mkdirs()
        val file = File(recordingsFolder, "${System.currentTimeMillis()}.mp4")
        val outputOptions = FileOutputOptions.Builder(file)
            .setDurationLimitMillis(TimeUnit.MINUTES.toMillis(duration.toLong()))
            .build()

        recording = capture.output
            .prepareRecording(activity, outputOptions)
            .start(ContextCompat.getMainExecutor(activity)) { event ->
                if(event is VideoRecordEvent.Finalize) {
                    startNewRecording()
                }
            }
    }

    fun cleanRecordings(forceDeleteAll: Boolean = false) {
        val videoDuration = videoDurationMin?.toLong() ?: return
        val deleteBeforeTime = if(videoDuration > 0 && !forceDeleteAll)
            System.currentTimeMillis() - 2 * TimeUnit.MINUTES.toMillis(videoDuration)
        else
            Long.MAX_VALUE

        recordingsFolder.listFiles()
            ?.filter { it.lastModified() < deleteBeforeTime}
            ?.forEach {
                it.delete()
            }
    }
}