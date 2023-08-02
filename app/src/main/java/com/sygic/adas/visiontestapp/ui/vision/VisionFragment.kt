package com.sygic.adas.visiontestapp.ui.vision

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.sygic.adas.vision.Vision
import com.sygic.adas.visiontestapp.MainActivity
import com.sygic.adas.visiontestapp.R
import com.sygic.adas.visiontestapp.core.*
import com.sygic.adas.visiontestapp.core.camera.CameraManager
import com.sygic.adas.visiontestapp.core.settings.AppSettings
import com.sygic.adas.visiontestapp.core.util.MediaStoreUtil
import com.sygic.adas.visiontestapp.databinding.FragmentVisionBinding
import com.sygic.adas.visiontestapp.getSupportActionBar
import com.sygic.adas.visiontestapp.navigate
import com.sygic.adas.visiontestapp.ui.vision.components.DetectedSignsAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class VisionFragment : Fragment() {

    private val viewModel: VisionViewModel by viewModels()
    private lateinit var binding: FragmentVisionBinding

    private val detectedSignsAdapter = DetectedSignsAdapter()

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var cameraManager: CameraManager

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val permissionsResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val allGranted = it.all { granted -> granted.value }
            if(allGranted)
                onPermissionsGranted()
            else
                Toast.makeText(requireContext(), "All permissions must be granted", Toast.LENGTH_LONG).show()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requireActivity().getSupportActionBar()?.hide()

        binding = FragmentVisionBinding.inflate(inflater, container, false).apply {
            recyclerSigns.adapter = detectedSignsAdapter
            recyclerSigns.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            recyclerSigns.isNestedScrollingEnabled = false

            imgSettings.setOnClickListener {
                (requireActivity() as MainActivity)
                    .navigate(R.id.action_visionFragment_to_settingsFragment)
            }

            imgExport.setOnClickListener { showExportDialog() }
        }

        setupCamera()

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.ding).apply {
            isLooping = true
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .build()
            )
        }
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val settings = AppSettings.get(requireContext().applicationContext)
        val missingPermissions = checkMissingPermissions()

        if (missingPermissions.isNotEmpty())
            permissionsResultLauncher.launch(missingPermissions)
        else
            onPermissionsGranted()

        // show/hide Vision init error
        launchAndRepeatWithViewLifecycle {
            viewModel.visionInitState.collect { state ->
                val info = when (state) {
                    Vision.InitState.Initializing -> "Vision initializing"
                    Vision.InitState.InvalidLicense -> "Vision: Invalid license"
                    Vision.InitState.UnknownError -> "Vision: Unknown error"
                    else -> null
                }
                if (info != null)
                    showSnackbar(info)
                else
                    hideSnackbar()
            }
        }

        // actual speed limit
        launchAndRepeatWithViewLifecycle {
            viewModel.speedLimitKmh
                .mapNotNull { it.speedLimitDrawableId() }
                .collect { resId ->
                    binding.imgActualSpeedLimit.setImageResource(resId)
                }
        }

        // current speed from location provider
        launchAndRepeatWithViewLifecycle {
            viewModel.speedKmh.collect { speed ->
                binding.textCurrentSpeed.text = "$speed km/h"
            }
        }

        // Vision roads
        launchAndRepeatWithViewLifecycle {
            viewModel.roads.collect { road ->
                binding.visionOverlay.drawRoad(road)
            }
        }

        // Vision objects
        launchAndRepeatWithViewLifecycle {
            viewModel.visionObjects.collect { objects ->
                binding.visionOverlay.drawObjects(objects)
            }
        }

        // tailgating
        launchAndRepeatWithViewLifecycle {
            viewModel.tailgating
                .map { it?.tailgatingObject }
                .collect { tailgatingObject ->
                    binding.visionOverlay.tailgatingObject = tailgatingObject

                    // warning sound when tailgating
                    if (tailgatingObject != null)
                        playWarningSound()
                    else
                        stopWarningSound()
                }
        }

        // signs to last detected signs RecyclerView
        launchAndRepeatWithViewLifecycle {
            viewModel.detectedSigns.collect { signs ->
                signs.forEach {
                    detectedSignsAdapter.addSign(it)
                }
                binding.recyclerSigns.scrollToPosition(0)
            }
        }

        // license plates
        launchAndRepeatWithViewLifecycle {
            viewModel.licensePlates.collect { licensePlates ->
                binding.visionOverlay.drawLicensePlates(licensePlates)
            }
        }

        // AR object
        launchAndRepeatWithViewLifecycle {
            viewModel.arObject.filterNotNull().collect {
                Log.d(Constants.LOG_TAG, "AR object: $it")
            }
        }

        // fps
        launchAndRepeatWithViewLifecycle {
            combine(
                viewModel.fps,
                viewModel.objFps
            ) { fps, objFps ->
                "FPS ${fps.format(1)} | $objFps"
            }.collect { text ->
                binding.textObjFps.text = text
            }
        }

        // show speed limits only
        launchAndRepeatWithViewLifecycle {
            settings.signsSpeedLimitsOnly.collect { speedLimitsOnly ->
                binding.recyclerSigns.visibility =
                    if (speedLimitsOnly) View.INVISIBLE else View.VISIBLE
            }
        }

        // aspect ratio from CameraImageProvider to VisionOverlay
        launchAndRepeatWithViewLifecycle {
            cameraManager.aspectRatio
                .filter { it > 0f }
                .collect { aspectRatio ->
                    binding.visionOverlay.setImageAspectRatio(aspectRatio)
                }
        }

        // if analyzing preview bitmap is required and PreviewView is streaming, analyze bitmap
        launchAndRepeatWithViewLifecycle {
            combine(
                cameraManager.shouldAnalyzePreview,
                binding.cameraPreview.previewStreamState.asFlow()
            ) { shouldAnalyzePreview, previewStreamState ->
                shouldAnalyzePreview && previewStreamState == PreviewView.StreamState.STREAMING
            }.collectLatest { shouldAnalyzePreview ->
                if(shouldAnalyzePreview) {
                    viewModel.isReadyForProcessing.collect { isReadyForProcessing ->
                        if(isReadyForProcessing) {
                            binding.cameraPreview.bitmap?.let {
                                viewModel.onBitmap(it)
                            }
                        }
                    }
                }
            }
        }

        // blinking recording indicator
        launchAndRepeatWithViewLifecycle {
            cameraManager.recordingActive.collectLatest { active ->
                if(active) {
                    while(true) {
                        delay(800)
                        binding.cameraIndicatorDot.isVisible = !binding.cameraIndicatorDot.isVisible
                    }
                }
                else {
                    binding.cameraIndicatorDot.isVisible = false
                }
            }
        }
    }

    override fun onStop() {
        cameraManager.stopVideoRecording()
        super.onStop()
    }

    override fun onDestroy() {
        cameraManager.stopAll()
        mediaPlayer.release()
        super.onDestroy()
    }

    private fun setupCamera() {
        // get images from camera and send them to viewModel
        cameraManager = CameraManager(
            activity = requireActivity(),
            cameraPreview = binding.cameraPreview,
            viewLifecycleOwner = viewLifecycleOwner
        ) { image, rotation ->
            viewModel.onImage(image, rotation)
        }


    }

    private fun checkMissingPermissions(): Array<String> {
        val context = requireContext()
        return requiredPermissions
            .filterNot { permission ->
                ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }
            .toTypedArray()
    }

    private fun onPermissionsGranted() {
        launchOnViewLifecycle {
            cameraManager.prepare()
            cameraManager.start()

            launchAndRepeatWithViewLifecycle {
                viewModel.dashcamVideoDuration.collect {
                    cameraManager.videoDurationMin = it
                }
            }

            launchAndRepeatWithViewLifecycle {
                viewModel.dashcamEnabled.collect { enabled ->
                    if(enabled)
                        cameraManager.startVideoRecording()
                    else {
                        cameraManager.cleanRecordings(forceDeleteAll = true)
                        cameraManager.stopVideoRecording()
                    }
                }
            }

        }
        viewModel.onPermissionsGranted()
    }

    private fun playWarningSound() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
    }

    private fun stopWarningSound() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            mediaPlayer.seekTo(0)
        }
    }

    private fun showExportDialog() {
        if(cameraManager.hasRecordings()) {
            AlertDialog.Builder(requireContext())
                .setMessage("Export recorded video?")
                .setPositiveButton("Yes") { _, _ -> exportVideos() }
                .setNegativeButton("No") { _, _ -> /* just dismiss */}
                .create()
                .show()
        } else {
            showSnackbar("Nothing to export", Snackbar.LENGTH_LONG)
        }
    }

    private fun exportVideos() {
        launchOnViewLifecycle {
            cameraManager.stopVideoRecording()
            showSnackbar("Exporting...")
            // TODO leave some time for recorder to stop. find better solution
            delay(1000)
            MediaStoreUtil.exportVideos(requireContext(), cameraManager.getRecordings())
            showSnackbar("Videos exported to Movies/dashcam directory", Snackbar.LENGTH_LONG)
            if(viewModel.dashcamEnabled.first()) {
                cameraManager.cleanRecordings(forceDeleteAll = true)
                cameraManager.startVideoRecording()
            }
        }
    }

    private var snackbar: Snackbar? = null

    private fun showSnackbar(text: String, length: Int = Snackbar.LENGTH_INDEFINITE) {
        snackbar = Snackbar.make(binding.root, text, length).apply {
            show()
        }
    }

    private fun hideSnackbar() {
        snackbar?.dismiss()
        snackbar = null
    }
}
