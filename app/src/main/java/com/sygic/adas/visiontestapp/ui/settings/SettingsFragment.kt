package com.sygic.adas.visiontestapp.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.*
import com.sygic.adas.vision.VisionPerformance
import com.sygic.adas.vision.logic.VehicleType
import com.sygic.adas.visiontestapp.R
import com.sygic.adas.visiontestapp.core.format
import com.sygic.adas.visiontestapp.core.launchAndRepeatWithViewLifecycle
import com.sygic.adas.visiontestapp.core.settings.AppSettings
import com.sygic.adas.visiontestapp.getSupportActionBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch


class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var settings: AppSettings

    private lateinit var valRateUltraLow: String
    private lateinit var valRateLow: String
    private lateinit var valRateMedium: String
    private lateinit var valRateHigh: String

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().getSupportActionBar()?.let {
            it.show()
            it.setDisplayHomeAsUpEnabled(true)
            it.title = "Settings"
        }

        settings = AppSettings.get(context)

        valRateUltraLow = getString(R.string.val_rate_ultra_low)
        valRateLow = getString(R.string.val_rate_low)
        valRateMedium = getString(R.string.val_rate_medium)
        valRateHigh = getString(R.string.val_rate_high)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Signs - active
        switchPref(settings.prefKeySignActive)?.let { pref ->
            pref.setupSwitchPreference(
                setting = settings.signsActive,
                enableBy = null
            ) { active -> settings.setSignsActive(active) }
        }

        // Vehicle type
        dropDownPref(settings.prefKeyVehicleType)?.let { pref ->
            launchAndRepeatWithViewLifecycle {
                settings.vehicleType.collect { vehicleType ->
                    when (vehicleType) {
                        VehicleType.Car -> {
                            pref.value = getString(R.string.val_vehicle_car)
                            pref.summary = getString(R.string.settings_vehicle_car)
                        }
                        VehicleType.Truck -> {
                            pref.value = getString(R.string.val_vehicle_truck)
                            pref.summary = getString(R.string.settings_vehicle_truck)
                        }
                    }
                }
            }
            pref.enableBy(settings.signsActive)
            pref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                (newValue as? String)?.let {
                    val newVehicleType = when (it) {
                        getString(R.string.val_vehicle_truck) -> VehicleType.Truck
                        else -> VehicleType.Car
                    }
                    launch { settings.setVehicleType(newVehicleType) }
                }
                true
            }
        }

        // Signs - rate
        dropDownPref(settings.prefKeySignRate)?.let { pref ->
            pref.setupRatePreference(
                setting = settings.signsRate,
                enableBy = settings.signsActive
            ) { rate ->
                settings.setSignsRate(rate)
            }
        }

        // Signs - classificator threshold
        seekBarPref(settings.prefKeySignClassThreshold)?.let { pref ->
            pref.setupSeekBarFloatPreference(
                setting = settings.signsClassificatorThreshold,
                name = getString(R.string.settings_class_threshold),
                enableBy = settings.signsActive,
                minValue = 0,
                maxValue = 100
            ) { threshold -> settings.setSignsClassificatorThreshold(threshold) }
        }

        // Signs - detector threshold
        seekBarPref(settings.prefKeySignDetectorThreshold)?.let { pref ->
            pref.setupSeekBarFloatPreference(
                setting = settings.signsStandardThreshold,
                name = getString(R.string.settings_detector_threshold),
                enableBy = settings.signsActive,
                minValue = 0,
                maxValue = 100
            ) { threshold -> settings.setSignsStandardThreshold(threshold) }
        }

        // Signs - ignore on cars
        switchPref(settings.prefKeySignIgnoreOnCars)?.let { pref ->
            pref.setupSwitchPreference(
                setting = settings.signsIgnoreOnCars,
                enableBy = settings.signsActive
            ) { ignore -> settings.setSignsIgnoreOnCars(ignore) }
        }

        // Signs - show speed limits only
        switchPref(settings.prefKeySpeedLimitOnly)?.let { pref ->
            pref.setupSwitchPreference(
                setting = settings.signsSpeedLimitsOnly,
                enableBy = settings.signsActive
            ) { speedLimitsOnly -> settings.setSignsSpeedLimitsOnly(speedLimitsOnly) }
        }

        // Vehicles - active
        switchPref(settings.prefKeyVehicleActive)?.let { pref ->
            pref.setupSwitchPreference(
                setting = settings.vehiclesActive,
                enableBy = null
            ) { active -> settings.setVehiclesActive(active) }
        }

        // Vehicles - rate
        dropDownPref(settings.prefKeyVehicleRate)?.let { pref ->
            pref.setupRatePreference(
                setting = settings.vehiclesRate,
                enableBy = settings.vehiclesActive
            ) { rate -> settings.setVehiclesRate(rate) }
        }

        // Vehicles - threshold
        seekBarPref(settings.prefKeyVehicleThreshold)?.let { pref ->
            pref.setupSeekBarFloatPreference(
                setting = settings.vehiclesDetectorThreshold,
                name = getString(R.string.settings_detector_threshold),
                enableBy = settings.vehiclesActive,
                minValue = 0,
                maxValue = 100
            ) { threshold -> settings.setVehiclesDetectorThreshold(threshold) }
        }

        // Vehicles - tailgating distance
        seekBarPref(settings.prefKeyTailgatingDistance)?.let { pref ->
            pref.setupSeekBarFloatPreference(
                setting = settings.tailgatingTimeDistance,
                name = getString(R.string.settings_tailgating_distance),
                enableBy = settings.vehiclesActive,
                minValue = 0,
                maxValue = 300
            ) { distance -> settings.setTailgatingTimeDistance(distance) }
        }

        // Vehicles - tailgating duration
        seekBarPref(settings.prefKeyTailgatingDuration)?.let { pref ->
            pref.setupSeekBarFloatPreference(
                setting = settings.tailgatingDuration,
                name = getString(R.string.settings_tailgating_duration),
                enableBy = settings.vehiclesActive,
                minValue = 0,
                maxValue = 300
            ) { duration -> settings.setTailgatingDuration(duration) }
        }

        // Vehicles - FF tailgating duration
        seekBarPref(settings.prefKeyFfTailgatingDuration)?.let { pref ->
            pref.setupSeekBarFloatPreference(
                setting = settings.fastFocusTailgatingDuration,
                name = getString(R.string.settings_ff_tailgating_duration),
                enableBy = settings.vehiclesActive,
                minValue = 0,
                maxValue = 300
            ) { duration -> settings.setFastFocusTailgatingDuration(duration) }
        }

        // Road - active
        switchPref(settings.prefKeyRoadActive)?.let { pref ->
            pref.setupSwitchPreference(
                setting = settings.roadActive,
                enableBy = null
            ) { active -> settings.setRoadActive(active) }
        }

        // Road - rate
        dropDownPref(settings.prefKeyRoadRate)?.let { pref ->
            pref.setupRatePreference(
                setting = settings.roadRate,
                enableBy = settings.roadActive
            ) { rate -> settings.setRoadRate(rate) }
        }

        // Road - mode
        dropDownPref(settings.prefKeyRoadMode)?.let { pref ->
            pref.setupModePreference(
                setting = settings.roadMode,
                enableBy = settings.roadActive
            ) { mode -> settings.setRoadMode(mode) }
        }

        // Lanes - active
        switchPref(settings.prefKeyLaneActive)?.let { pref ->
            pref.setupSwitchPreference(
                setting = settings.lanesActive,
                enableBy = null
            ) { active -> settings.setLanesActive(active) }
        }

        // Lanes - pause when not moving
        switchPref(settings.prefKeyLanePauseWhenNotMoving)?.let { pref ->
            pref.setupSwitchPreference(
                setting = settings.lanesPauseWhenNotMoving,
                enableBy = settings.lanesActive
            ) { pause -> settings.setLanesPauseWhenNotMoving(pause) }
        }

        // Lanes - fast focus mode
        switchPref(settings.prefKeyLaneFastFocusMode)?.let { pref ->
            pref.setupSwitchPreference(
                setting = settings.lanesFastFocusMode,
                enableBy = settings.lanesActive
            ) { ffMode -> settings.setLanesFastFocusMode(ffMode) }
        }

        // Lanes - dynamic focus axis
        switchPref(settings.prefKeyLaneDynamicFocus)?.let { pref ->
            pref.setupSwitchPreference(
                setting = settings.lanesDynamicFocus,
                enableBy = settings.lanesActive
            ) { dynamicFocus -> settings.setLanesDynamicFocus(dynamicFocus) }
        }

        // Lanes - min focus samples
        seekBarPref(settings.prefKeyLaneMinSamples)?.let { pref ->
            pref.setupSeekBarIntPreference(
                setting = settings.lanesMinSamples,
                name = getString(R.string.settings_lane_min_samples),
                minValue = 0,
                maxValue = 100,
                enableBy = settings.lanesActive
            ) { samples -> settings.setLanesMinSamples(samples) }
        }

        // Lanes - max focus samples
        seekBarPref(settings.prefKeyLaneMaxSamples)?.let { pref ->
            pref.setupSeekBarIntPreference(
                setting = settings.lanesMaxSamples,
                name = getString(R.string.settings_lane_max_samples),
                minValue = 0,
                maxValue = 100,
                enableBy = settings.lanesActive
            ) { samples -> settings.setLanesMaxSamples(samples) }
        }
        // Text - active
        switchPref(settings.prefKeyTextActive)?.let { pref ->
            pref.setupSwitchPreference(
                setting = settings.textActive,
                enableBy = null
            ) { active -> settings.setTextActive(active) }
        }

        // Text - rate
        dropDownPref(settings.prefKeyTextRate)?.let { pref ->
            pref.setupRatePreference(
                setting = settings.textRate,
                enableBy = settings.textActive
            ) { rate -> settings.setTextRate(rate) }
        }

        // Text - on cars only
        switchPref(settings.prefKeyTextOnCarsOnly)?.let { pref ->
            pref.setupSwitchPreference(
                setting = settings.textShowOnCarsOnly,
                enableBy = settings.textActive
            ) { onCarsOnly -> settings.setTextShowOnCarsOnly(onCarsOnly) }
        }

        // AR - active
        switchPref(settings.prefKeyArActive)?.let { pref ->
            pref.setupSwitchPreference(
                setting = settings.arActive,
                enableBy = null
            ) { active -> settings.setArActive(active) }
        }

        // AR - heading correction
        switchPref(settings.prefKeyArHeadingCorrection)?.let { pref ->
            pref.setupSwitchPreference(
                setting = settings.arHeadingCorrection,
                enableBy = settings.arActive
            ) { correction -> settings.setArHeadingCorrection(correction) }
        }

        // AR - feature tracking
        switchPref(settings.prefKeyArFeatureTracking)?.let { pref ->
            pref.setupSwitchPreference(
                setting = settings.arFeatureTracking,
                enableBy = settings.arActive
            ) { ft -> settings.setArFeatureTracking(ft) }
        }

        // Reset to defaults
        pref(settings.prefKeyResetToDefaults)?.let { pref ->
            pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                launch {
                    settings.resetToDefaults()
                    Toast.makeText(requireContext(), "Settings reset to default", Toast.LENGTH_SHORT).show()
                }
                true
            }
        }
        // Dashcam - active
        switchPref(settings.prefKeyDashcamActive)?.let { pref ->
            pref.setupSwitchPreference(
                setting = settings.dashcamActive,
                enableBy = null
            ) { active -> settings.setDashcamActive(active) }
        }
        // Dashcam - video duration
        seekBarPref(settings.prefKeyDashcamVideoDuration)?.let { pref ->
            pref.setupSeekBarIntPreference(
                setting = settings.dashcamVideoDuration,
                name = getString(R.string.settings_dashcam_video_duration),
                minValue = 0,
                maxValue = 15,
                enableBy = settings.dashcamActive
            ) { duration -> settings.setDashcamVideoDuration(duration) }
        }

    }

    private fun launch(block: suspend CoroutineScope.() -> Unit) {
        lifecycleScope.launch(block = block)
    }

    private fun SwitchPreference.setupSwitchPreference(
        setting: Flow<Boolean>,
        enableBy: Flow<Boolean>?,
        updateSettingsBlock: suspend (Boolean) -> Unit
    ) {
        launchAndRepeatWithViewLifecycle {
            setting.collect { active ->
                isChecked = active
            }
        }
        onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            (newValue as? Boolean)?.let {
                launch { updateSettingsBlock(it) }
            }
            true
        }
        enableBy?.let {
            enableBy(it)
        }
    }

    private fun DropDownPreference.setupRatePreference(
        setting: Flow<VisionPerformance.Rate>,
        enableBy: Flow<Boolean>,
        updateSettingsBlock: suspend (VisionPerformance.Rate) -> Unit
    ) {
        setEntries(R.array.settings_rates)
        setEntryValues(R.array.val_rates)
        launchAndRepeatWithViewLifecycle {
            setting.collect { rate ->
                when (rate) {
                    VisionPerformance.Rate.UltraLow -> {
                        value = valRateUltraLow
                        summary = getString(R.string.settings_rate_ultra_low)
                    }
                    VisionPerformance.Rate.Low -> {
                        value = valRateLow
                        summary = getString(R.string.settings_rate_low)
                    }
                    VisionPerformance.Rate.Medium -> {
                        value = valRateMedium
                        summary = getString(R.string.settings_rate_medium)
                    }
                    VisionPerformance.Rate.High -> {
                        value = valRateHigh
                        summary = getString(R.string.settings_rate_high)
                    }
                }
            }
        }
        enableBy(enableBy)
        onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            (newValue as? String)?.let {
                val newRate = when (it) {
                    valRateUltraLow -> VisionPerformance.Rate.UltraLow
                    valRateLow -> VisionPerformance.Rate.Low
                    valRateMedium -> VisionPerformance.Rate.Medium
                    valRateHigh -> VisionPerformance.Rate.High
                    else -> return@let
                }
                launch { updateSettingsBlock(newRate) }
            }
            true
        }
    }

    private fun DropDownPreference.setupModePreference(
        setting: Flow<VisionPerformance.Mode>,
        enableBy: Flow<Boolean>,
        updateSettings: suspend (VisionPerformance.Mode) -> Unit
    ) {
        setEntries(R.array.settings_modes)
        setEntryValues(R.array.val_modes)
        launchAndRepeatWithViewLifecycle {
            setting.collect { mode ->
                when (mode) {
                    VisionPerformance.Mode.Fixed -> {
                        value = getString(R.string.val_mode_fixed)
                        summary = getString(R.string.settings_mode_fixed)
                    }
                    VisionPerformance.Mode.Dynamic -> {
                        value = getString(R.string.val_mode_dynamic)
                        summary = getString(R.string.settings_mode_dynamic)
                    }
                }
            }
        }
        enableBy(enableBy)
        onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            (newValue as? String)?.let {
                val newMode = when (it) {
                    getString(R.string.val_mode_fixed) -> VisionPerformance.Mode.Fixed
                    getString(R.string.val_mode_dynamic) -> VisionPerformance.Mode.Dynamic
                    else -> return@let
                }
                launch { updateSettings(newMode) }
            }
            true
        }
    }

    private fun SeekBarPreference.setupSeekBarFloatPreference(
        setting: Flow<Float>,
        name: String,
        enableBy: Flow<Boolean>,
        minValue: Int,
        maxValue: Int,
        updateSettingsBlock: suspend (Float) -> Unit
    ) {
        title = name
        min = minValue
        max = maxValue

        launchAndRepeatWithViewLifecycle {
            setting.collect { settingValue ->
                title = "$name: ${settingValue.format(2)}"
                value = (settingValue * 100f).toInt()
            }
        }
        enableBy(enableBy)
        onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            (newValue as? Int)?.let {
                val newVal = it.toFloat() / 100f
                launch { updateSettingsBlock(newVal) }
            }
            true
        }
    }

    private fun SeekBarPreference.setupSeekBarIntPreference(
        setting: Flow<Int>,
        name: String,
        enableBy: Flow<Boolean>,
        minValue: Int,
        maxValue: Int,
        updateSettingsBlock: suspend (Int) -> Unit
    ) {
        title = name
        min = minValue
        max = maxValue

        launchAndRepeatWithViewLifecycle {
            setting.collect { settingValue ->
                title = "$name: $settingValue"
                value = settingValue
            }
        }
        enableBy(enableBy)
        onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            (newValue as? Int)?.let {
                launch { updateSettingsBlock(it) }
            }
            true
        }
    }

    private fun Preference.enableBy(isActive: Flow<Boolean>) {
        launchAndRepeatWithViewLifecycle {
            isActive.collect { active -> isEnabled = active }
        }
    }
}

private fun PreferenceFragmentCompat.pref(key: String) = findPreference<Preference>(key)
private fun PreferenceFragmentCompat.switchPref(key: String) = findPreference<SwitchPreference>(key)
private fun PreferenceFragmentCompat.seekBarPref(key: String) = findPreference<SeekBarPreference>(key)
private fun PreferenceFragmentCompat.dropDownPref(key: String) = findPreference<DropDownPreference>(key)