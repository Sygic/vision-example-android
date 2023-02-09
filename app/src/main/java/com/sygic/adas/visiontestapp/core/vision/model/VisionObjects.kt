package com.sygic.adas.visiontestapp.core.vision.model

import com.sygic.adas.vision.objects.VisionObject

class VisionObjects(
    val objects: Array<VisionObject>,
    val fps: Int
)