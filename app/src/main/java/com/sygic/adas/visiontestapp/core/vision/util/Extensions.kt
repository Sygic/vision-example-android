package com.sygic.adas.visiontestapp.core.vision.util

import com.sygic.adas.vision.Vision
import com.sygic.adas.vision.objects.VisionObject
import com.sygic.adas.vision.objects.VisionObjectsInfo
import com.sygic.adas.visiontestapp.core.vision.model.VisionObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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
