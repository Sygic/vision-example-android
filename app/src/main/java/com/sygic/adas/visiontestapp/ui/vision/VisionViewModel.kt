package com.sygic.adas.visiontestapp.ui.vision

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import android.media.Image
import android.util.SizeF
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.sygic.adas.vision.Diagnostics
import com.sygic.adas.vision.objects.Sign
import com.sygic.adas.vision.route.Route
import com.sygic.adas.visiontestapp.core.Constants.MIN_SAME_SIGN_TIME_DIFF_MILLIS
import com.sygic.adas.visiontestapp.core.accurateLocationFlow
import com.sygic.adas.visiontestapp.core.mpsToKmh
import com.sygic.adas.visiontestapp.core.settings.AppSettings
import com.sygic.adas.visiontestapp.core.vision.VisionManager
import com.sygic.adas.visiontestapp.core.vision.util.RouteInstructionProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*


@SuppressLint("UnsafeExperimentalUsageError")
class VisionViewModel(application: Application) : AndroidViewModel(application) {

    private val visionManager = VisionManager.getInstance(application)
    private val vision = visionManager.visionInstance

    private val appSettings: AppSettings = AppSettings.get(application)

    val visionInitState = visionManager.initState

    val roads = visionManager.roads
    val visionObjects = visionManager.objects.map { it.objects }
    val tailgating = visionManager.tailgating
    val licensePlates = visionManager.licensePlates
    val speedLimitKmh = visionManager.speedLimit.map { it.speedLimit }
    val arObject = visionManager.arObject
    val dashcamEnabled = appSettings.dashcamActive
    val dashcamVideoDuration = appSettings.dashcamVideoDuration
    val isReadyForProcessing = visionManager.isReadyForProcessing

    private val _fps: MutableStateFlow<Float> = MutableStateFlow(0.0f)
    val fps = _fps.asStateFlow()

    val objFps = visionManager.objects
        .onEach { _fps.value = Diagnostics.fps }
        .map { it.fps }

    private var lastSignType: Sign.Type = Sign.Type.General
    private var lastSignTime: Long = 0L

    val detectedSigns = visionObjects.map { signs ->
        val dateNow = Date().time
        signs
            .filterIsInstance(Sign::class.java)
            .filter { sign -> sign.passed }
            .filterNot { sign -> sign.signType.isDuplicate() }
            .onEach { sign ->
                // do not add 2 same recently detected signs
                lastSignType = sign.signType
                lastSignTime = dateNow
            }
    }

    private val fusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application.applicationContext)

    private val startLocationUpdatesTrigger = Channel<Unit>()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val locations = startLocationUpdatesTrigger.receiveAsFlow()
        .flatMapLatest {
            fusedLocationProviderClient.accurateLocationFlow()
        }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)

    val speedKmh = locations
        .map { it.speed }
        .onEach { visionManager.setSpeed(it) }
        .map { it.mpsToKmh().toInt() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    fun onPermissionsGranted() {
        viewModelScope.launch {
            startLocationUpdatesTrigger.send(Unit)
        }
        visionManager.initIfNeeded()
        startUpdatingVisionRoute()
    }

    fun onImage(image: Image, rotation: Int) {
        vision.value?.process(image, rotation)
    }

    fun onBitmap(bitmap: Bitmap) {
        vision.value?.process(bitmap)
    }

    override fun onCleared() {
        visionManager.deinitialize()
        super.onCleared()
    }

    private val routeManeuverSize = SizeF(2f, 2f)

    private fun startUpdatingVisionRoute() {
        viewModelScope.launch {
            combine(RouteInstructionProvider.instructions, locations) { instruction, location ->
                Route(
                    instruction.id,
                    instruction.location,
                    routeManeuverSize,
                    instruction.maneuverType,
                    location
                )
            }.collect {
                vision.value?.process(it)
            }
        }
    }

    private fun Sign.Type.isDuplicate(): Boolean {
        return this == lastSignType
                && Date().time - lastSignTime < MIN_SAME_SIGN_TIME_DIFF_MILLIS
    }
}
