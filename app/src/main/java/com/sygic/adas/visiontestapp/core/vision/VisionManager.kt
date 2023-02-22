package com.sygic.adas.visiontestapp.core.vision

import android.content.Context
import android.util.Log
import com.sygic.adas.vision.Vision
import com.sygic.adas.vision.ar_object.ArObject
import com.sygic.adas.vision.licensing.SygicLicense
import com.sygic.adas.vision.logic.SpeedLimitInfo
import com.sygic.adas.vision.logic.TailgatingInfo
import com.sygic.adas.vision.logic.VisionLogic
import com.sygic.adas.vision.objects.VisionTextBlock
import com.sygic.adas.vision.road.Road
import com.sygic.adas.visiontestapp.BuildConfig
import com.sygic.adas.visiontestapp.core.Constants
import com.sygic.adas.visiontestapp.core.camera.CameraParamsProvider
import com.sygic.adas.visiontestapp.core.settings.AppSettings
import com.sygic.adas.visiontestapp.core.vision.model.VisionObjects
import com.sygic.adas.visiontestapp.core.vision.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VisionManager(
    private val context: Context,
    private val scope: CoroutineScope
) {

    private val settings = AppSettings.get(context)

    private val jobs = mutableListOf<Job>()

    private val _visionInstance: MutableStateFlow<Vision?> = MutableStateFlow(null)
    val visionInstance: StateFlow<Vision?> = _visionInstance

    private val visionLogicInstance: MutableStateFlow<VisionLogic?> = MutableStateFlow(null)

    private val active = MutableStateFlow(false).apply {
        onEach { isActive ->
            if(isActive)
                initializeInternal()
            else
                deinitializeInternal()
        }.launchIn(scope)
    }

    private val _initState: MutableStateFlow<Vision.InitState> = MutableStateFlow(Vision.InitState.Uninitialized)
    val initState: StateFlow<Vision.InitState> = _initState

    private var latestSpeed = 0.0f

    fun initIfNeeded() {
        active.value = true
    }

    fun deinitialize() {
        active.value = false
    }

    private suspend fun initializeInternal() {
        if(!VisionLogic.isInitialized) {
            VisionLogic.initialize(context)
        }
        visionLogicInstance.value = VisionLogic.getInstance()

        if(!Vision.isInitialized) {
            val cameraParams = CameraParamsProvider(context).getParams()
            val visionConfig = settings.visionConfiguration.first()
            val visionInit = Vision.Initializer(
                context = context,
                initListener = visionInitListener,
                config = visionConfig,
                clientId = Constants.CLIENT_ID,
                license = SygicLicense.KeyString(BuildConfig.SYGIC_LICENSE),
                cameraParams = cameraParams
            )
            Vision.initialize(visionInit)
        }
        else {
            _visionInstance.value = Vision.getInstance()
            onInitialized()
        }

    }

    private fun deinitializeInternal() {
        onDeinitialized()

        if(Vision.isInitialized)
            Vision.deinitialize()

        if(VisionLogic.isInitialized)
            VisionLogic.deinitialize()

        _visionInstance.value = null
        visionLogicInstance.value = null
    }

    fun setSpeed(speedMps: Float) {
        latestSpeed = speedMps
    }

    val objects: SharedFlow<VisionObjects> = visionFlow { vision ->
        vision.objectsResultFlow()
    }.shareIn(scope, SharingStarted.WhileSubscribed(), 0)

    val roads: SharedFlow<Road?> = visionFlow { vision ->
        vision.roadFlow()
    }.shareIn(scope, SharingStarted.WhileSubscribed(), 0)

    val speedLimit: SharedFlow<SpeedLimitInfo> = visionLogicFlow { visionLogic ->
        visionLogic.speedLimitFlow()
    }.shareIn(scope, SharingStarted.WhileSubscribed(), 1)

    val tailgating: StateFlow<TailgatingInfo?> = visionLogicFlow { visionLogic ->
        visionLogic.tailgatingFlow()
    }.stateIn(scope, SharingStarted.WhileSubscribed(), null)

    val licensePlates: SharedFlow<Array<VisionTextBlock>> = visionFlow { vision ->
        vision.licensePlatesFlow()
    }.shareIn(scope, SharingStarted.WhileSubscribed(), 0)

    val arObject: StateFlow<ArObject?> = visionFlow {
        it.arObjectsFlow()
    }.stateIn(scope, SharingStarted.WhileSubscribed(), null)

    private val visionInitListener = object: Vision.InitListener {
        override fun onInitStateChanged(state: Vision.InitState) {
            if(state == Vision.InitState.Initialized) {
                _visionInstance.value = Vision.getInstance()
                onInitialized()
            } else {
                _visionInstance.value = null
            }
            _initState.value = state
        }
    }

    private fun onInitialized() {
        val visionToVisionLogicJob = scope.launch {
            objects.collect { objects ->
                visionLogicInstance.value?.addVisionObjects(objects.objects, latestSpeed)
            }
        }
        jobs.add(visionToVisionLogicJob)

        val visionConfigJob = scope.launch {
            settings.visionConfiguration.collect { config ->
                visionInstance.value?.configuration = config
            }
        }
        jobs.add(visionConfigJob)

        val visionLogicConfigJob = scope.launch {
            settings.visionLogicConfiguration.collect { config ->
                visionLogicInstance.value?.configuration = config
            }
        }
        jobs.add(visionLogicConfigJob)
    }

    private fun onDeinitialized() {
        jobs.forEach { it.cancel() }
        jobs.clear()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> visionFlow(flowCreator: (vision: Vision) -> Flow<T>): Flow<T> {
        return visionInstance.flatMapLatest { vision ->
            if(vision != null) flowCreator(vision) else emptyFlow()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> visionLogicFlow(flowCreator: (visionLogic: VisionLogic) -> Flow<T>): Flow<T> {
        return visionLogicInstance.flatMapLatest { visionLogic ->
            if(visionLogic != null) flowCreator(visionLogic) else emptyFlow()
        }
    }
}