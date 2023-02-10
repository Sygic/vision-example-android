package com.sygic.adas.visiontestapp.core

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.sygic.adas.vision.objects.Sign
import com.sygic.adas.visiontestapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlin.math.min


fun Sign.isSpeedLimit() =
    signType.ordinal >= Sign.Type.MaximumSpeedLimit10.ordinal && signType.ordinal <= Sign.Type.MaximumSpeedLimit130.ordinal

@DrawableRes
fun Sign.getDrawableId(): Int = when(this.signType) {
    Sign.Type.AccidentArea -> R.drawable.accident_area
    Sign.Type.BicycleAdditional -> R.drawable.bicycle_additional
    Sign.Type.BicyclesOnly -> R.drawable.bicycles_only
    Sign.Type.BusAdditional -> R.drawable.bus_additional
    Sign.Type.BusLane -> R.drawable.bus_lane
    Sign.Type.BusStop -> R.drawable.bus_stop
    Sign.Type.CautionSnowOrIce -> R.drawable.caution_snow_or_ice
    Sign.Type.Children -> R.drawable.children
    Sign.Type.CrossroadsWithAMinorRoad -> R.drawable.crossroads_with_a_minor_road
    Sign.Type.CrossroadsWithPriorityToTheRight -> R.drawable.crossroads_with_priority_to_the_right
    Sign.Type.CurveLeft -> R.drawable.curve_left
    Sign.Type.CurveRight -> R.drawable.curve_right
    Sign.Type.Cyclists -> R.drawable.cyclists
    Sign.Type.DomesticAnimals -> R.drawable.domestic_animals
    Sign.Type.DoubleCurveLeft -> R.drawable.double_curve_left
    Sign.Type.DoubleCurveRight -> R.drawable.double_curve_right
    Sign.Type.DoubleLaneEnds -> R.drawable.double_lane_ends
    Sign.Type.DownArrowAdditional -> R.drawable.down_arrow_additional
    Sign.Type.EndOfAllRestrictions -> R.drawable.end_of_all_restrictions
    Sign.Type.EndOfLimitedAccessRoad -> R.drawable.end_of_limited_access_road
    Sign.Type.EndOfMotorway -> R.drawable.end_of_motorway
    Sign.Type.EndOfOvertakingProhibition -> R.drawable.end_of_overtaking_prohibition
    Sign.Type.EndOfPriorityRoad -> R.drawable.end_of_priority_road
    Sign.Type.EquestrianPath -> R.drawable.equestrian_path
    Sign.Type.EscapeLane -> R.drawable.escape_lane
    Sign.Type.FallingRocksOrDebris -> R.drawable.falling_rocks_or_debris
    Sign.Type.FirstAid -> R.drawable.first_aid
    Sign.Type.General -> R.drawable.other_danger
    Sign.Type.GiveWay -> R.drawable.give_way
    Sign.Type.GiveWayToOncomingTraffic -> R.drawable.give_way_to_oncoming_traffic
    Sign.Type.HorizontalArrowAdditional -> R.drawable.horizontal_arrow_additional
    Sign.Type.Hospital -> R.drawable.hospital
    Sign.Type.JunctionWithAMinorSideroad -> R.drawable.junction_with_a_minor_side_road
    Sign.Type.LeftArrowAdditional -> R.drawable.left_arrow_additional
    Sign.Type.LevelCrossingWithBarriersAhead -> R.drawable.level_crossing_with_barriers_ahead
    Sign.Type.LevelCrossingWithoutBarriersAhead -> R.drawable.level_crossing_without_barriers_ahead
    Sign.Type.LimitedAccessRoad -> R.drawable.limited_access_road
    Sign.Type.LooseRoadSurface -> R.drawable.loose_road_surface
    Sign.Type.LowFlyingAircraftLeft -> R.drawable.low_flying_aircraft_left
    Sign.Type.LowFlyingAircraftRight -> R.drawable.low_flying_aircraft_right
    Sign.Type.MaximumHeight -> R.drawable.maximum_height
    Sign.Type.MaximumSpeedLimit10 -> R.drawable.maximum_speed_limit_10
    Sign.Type.MaximumSpeedLimit20 -> R.drawable.maximum_speed_limit_20
    Sign.Type.MaximumSpeedLimit30 -> R.drawable.maximum_speed_limit_30
    Sign.Type.MaximumSpeedLimit40 -> R.drawable.maximum_speed_limit_40
    Sign.Type.MaximumSpeedLimit50 -> R.drawable.maximum_speed_limit_50
    Sign.Type.MaximumSpeedLimit60 -> R.drawable.maximum_speed_limit_60
    Sign.Type.MaximumSpeedLimit70 -> R.drawable.maximum_speed_limit_70
    Sign.Type.MaximumSpeedLimit80 -> R.drawable.maximum_speed_limit_80
    Sign.Type.MaximumSpeedLimit90 -> R.drawable.maximum_speed_limit_90
    Sign.Type.MaximumSpeedLimit100 -> R.drawable.maximum_speed_limit_100
    Sign.Type.MaximumSpeedLimit110 -> R.drawable.maximum_speed_limit_110
    Sign.Type.MaximumSpeedLimit120 -> R.drawable.maximum_speed_limit_120
    Sign.Type.MaximumSpeedLimit130 -> R.drawable.maximum_speed_limit_130
    Sign.Type.MaximumWeight -> R.drawable.maximum_weight
    Sign.Type.MaximumWeightPerAxle -> R.drawable.maximum_weight_per_axle
    Sign.Type.MaximumWidth -> R.drawable.maximum_width
    Sign.Type.MergingTraffic -> R.drawable.merging_traffic
    Sign.Type.Motorway -> R.drawable.motorway
    Sign.Type.NoBicycles -> R.drawable.no_bicycles
    Sign.Type.NoBuses -> R.drawable.no_buses
    Sign.Type.NoEntryForVehicularTraffic -> R.drawable.no_entry_for_vehicular_traffic
    Sign.Type.NoHeavyGoodsVehicles -> R.drawable.no_heavy_goods_vehicles
    Sign.Type.NoHornsOrExcessiveMotorNoise -> R.drawable.no_horns_or_excessive_motor_noise
    Sign.Type.NoLeftTurn -> R.drawable.no_left_turn
    Sign.Type.NoMotorVehiclesExceptMotorcycles -> R.drawable.no_motor_vehicles_except_motorcycles
    Sign.Type.NoMotorcycles -> R.drawable.no_motorcycles
    Sign.Type.NoOvertaking -> R.drawable.no_overtaking
    Sign.Type.NoOvertakingByHeavyGoodsVehicles -> R.drawable.no_overtaking_by_heavy_goods_vehicles
    Sign.Type.NoParkingOrWaiting -> R.drawable.no_parking_or_waiting
    Sign.Type.NoPedestrians -> R.drawable.no_pedestrians
    Sign.Type.NoRightTurn -> R.drawable.no_right_turn
    Sign.Type.NoStopping -> R.drawable.no_stopping
    Sign.Type.NoThroughRoad -> R.drawable.no_through_road
    Sign.Type.NoTractors -> R.drawable.no_tractors
    Sign.Type.NoUTurns -> R.drawable.no_uturns
    Sign.Type.NoVehiclesCarryingDangerousGoods -> R.drawable.no_vehicles_carrying_dangerous_goods
    Sign.Type.NoVehiclesCarryingDangerousWaterPollutants -> R.drawable.no_vehicles_carrying_dangerous_water_pollutants
    Sign.Type.NoVehiclesCarryingExplosives -> R.drawable.no_vehicles_carrying_explosives
    Sign.Type.OneWayStreet -> R.drawable.one_way_street
    Sign.Type.OpeningOrSwingBridge -> R.drawable.opening_or_swing_bridge
    Sign.Type.OtherDanger -> R.drawable.other_danger
    Sign.Type.ParkingPlace -> R.drawable.parking_place
    Sign.Type.PassOnEitherSide -> R.drawable.pass_on_either_side
    Sign.Type.PassOnLeftSide -> R.drawable.pass_on_left_side
    Sign.Type.PassOnRightSide -> R.drawable.pass_on_right_side
    Sign.Type.PedestrianCrossing -> R.drawable.pedestrian_crossing
    Sign.Type.PedestrianCrossingAhead -> R.drawable.pedestrian_crossing_ahead
    Sign.Type.PedestriansOnly -> R.drawable.pedestrians_only
    Sign.Type.PriorityOverOnComingVehicles -> R.drawable.priority_over_oncoming_vehicles
    Sign.Type.PriorityRoad -> R.drawable.priority_road
    Sign.Type.ProceedStraight -> R.drawable.proceed_straight
    Sign.Type.ProceedStraightOrTurnLeft -> R.drawable.proceed_straight_or_turn_left
    Sign.Type.ProceedStraightOrTurnRight -> R.drawable.proceed_straight_or_turn_right
    Sign.Type.RainAdditional -> R.drawable.rain_additional
    Sign.Type.ResidentialArea -> R.drawable.residential_area
    Sign.Type.RightArrowAdditional -> R.drawable.right_arrow_additional
    Sign.Type.RoadBump -> R.drawable.road_bump
    Sign.Type.RoadBumpAhead -> R.drawable.road_bump_ahead
    Sign.Type.RoadClosedToAllVehiclesInBothDirections -> R.drawable.road_closed_to_all_vehicles_in_both_directions
    Sign.Type.RoadNarrowsOnBothSides -> R.drawable.road_narrows_on_both_sides
    Sign.Type.RoadNarrowsOnLeft -> R.drawable.road_narrows_on_left
    Sign.Type.RoadNarrowsOnRight -> R.drawable.road_narrows_on_right
    Sign.Type.RoadworksAhead -> R.drawable.roadworks_ahead
    Sign.Type.Roundabout -> R.drawable.roundabout
    Sign.Type.RoundaboutAhead -> R.drawable.roundabout_ahead
    Sign.Type.SharedUsePath -> R.drawable.shared_use_path
    Sign.Type.SlipperyRoadSurface -> R.drawable.slippery_road_surface
    Sign.Type.SnowAdditional -> R.drawable.snow_additional
    Sign.Type.SoftVergesOrDangerousShoulder -> R.drawable.soft_verges_or_dangerous_shoulder
    Sign.Type.SteepAscent -> R.drawable.steep_ascent
    Sign.Type.SteepDescent -> R.drawable.steep_descent
    Sign.Type.Stop -> R.drawable.stop
    Sign.Type.TowAdditional -> R.drawable.tow_additional
    Sign.Type.TrafficQueuesLikelyAhead -> R.drawable.traffic_queues_likely_ahead
    Sign.Type.TrafficSignalsAhead -> R.drawable.traffic_signals_ahead
    Sign.Type.TramAdditional -> R.drawable.tram_additional
    Sign.Type.TramCrossing -> R.drawable.tram_crossing
    Sign.Type.TripleLaneEnds -> R.drawable.triple_lane_ends
    Sign.Type.TruckAdditional -> R.drawable.truck_additional
    Sign.Type.Tunnel -> R.drawable.tunnel
    Sign.Type.TunnelAhead -> R.drawable.tunnel_ahead
    Sign.Type.TurnLeft -> R.drawable.turn_left
    Sign.Type.TurnLeftAhead -> R.drawable.turn_left_ahead
    Sign.Type.TurnRight -> R.drawable.turn_right
    Sign.Type.TurnRightAhead -> R.drawable.turn_right_ahead
    Sign.Type.TwoWayTrafficAhead -> R.drawable.two_way_traffic_ahead
    Sign.Type.UnevenRoadAhead -> R.drawable.uneven_road_ahead
    Sign.Type.UnprotectedQuaysideOrRiverbank -> R.drawable.unprotected_quayside_or_riverbank
    Sign.Type.UpArrowAdditional -> R.drawable.up_arrow_additional
    Sign.Type.VerticalArrowAdditional -> R.drawable.vertical_arrow_additional
    Sign.Type.WildAnimals -> R.drawable.wild_animals
    Sign.Type.CrossedMaximumSpeedLimit -> R.drawable.crossed_maximum_speed_limit
    Sign.Type.DynamicMaximumSpeedLimit20 -> R.drawable.dynamic_maximum_speed_limit_20
    Sign.Type.DynamicMaximumSpeedLimit30 -> R.drawable.dynamic_maximum_speed_limit_30
    Sign.Type.DynamicMaximumSpeedLimit40 -> R.drawable.dynamic_maximum_speed_limit_40
    Sign.Type.DynamicMaximumSpeedLimit50 -> R.drawable.dynamic_maximum_speed_limit_50
    Sign.Type.DynamicMaximumSpeedLimit60 -> R.drawable.dynamic_maximum_speed_limit_60
    Sign.Type.DynamicMaximumSpeedLimit70 -> R.drawable.dynamic_maximum_speed_limit_70
    Sign.Type.DynamicMaximumSpeedLimit80 -> R.drawable.dynamic_maximum_speed_limit_80
    Sign.Type.DynamicMaximumSpeedLimit90 -> R.drawable.dynamic_maximum_speed_limit_90
    Sign.Type.DynamicMaximumSpeedLimit100 -> R.drawable.dynamic_maximum_speed_limit_100
    Sign.Type.DynamicMaximumSpeedLimit110 -> R.drawable.dynamic_maximum_speed_limit_110
    Sign.Type.DynamicMaximumSpeedLimit120 -> R.drawable.dynamic_maximum_speed_limit_120
    Sign.Type.DynamicMaximumSpeedLimit130 -> R.drawable.dynamic_maximum_speed_limit_130
}

@DrawableRes
fun Int.speedLimitDrawableId(): Int? = when (this) {
    10 -> R.drawable.maximum_speed_limit_10
    20 -> R.drawable.maximum_speed_limit_20
    30 -> R.drawable.maximum_speed_limit_30
    40 -> R.drawable.maximum_speed_limit_40
    50 -> R.drawable.maximum_speed_limit_50
    60 -> R.drawable.maximum_speed_limit_60
    70 -> R.drawable.maximum_speed_limit_70
    80 -> R.drawable.maximum_speed_limit_80
    90 -> R.drawable.maximum_speed_limit_90
    100 -> R.drawable.maximum_speed_limit_100
    110 -> R.drawable.maximum_speed_limit_110
    120 -> R.drawable.maximum_speed_limit_120
    130 -> R.drawable.maximum_speed_limit_130
    else -> null
}

fun Float.format(digits: Int): String = java.lang.String.format("%.${digits}f", this)

fun <T> MutableList<T>.removeLast(count: Int) {
    val removeCount = min(size, count)
    repeat(removeCount) {
        removeAt(size - 1)
    }
}

/**
 * Launches a new coroutine and repeats `block` every time the Fragment's viewLifecycleOwner
 * is in and out of `minActiveState` lifecycle state.
 */
inline fun Fragment.launchAndRepeatWithViewLifecycle(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline block: suspend CoroutineScope.() -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.lifecycle.repeatOnLifecycle(minActiveState) {
            block()
        }
    }
}

fun Float.mpsToKmh() = this * 3.6f

@SuppressLint("MissingPermission")
fun FusedLocationProviderClient.accurateLocationFlow() = callbackFlow<Location> {
    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            for (location in result.locations) {
                try {
                    trySend(location)
                } catch (t: Throwable) {
                    // Location couldn't be sent to the flow
                }
            }
        }
    }

    val locationRequest = LocationRequest.create().apply {
        interval = 1000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    requestLocationUpdates(
        locationRequest,
        callback,
        Looper.getMainLooper()
    ).addOnFailureListener { e ->
        close(e) // in case of error, close the Flow
    }

    awaitClose {
        removeLocationUpdates(callback)
    }
}

fun Fragment.launchOnViewLifecycle(block: suspend () -> Unit) =
    viewLifecycleOwner.lifecycleScope.launch {
        block()
    }