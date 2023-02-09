package com.sygic.adas.visiontestapp.core.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Range
import android.util.Size
import android.util.SizeF
import com.sygic.adas.vision.CameraParams

typealias IntRange = Range<Int>

class CameraParamsProvider(context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    val frameRate: IntRange
    private val focalLength: Float
    private val sensorSize: SizeF
    private val imageSize: Size

    init {
        with(cameraManager.getCameraCharacteristics(cameraManager.cameraIdList[0])) {
            focalLength = get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.get(0) ?: 0.0f
            frameRate = getHighestStableFrameRate(this) ?: Range(15, 15)
            sensorSize = get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) ?: SizeF(0f, 0f)
            imageSize = get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE) ?: Size(0, 0)    // ?
        }
    }

    fun getParams() = CameraParams(focalLength, sensorSize, frameRate.lower, imageSize)

    private fun getHighestStableFrameRate(cameraCharacteristics: CameraCharacteristics): IntRange? {
        val frameRates = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) ?: return null
        if(frameRates.isEmpty())
            return null

        var max = 0
        var result: IntRange? = null
        for(frameRate in frameRates) {
            if(frameRate.lower == frameRate.upper && frameRate.lower > max) {
                max = frameRate.lower
                result = frameRate
            }
        }

        return result ?: frameRates[frameRates.size - 1]
    }

}