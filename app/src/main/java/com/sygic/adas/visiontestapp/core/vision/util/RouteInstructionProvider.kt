package com.sygic.adas.visiontestapp.core.vision.util

import android.location.Location
import com.sygic.adas.vision.route.ManeuverType
import kotlinx.coroutines.flow.flowOf

// simulates route instructions from navigation
object RouteInstructionProvider {
    val instructions = flowOf(
        RouteInstruction(
            id = 100,
            location = Location("").apply {
                latitude = 48.774878
                longitude = 17.437890
            },
            maneuverType = ManeuverType.Right
        )
    )
}

data class RouteInstruction(
    val id: Int,
    val location: Location,
    val maneuverType: ManeuverType
)