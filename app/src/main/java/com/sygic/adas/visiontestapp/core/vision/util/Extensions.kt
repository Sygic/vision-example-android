package com.sygic.adas.visiontestapp.core.vision.util

import com.sygic.adas.vision.Vision
import com.sygic.adas.vision.ar_object.ArObject
import com.sygic.adas.vision.logic.SpeedLimitInfo
import com.sygic.adas.vision.logic.TailgatingInfo
import com.sygic.adas.vision.logic.VisionLogic
import com.sygic.adas.vision.objects.VisionObject
import com.sygic.adas.vision.objects.VisionObjectsInfo
import com.sygic.adas.vision.objects.VisionTextBlock
import com.sygic.adas.vision.road.Road
import com.sygic.adas.vision.road.RoadInfo
import com.sygic.adas.visiontestapp.core.vision.model.VisionObjects
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@OptIn(ExperimentalCoroutinesApi::class)
fun Vision.objectsResultFlow(): Flow<VisionObjects> = callbackFlow {
    val objectsListener = object: Vision.ObjectsListener {
        override fun onObjects(objects: Array<VisionObject>, info: VisionObjectsInfo) {
            trySend(VisionObjects(objects, info.fps))
        }
    }
    addObjectsListener(objectsListener)

    awaitClose {
        removeObjectsListener(objectsListener)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun Vision.roadFlow(): Flow<Road?> = callbackFlow {
    val roadsListener = object: Vision.ObjectsListener {
        override fun onRoad(road: Road?, info: RoadInfo) {
            trySend(road)
        }
    }
    addObjectsListener(roadsListener)

    awaitClose {
        removeObjectsListener(roadsListener)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun Vision.licensePlatesFlow(): Flow<Array<VisionTextBlock>> = callbackFlow {
    val objectsListener = object: Vision.ObjectsListener {
        override fun onLicensePlates(licensePlates: Array<VisionTextBlock>) {
            trySend(licensePlates)
        }
    }
    addObjectsListener(objectsListener)

    awaitClose {
        removeObjectsListener(objectsListener)
    }

}

@OptIn(ExperimentalCoroutinesApi::class)
fun VisionLogic.tailgatingFlow(): Flow<TailgatingInfo?> = callbackFlow {
    val listener = object: VisionLogic.Listener {
        override fun onTailgating(tailgatingInfo: TailgatingInfo?) {
            trySend(tailgatingInfo)
        }
    }
    addListener(listener)

    awaitClose {
        removeListener(listener)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun VisionLogic.speedLimitFlow(): Flow<SpeedLimitInfo> = callbackFlow {
    trySend(getCurrentSpeedLimit())

    val listener = object: VisionLogic.Listener {
        override fun onSpeedLimitChanged(speedLimitInfo: SpeedLimitInfo) {
            trySend(speedLimitInfo)
        }
    }
    addListener(listener)

    awaitClose {
        removeListener(listener)
    }
}


@OptIn(ExperimentalCoroutinesApi::class)
fun Vision.arObjectsFlow() = callbackFlow {
    val objectsListener = object: Vision.ObjectsListener {
        override fun onArObject(arObject: ArObject) {
            trySend(arObject)
        }
    }
    addObjectsListener(objectsListener)
    awaitClose {
        removeObjectsListener(objectsListener)
    }
}


