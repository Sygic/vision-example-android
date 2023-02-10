package com.sygic.adas.visiontestapp.core.settings

import android.content.Context
import com.sygic.adas.vision.VisionConfig
import com.sygic.adas.vision.VisionPerformance
import com.sygic.adas.vision.logic.VehicleType
import com.sygic.adas.vision.logic.VisionLogicConfig
import com.sygic.adas.visiontestapp.R
import kotlinx.coroutines.flow.Flow

abstract class AppSettings(context: Context) {

    val prefKeySignActive = context.getString(R.string.key_sign_active)
    val prefKeyVehicleType = context.getString(R.string.key_sign_vehicle_type)
    val prefKeySignRate = context.getString(R.string.key_sign_rate)
    val prefKeySignClassThreshold = context.getString(R.string.key_sign_class_threshold)
    val prefKeySignDetectorThreshold = context.getString(R.string.key_sign_detector_threshold)
    val prefKeySignDynamicThreshold = context.getString(R.string.key_sign_dynamic_threshold)
    val prefKeySignAdditionalThreshold = context.getString(R.string.key_sign_additional_threshold)
    val prefKeySignIgnoreOnCars = context.getString(R.string.key_sign_ignore_on_cars)
    val prefKeySpeedLimitOnly = context.getString(R.string.key_sign_speed_limit_only)
    val prefKeyVehicleActive = context.getString(R.string.key_car_active)
    val prefKeyVehicleRate = context.getString(R.string.key_car_rate)
    val prefKeyVehicleThreshold = context.getString(R.string.key_car_threshold)
    val prefKeyTailgatingDistance = context.getString(R.string.key_car_tailgating_distance)
    val prefKeyTailgatingDuration = context.getString(R.string.key_car_tailgating_duration)
    val prefKeyFfTailgatingDuration = context.getString(R.string.key_car_ff_tailgating_duration)
    val prefKeyLaneActive = context.getString(R.string.key_lane_active)
    val prefKeyLanePauseWhenNotMoving = context.getString(R.string.key_lane_pause_when_not_moving)
    val prefKeyLaneFastFocusMode = context.getString(R.string.key_lane_fast_focus_mode)
    val prefKeyLaneDynamicFocus = context.getString(R.string.key_lane_dynamic_focus)
    val prefKeyLaneMinSamples = context.getString(R.string.key_lane_min_samples)
    val prefKeyLaneMaxSamples = context.getString(R.string.key_lane_max_samples)
    val prefKeyRoadActive = context.getString(R.string.key_road_active)
    val prefKeyRoadRate = context.getString(R.string.key_road_rate)
    val prefKeyRoadMode = context.getString(R.string.key_road_mode)
    val prefKeyTextActive = context.getString(R.string.key_text_active)
    val prefKeyTextRate = context.getString(R.string.key_text_rate)
    val prefKeyTextOnCarsOnly = context.getString(R.string.key_text_on_cars_only)
    val prefKeyResetToDefaults = context.getString(R.string.key_reset_to_defaults)
    val prefKeyArActive = context.getString(R.string.key_ar_active)
    val prefKeyArHeadingCorrection = context.getString(R.string.key_ar_heading_correction)
    val prefKeyArFeatureTracking = context.getString(R.string.key_ar_feature_tracking)
    val prefKeyDashcamActive = context.getString(R.string.key_dashcam_active)
    val prefKeyDashcamVideoDuration = context.getString(R.string.key_dashcam_video_duration)

    abstract val visionConfiguration: Flow<VisionConfig>
    abstract val visionLogicConfiguration: Flow<VisionLogicConfig>

    abstract val signsActive: Flow<Boolean>
    abstract val vehicleType: Flow<VehicleType>
    abstract val signsRate: Flow<VisionPerformance.Rate>
    abstract val signsClassificatorThreshold: Flow<Float>
    abstract val signsStandardThreshold: Flow<Float>
    abstract val signsDynamicThreshold: Flow<Float>
    abstract val signsAdditionalThreshold: Flow<Float>
    abstract val signsIgnoreOnCars: Flow<Boolean>
    abstract val signsSpeedLimitsOnly: Flow<Boolean>

    abstract val vehiclesActive: Flow<Boolean>
    abstract val vehiclesRate: Flow<VisionPerformance.Rate>
    abstract val vehiclesDetectorThreshold: Flow<Float>
    abstract val tailgatingTimeDistance: Flow<Float>
    abstract val tailgatingDuration: Flow<Float>
    abstract val fastFocusTailgatingDuration: Flow<Float>

    abstract val lanesActive: Flow<Boolean>
    abstract val lanesPauseWhenNotMoving: Flow<Boolean>
    abstract val lanesFastFocusMode: Flow<Boolean>
    abstract val lanesDynamicFocus: Flow<Boolean>
    abstract val lanesMinSamples: Flow<Int>
    abstract val lanesMaxSamples: Flow<Int>

    abstract val roadActive: Flow<Boolean>
    abstract val roadRate: Flow<VisionPerformance.Rate>
    abstract val roadMode: Flow<VisionPerformance.Mode>

    abstract val textActive: Flow<Boolean>
    abstract val textRate: Flow<VisionPerformance.Rate>
    abstract val textShowOnCarsOnly: Flow<Boolean>

    abstract val arActive: Flow<Boolean>
    abstract val arFeatureTracking: Flow<Boolean>
    abstract val arHeadingCorrection: Flow<Boolean>

    abstract val dashcamActive: Flow<Boolean>
    abstract val dashcamVideoDuration: Flow<Int>

    abstract suspend fun setSignsActive(active: Boolean)
    abstract suspend fun setVehicleType(vehicleType: VehicleType)
    abstract suspend fun setSignsRate(rate: VisionPerformance.Rate)
    abstract suspend fun setSignsClassificatorThreshold(threshold: Float)
    abstract suspend fun setSignsStandardThreshold(threshold: Float)
    abstract suspend fun setSignsDynamicThreshold(threshold: Float)
    abstract suspend fun setSignsAdditionalThreshold(threshold: Float)
    abstract suspend fun setSignsIgnoreOnCars(ignore: Boolean)
    abstract suspend fun setSignsSpeedLimitsOnly(speedLimitsOnly: Boolean)

    abstract suspend fun setVehiclesActive(active: Boolean)
    abstract suspend fun setVehiclesRate(rate: VisionPerformance.Rate)
    abstract suspend fun setVehiclesDetectorThreshold(threshold: Float)
    abstract suspend fun setTailgatingTimeDistance(timeDistance: Float)
    abstract suspend fun setTailgatingDuration(duration: Float)
    abstract suspend fun setFastFocusTailgatingDuration(duration: Float)

    abstract suspend fun setLanesActive(active: Boolean)
    abstract suspend fun setLanesPauseWhenNotMoving(pause: Boolean)
    abstract suspend fun setLanesFastFocusMode(ffMode: Boolean)
    abstract suspend fun setLanesDynamicFocus(dynamicFocus: Boolean)
    abstract suspend fun setLanesMinSamples(minSamples: Int)
    abstract suspend fun setLanesMaxSamples(maxSamples: Int)

    abstract suspend fun setRoadActive(active: Boolean)
    abstract suspend fun setRoadRate(rate: VisionPerformance.Rate)
    abstract suspend fun setRoadMode(mode: VisionPerformance.Mode)

    abstract suspend fun setTextActive(active: Boolean)
    abstract suspend fun setTextRate(rate: VisionPerformance.Rate)
    abstract suspend fun setTextShowOnCarsOnly(onCarsOnly: Boolean)

    abstract suspend fun setArActive(active: Boolean)
    abstract suspend fun setArHeadingCorrection(correction: Boolean)
    abstract suspend fun setArFeatureTracking(featureTracking: Boolean)

    abstract suspend fun setDashcamActive(active: Boolean)
    abstract suspend fun setDashcamVideoDuration(duration: Int)

    abstract suspend fun resetToDefaults()

    companion object {
        private var settingsImpl: AppSettings? = null

        fun get(context: Context): AppSettings {
            synchronized(this) {
                if (settingsImpl == null) {
                    settingsImpl = AppSettingsImpl(context)
                }
                return settingsImpl!!

            }
        }
    }
}

