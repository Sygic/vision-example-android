package com.sygic.adas.visiontestapp.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sygic.adas.vision.*
import com.sygic.adas.vision.logic.VehicleType
import com.sygic.adas.vision.logic.VisionLogicConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "vision_test_app_settings"
)

class AppSettingsImpl(context: Context): AppSettings(context) {

    private val dataStore: DataStore<Preferences> = context.dataStore
    private val scope = ProcessLifecycleOwner.get().lifecycleScope

    private val keySignsActive = booleanPreferencesKey(prefKeySignActive)
    private val keyVehicleType = intPreferencesKey(prefKeyVehicleType)
    private val keySignsRate = intPreferencesKey(prefKeySignRate)
    private val keySignsClassificatorThreshold = floatPreferencesKey(prefKeySignClassThreshold)
    private val keySignsDetectorThreshold = floatPreferencesKey(prefKeySignDetectorThreshold)
    private val keySignsDynamicThreshold = floatPreferencesKey(prefKeySignDynamicThreshold)
    private val keySignsAdditionalThreshold = floatPreferencesKey(prefKeySignAdditionalThreshold)
    private val keySignsIgnoreOnCars = booleanPreferencesKey(prefKeySignIgnoreOnCars)
    private val keySignsSpeedLimitsOnly = booleanPreferencesKey(prefKeySpeedLimitOnly)
    private val keyVehiclesActive = booleanPreferencesKey(prefKeyVehicleActive)
    private val keyVehiclesRate = intPreferencesKey(prefKeyVehicleRate)
    private val keyVehiclesDetectorThreshold = floatPreferencesKey(prefKeyVehicleThreshold)
    private val keyTailgatingTimeDistance = floatPreferencesKey(prefKeyTailgatingDistance)
    private val keyTailgatingDuration = floatPreferencesKey(prefKeyTailgatingDuration)
    private val keyFfTailgatingDuration = floatPreferencesKey(prefKeyFfTailgatingDuration)
    private val keyLanesActive = booleanPreferencesKey(prefKeyLaneActive)
    private val keyLanesPauseWhenNotMoving = booleanPreferencesKey(prefKeyLanePauseWhenNotMoving)
    private val keyLanesFastFocusMode = booleanPreferencesKey(prefKeyLaneFastFocusMode)
    private val keyLanesDynamicFocus = booleanPreferencesKey(prefKeyLaneDynamicFocus)
    private val keyLanesMinSamples = intPreferencesKey(prefKeyLaneMinSamples)
    private val keyLanesMaxSamples = intPreferencesKey(prefKeyLaneMaxSamples)
    private val keyRoadActive = booleanPreferencesKey(prefKeyRoadActive)
    private val keyRoadRate = intPreferencesKey(prefKeyRoadRate)
    private val keyRoadMode = intPreferencesKey(prefKeyRoadMode)
    private val keyTextActive = booleanPreferencesKey(prefKeyTextActive)
    private val keyTextRate = intPreferencesKey(prefKeyTextRate)
    private val keyTextShowOnCarsOnly = booleanPreferencesKey(prefKeyTextOnCarsOnly)
    private val keyArActive = booleanPreferencesKey(prefKeyArActive)
    private val keyArFeatureTracking = booleanPreferencesKey(prefKeyArFeatureTracking)
    private val keyArHeadingCorrection = booleanPreferencesKey(prefKeyArHeadingCorrection)
    private val keyDashcamActive = booleanPreferencesKey(prefKeyDashcamActive)
    private val keyDashcamVideoDuration = intPreferencesKey(prefKeyDashcamVideoDuration)

    private val defaultVisionConfig = VisionConfig()
    private val defaultVisionLogicConfig = VisionLogicConfig()

    private val _visionConfiguration = MutableStateFlow(defaultVisionConfig)
    override val visionConfiguration: Flow<VisionConfig> = _visionConfiguration
    
    private val _visionLogicConfiguration = MutableStateFlow(defaultVisionLogicConfig)
    override val visionLogicConfiguration: Flow<VisionLogicConfig> = _visionLogicConfiguration

    override val signsActive: Flow<Boolean> = dataStore.data.map {
        it[keySignsActive] ?: defaultVisionConfig.sign.active
    }

    override val vehicleType: Flow<VehicleType> = dataStore.data.map {
        it[keyVehicleType]?.toVehicleType() ?: defaultVisionLogicConfig.vehicleType
    }

    override val signsRate: Flow<VisionPerformance.Rate> = dataStore.data.map {
        it[keySignsRate]?.toRate() ?: defaultVisionConfig.sign.performance.rate
    }

    override val signsClassificatorThreshold: Flow<Float> = dataStore.data.map {
        it[keySignsClassificatorThreshold] ?: defaultVisionConfig.sign.classificatorThreshold
    }

    override val signsStandardThreshold: Flow<Float> = dataStore.data.map {
        it[keySignsDetectorThreshold] ?: defaultVisionConfig.sign.detectorThreshold
    }

    override val signsDynamicThreshold: Flow<Float> = dataStore.data.map {
        it[keySignsDynamicThreshold] ?: defaultVisionConfig.sign.dynamicSpeedLimitThreshold
    }

    override val signsAdditionalThreshold: Flow<Float> = dataStore.data.map {
        it[keySignsAdditionalThreshold] ?: defaultVisionConfig.sign.additionalDetectorThreshold
    }

    override val signsIgnoreOnCars: Flow<Boolean> = dataStore.data.map {
        it[keySignsIgnoreOnCars] ?: defaultVisionConfig.sign.ignoreSignsOnCar
    }

    override val signsSpeedLimitsOnly: Flow<Boolean> = dataStore.data.map {
        it[keySignsSpeedLimitsOnly] ?: false
    }

    override val vehiclesActive: Flow<Boolean> = dataStore.data.map {
        it[keyVehiclesActive] ?: defaultVisionConfig.objects.active
    }

    override val vehiclesRate: Flow<VisionPerformance.Rate> = dataStore.data.map {
        it[keyVehiclesRate]?.toRate() ?: defaultVisionConfig.objects.performance.rate
    }

    override val vehiclesDetectorThreshold: Flow<Float> = dataStore.data.map {
        it[keyVehiclesDetectorThreshold] ?: defaultVisionConfig.objects.detectorThreshold
    }

    override val tailgatingTimeDistance: Flow<Float> = dataStore.data.map {
        it[keyTailgatingTimeDistance] ?: defaultVisionLogicConfig.tailgatingTimeDistance
    }

    override val tailgatingDuration: Flow<Float> = dataStore.data.map {
        it[keyTailgatingDuration] ?: defaultVisionLogicConfig.tailgatingDuration
    }

    override val fastFocusTailgatingDuration: Flow<Float> = dataStore.data.map {
        it[keyFfTailgatingDuration] ?: defaultVisionLogicConfig.fastFocusTailgatingDuration
    }

    override val lanesActive: Flow<Boolean> = dataStore.data.map {
        it[keyLanesActive] ?: defaultVisionConfig.lane.active
    }

    override val lanesPauseWhenNotMoving: Flow<Boolean> = dataStore.data.map {
        it[keyLanesPauseWhenNotMoving] ?: defaultVisionConfig.lane.pauseWhenNotMoving
    }

    override val lanesFastFocusMode: Flow<Boolean> = dataStore.data.map {
        it[keyLanesFastFocusMode] ?: defaultVisionConfig.lane.fastFocusMode
    }

    override val lanesDynamicFocus: Flow<Boolean> = dataStore.data.map {
        it[keyLanesDynamicFocus] ?: defaultVisionConfig.lane.dynamicFocusAxis
    }

    override val lanesMinSamples: Flow<Int> = dataStore.data.map {
        it[keyLanesMinSamples] ?: defaultVisionConfig.lane.minFocusLineSamples
    }

    override val lanesMaxSamples: Flow<Int> = dataStore.data.map {
        it[keyLanesMaxSamples] ?: defaultVisionConfig.lane.maxFocusLineSamples
    }

    override val roadActive: Flow<Boolean> = dataStore.data.map {
        it[keyRoadActive] ?: defaultVisionConfig.road.active
    }

    override val roadRate: Flow<VisionPerformance.Rate> = dataStore.data.map {
        it[keyRoadRate]?.toRate() ?: defaultVisionConfig.road.performance.rate
    }

    override val roadMode: Flow<VisionPerformance.Mode> = dataStore.data.map {
        it[keyRoadMode]?.toMode() ?: defaultVisionConfig.road.performance.mode
    }

    override val textActive: Flow<Boolean> = dataStore.data.map {
        it[keyTextActive] ?: defaultVisionConfig.text.active
    }

    override val textRate: Flow<VisionPerformance.Rate> = dataStore.data.map {
        it[keyTextRate]?.toRate() ?: defaultVisionConfig.text.performance.rate
    }

    override val textShowOnCarsOnly: Flow<Boolean> = dataStore.data.map {
        it[keyTextShowOnCarsOnly] ?: defaultVisionConfig.text.showOnCarsOnly
    }

    override val arActive: Flow<Boolean> = dataStore.data.map {
        it[keyArActive] ?: defaultVisionConfig.ar.active
    }

    override val arFeatureTracking: Flow<Boolean> = dataStore.data.map {
        it[keyArFeatureTracking] ?: defaultVisionConfig.ar.featureTracking
    }

    override val arHeadingCorrection: Flow<Boolean> = dataStore.data.map {
        it[keyArHeadingCorrection] ?: defaultVisionConfig.ar.headingCorrection
    }

    override val dashcamActive: Flow<Boolean> = dataStore.data.map {
        it[keyDashcamActive] ?: false
    }

    override val dashcamVideoDuration: Flow<Int> = dataStore.data.map {
        it[keyDashcamVideoDuration] ?: 1
    }

    override suspend fun setSignsActive(active: Boolean) {
        dataStore.edit { it[keySignsActive] = active }
        onVisionConfigChanged()
    }

    override suspend fun setVehicleType(vehicleType: VehicleType) {
        dataStore.edit { it[keyVehicleType] = vehicleType.ordinal }
        onVisionLogicConfigChanged()
    }

    override suspend fun setSignsRate(rate: VisionPerformance.Rate) {
        dataStore.edit { it[keySignsRate] = rate.ordinal }
        onVisionConfigChanged()
    }

    override suspend fun setSignsClassificatorThreshold(threshold: Float) {
        dataStore.edit { it[keySignsClassificatorThreshold] = threshold }
        onVisionConfigChanged()
    }

    override suspend fun setSignsStandardThreshold(threshold: Float) {
        dataStore.edit { it[keySignsDetectorThreshold] = threshold }
        onVisionConfigChanged()
    }

    override suspend fun setSignsDynamicThreshold(threshold: Float) {
        dataStore.edit { it[keySignsDetectorThreshold] = threshold }
        onVisionConfigChanged()
    }

    override suspend fun setSignsAdditionalThreshold(threshold: Float) {
        dataStore.edit { it[keySignsDetectorThreshold] = threshold }
        onVisionConfigChanged()
    }

    override suspend fun setSignsIgnoreOnCars(ignore: Boolean) {
        dataStore.edit { it[keySignsIgnoreOnCars] = ignore }
        onVisionConfigChanged()
    }

    override suspend fun setSignsSpeedLimitsOnly(speedLimitsOnly: Boolean) {
        dataStore.edit { it[keySignsSpeedLimitsOnly] = speedLimitsOnly }
        onVisionConfigChanged()
    }

    override suspend fun setVehiclesActive(active: Boolean) {
        dataStore.edit { it[keyVehiclesActive] = active }
        onVisionConfigChanged()
    }

    override suspend fun setVehiclesRate(rate: VisionPerformance.Rate) {
        dataStore.edit { it[keyVehiclesRate] = rate.ordinal }
        onVisionConfigChanged()
    }

    override suspend fun setVehiclesDetectorThreshold(threshold: Float) {
        dataStore.edit { it[keyVehiclesDetectorThreshold] = threshold }
        onVisionConfigChanged()
    }

    override suspend fun setTailgatingTimeDistance(timeDistance: Float) {
        dataStore.edit { it[keyTailgatingTimeDistance] = timeDistance }
        onVisionLogicConfigChanged()
    }

    override suspend fun setTailgatingDuration(duration: Float) {
        dataStore.edit { it[keyTailgatingDuration] = duration }
        onVisionLogicConfigChanged()
    }

    override suspend fun setFastFocusTailgatingDuration(duration: Float) {
        dataStore.edit { it[keyFfTailgatingDuration] = duration }
        onVisionLogicConfigChanged()
    }

    override suspend fun setLanesActive(active: Boolean) {
        dataStore.edit { it[keyLanesActive] = active }
        onVisionConfigChanged()
    }

    override suspend fun setLanesPauseWhenNotMoving(pause: Boolean) {
        dataStore.edit { it[keyLanesPauseWhenNotMoving] = pause }
        onVisionConfigChanged()
    }

    override suspend fun setLanesFastFocusMode(ffMode: Boolean) {
        dataStore.edit { it[keyLanesFastFocusMode] = ffMode }
        onVisionConfigChanged()
    }

    override suspend fun setLanesDynamicFocus(dynamicFocus: Boolean) {
        dataStore.edit { it[keyLanesDynamicFocus] = dynamicFocus }
        onVisionConfigChanged()
    }

    override suspend fun setLanesMinSamples(minSamples: Int) {
        dataStore.edit { it[keyLanesMinSamples] = minSamples }
        onVisionConfigChanged()
    }

    override suspend fun setLanesMaxSamples(maxSamples: Int) {
        dataStore.edit { it[keyLanesMaxSamples] = maxSamples }
        onVisionConfigChanged()
    }

    override suspend fun setRoadActive(active: Boolean) {
        dataStore.edit { it[keyRoadActive] = active }
        onVisionConfigChanged()
    }

    override suspend fun setRoadRate(rate: VisionPerformance.Rate) {
        dataStore.edit { it[keyRoadRate] = rate.ordinal }
        onVisionConfigChanged()
    }

    override suspend fun setRoadMode(mode: VisionPerformance.Mode) {
        dataStore.edit { it[keyRoadRate] = mode.ordinal }
        onVisionConfigChanged()
    }

    override suspend fun setTextActive(active: Boolean) {
        dataStore.edit { it[keyTextActive] = active }
        onVisionConfigChanged()
    }

    override suspend fun setTextRate(rate: VisionPerformance.Rate) {
        dataStore.edit { it[keyTextRate] = rate.ordinal }
        onVisionConfigChanged()
    }

    override suspend fun setTextShowOnCarsOnly(onCarsOnly: Boolean) {
        dataStore.edit { it[keyTextShowOnCarsOnly] = onCarsOnly }
        onVisionConfigChanged()
    }

    override suspend fun setArActive(active: Boolean) {
        dataStore.edit { it[keyArActive] = active }
        onVisionConfigChanged()
    }

    override suspend fun setArFeatureTracking(featureTracking: Boolean) {
        dataStore.edit { it[keyArFeatureTracking] = featureTracking }
        onVisionConfigChanged()
    }

    override suspend fun setArHeadingCorrection(headingCorrection: Boolean) {
        dataStore.edit { it[keyArHeadingCorrection] = headingCorrection }
        onVisionConfigChanged()
    }

    override suspend fun setDashcamActive(active: Boolean) {
        dataStore.edit { it[keyDashcamActive] = active }
        onVisionConfigChanged()
    }

    override suspend fun setDashcamVideoDuration(duration: Int) {
        dataStore.edit { it[keyDashcamVideoDuration] = duration }
        onVisionConfigChanged()
    }

    override suspend fun resetToDefaults() {
        dataStore.edit { it.clear() }
        onVisionConfigChanged()
        onVisionLogicConfigChanged()
    }

    private suspend fun onVisionConfigChanged() {
        _visionConfiguration.emit(getVisionConfig())
    }

    private suspend fun onVisionLogicConfigChanged() {
        _visionLogicConfiguration.emit(getVisionLogicConfig())
    }

    private suspend fun getVisionConfig(): VisionConfig {
        return VisionConfig(
            sign = VisionModuleSignConfig(
                active = signsActive.first(),
                classificatorThreshold = signsClassificatorThreshold.first(),
                detectorThreshold = signsStandardThreshold.first(),
                dynamicSpeedLimitThreshold = signsDynamicThreshold.first(),
                additionalDetectorThreshold = signsAdditionalThreshold.first(),
                ignoreSignsOnCar = signsIgnoreOnCars.first(),
                performance = VisionPerformance(
                    mode = defaultVisionConfig.sign.performance.mode,
                    rate = signsRate.first()
                )
            ),
            road = VisionModuleRoadConfig(
                active = roadActive.first(),
                performance = VisionPerformance(
                    mode = roadMode.first(),
                    rate = roadRate.first()
                )
            ),
            objects = VisionModuleObjectConfig(
                active = vehiclesActive.first(),
                detectorThreshold = vehiclesDetectorThreshold.first(),
                performance = VisionPerformance(
                    mode = defaultVisionConfig.objects.performance.mode,
                    rate = roadRate.first()
                )
            ),
            lane = VisionModuleLaneConfig(
                active = lanesActive.first(),
                dynamicFocusAxis = lanesDynamicFocus.first(),
                minFocusLineSamples = lanesMinSamples.first(),
                maxFocusLineSamples = lanesMaxSamples.first(),
                pauseWhenNotMoving = lanesPauseWhenNotMoving.first(),
                fastFocusMode = lanesFastFocusMode.first()
            ),
            text = VisionModuleTextConfig(
                active = textActive.first(),
                showOnCarsOnly = textShowOnCarsOnly.first(),
                performance = VisionPerformance(
                    mode = defaultVisionConfig.text.performance.mode,
                    rate = roadRate.first()
                )
            ),
            ar = VisionModuleArConfig(
                active = arActive.first(),
                featureTracking = arFeatureTracking.first(),
                headingCorrection = arHeadingCorrection.first()
            )
        )
    }

    private suspend fun getVisionLogicConfig(): VisionLogicConfig {
        return VisionLogicConfig(
            vehicleType = vehicleType.first(),
            tailgatingTimeDistance = tailgatingTimeDistance.first(),
            tailgatingDuration = tailgatingDuration.first(),
            fastFocusTailgatingDuration = fastFocusTailgatingDuration.first()
        )
    }

    init {
        scope.launch {
            _visionConfiguration.emit(getVisionConfig())
        }
        scope.launch {
            _visionLogicConfiguration.emit(getVisionLogicConfig())
        }
    }
}

fun Int.toVehicleType(): VehicleType {
    return if(this in VehicleType.values().indices)
        VehicleType.values()[this]
    else
        VehicleType.Car
}

fun Int.toRate(): VisionPerformance.Rate {
    return if(this in VisionPerformance.Rate.values().indices)
        VisionPerformance.Rate.values()[this]
    else
        VisionPerformance.Rate.Medium
}

fun Int.toMode(): VisionPerformance.Mode {
    return if(this in VisionPerformance.Mode.values().indices)
        VisionPerformance.Mode.values()[this]
    else
        VisionPerformance.Mode.Fixed
}