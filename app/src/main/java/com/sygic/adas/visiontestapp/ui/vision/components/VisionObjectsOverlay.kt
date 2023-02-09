package com.sygic.adas.visiontestapp.ui.vision.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.sygic.adas.vision.objects.*
import com.sygic.adas.vision.road.Line
import com.sygic.adas.vision.road.Road
import kotlin.math.round

private val SIGNS_RECT_PAINT = Paint().apply {
    style = Paint.Style.STROKE
    color = Color.rgb(255, 255, 255)
    strokeWidth = 8.0f
}

private val SIGNS_TEXT_PAINT = Paint().apply {
    color = Color.rgb(255, 255, 255)
    isFakeBoldText = true
    textSize = 40f
}

private val VEHICLES_RECT_PAINT = Paint().apply {
    style = Paint.Style.STROKE
    color = Color.rgb(255, 255, 255)
    strokeWidth = 8.0f
}

private val VEHICLES_TAILGATING_RECT_PAINT = Paint(VEHICLES_RECT_PAINT).apply {
    color = Color.rgb(255, 0, 0)
}

private val VEHICLES_TEXT_PAINT = Paint().apply {
    color = Color.rgb(0, 255, 0)
    isFakeBoldText = true
    textSize = 70f
}

private val VEHICLES_TAILGATING_TEXT_PAINT = Paint(VEHICLES_TEXT_PAINT).apply {
    color = Color.rgb(255, 0, 0)
}

private val VEHICLES_LICENSE_TEXT_PAINT = Paint().apply {
    color = Color.rgb(255, 255, 255)
    textSize = 50f
}

private val ROAD_FOCUS_PAINT = Paint().apply {
    color = Color.rgb(255, 255, 0)
    strokeWidth = 8.0f
    style = Paint.Style.STROKE
    pathEffect = DashPathEffect(arrayOf(20f, 10f, 6f, 10f).toFloatArray(), 0f)
}

private val ROAD_LANE_PAINT = Paint().apply {
    color = Color.rgb(0, 255, 255)
    strokeWidth = 8.0f
    style = Paint.Style.STROKE
    pathEffect = DashPathEffect(arrayOf(15f, 6f).toFloatArray(), 0f)
}

private val ROAD_LANE_PAINT_NORMALIZED = Paint(ROAD_LANE_PAINT).apply {
    color = Color.rgb(0, 255, 0)
}

private val ROAD_LANE_PAINT_NOT_NORMALIZED = Paint(ROAD_LANE_PAINT).apply {
    color = Color.rgb(255, 0, 0)
}

private val LICENSE_PLATE_TEXT_PAINT = Paint().apply {
    typeface = Typeface.DEFAULT_BOLD
    color = Color.BLACK
    textSize = 50.0f
}

private val LICENSE_PLATE_RECT_FILL_PAINT = Paint().apply {
    style = Paint.Style.FILL
    color = Color.rgb(255, 255, 255)
}

private val LICENSE_PLATE_RECT_STROKE_PAINT = Paint().apply {
    style = Paint.Style.STROKE
    color = Color.BLACK
    strokeWidth = 3.0f
}


class VisionObjectsOverlay(context: Context, attributeSet: AttributeSet?): View(context, attributeSet) {

    private val objects = mutableListOf<VisionObject>()
    private var road: Road? = null
    private val licensePlates = mutableListOf<VisionTextBlock>()

    private var bitmap: Bitmap? = null

    private var aspectRatio: Float = 0.0f
    private var virtualImgRect: Rect? = null

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return

        objects.forEach {
            when (it) {
                is Sign -> canvas.drawSign(it)
                is Vehicle -> canvas.drawVehicle(it)
            }
        }
        road?.let { canvas.drawRoad(it) }
        tailgatingObject?.let { canvas.drawTailgatingObject(it) }
        licensePlates.forEach { canvas.drawLicensePlate(it) }
        //bitmap?.let { canvas.drawBitmap(it, 0f, 0f, null)}
    }

    fun setImageAspectRatio(ratio: Float) {
        if(ratio == aspectRatio)
            return

        aspectRatio = ratio
        val overlayRatio = width.toFloat() / height.toFloat()

        virtualImgRect = if(overlayRatio > ratio) {
            val newHeight = width.toFloat() / ratio
            val yShift: Int = round((newHeight - height.toFloat()) / 2f).toInt()
            Rect(0, -yShift, width, height + yShift)
        }
        else {
            val newWidth = height.toFloat() * ratio
            val xShift: Int = round((newWidth - width.toFloat()) / 2f).toInt()
            Rect(-xShift, 0, width + xShift, height)
        }
    }

    fun drawObjects(objects: Array<VisionObject>) {
        this.objects.clear()
        if(objects.isNotEmpty()) {
            this.objects.addAll(objects)
        }

        // don't call invalidate, otherwise tailgating object will be drawn twice
    }

    fun drawRoad(road: Road?) {
        this.road = road
        invalidate()
    }

    fun drawLicensePlates(licensePlates: Array<VisionTextBlock>) {
        this.licensePlates.clear()
        this.licensePlates.addAll(licensePlates)
        invalidate()
    }

    fun drawBitmap(bitmap: Bitmap?) {
        this.bitmap = bitmap
        invalidate()
    }

    var tailgatingObject: VisionObject? = null
        set(value) {
            field = value
            if(value != null) {
                objects.find { it.boundary == value.boundary }?.let {
                    objects.remove(it)
                }
            }
            invalidate()
        }

    private fun Canvas.drawSign(sign: Sign) {
        val rect = sign.boundary.toScreen()
        drawRect(rect, SIGNS_RECT_PAINT)
        drawText("[${sign.confidence.toInt()}][${sign.signConfidence.toInt()}] ${sign.signType}", rect.left, rect.bottom + 40f, SIGNS_TEXT_PAINT)
    }

    private fun Canvas.drawVehicle(vehicle: Vehicle) {
        val rect = vehicle.boundary.toScreen()
        drawRect(rect, VEHICLES_RECT_PAINT)
        if(vehicle.collides)
            drawText( "${round(vehicle.distance).toInt()} m", rect.left + 6, rect.top - 6, VEHICLES_TEXT_PAINT)

        drawText(vehicle.licensePlate.text, rect.left + 10, rect.bottom - 4, VEHICLES_LICENSE_TEXT_PAINT)
    }

    private fun Canvas.drawTailgatingObject(visionObject: VisionObject) {
        val rect = visionObject.boundary.toScreen()
        drawRect(rect, VEHICLES_TAILGATING_RECT_PAINT)
        drawText( "${round(visionObject.distance).toInt()} m", rect.left + 6, rect.top - 6, VEHICLES_TAILGATING_TEXT_PAINT)

        if(visionObject is Vehicle)
            drawText(visionObject.licensePlate.text, rect.left + 10, rect.bottom - 4, VEHICLES_LICENSE_TEXT_PAINT)
    }

    private fun Canvas.drawRoad(road: Road) {
        drawVisionLine(road.focusMid, ROAD_FOCUS_PAINT)
        drawVisionLine(road.focusLeft, ROAD_FOCUS_PAINT)
        drawVisionLine(road.focusRight, ROAD_FOCUS_PAINT)
        road.lanes.forEach {
            drawVisionLine(it.line, if(it.isNormalized) ROAD_LANE_PAINT_NORMALIZED else ROAD_LANE_PAINT_NOT_NORMALIZED)
        }
    }

    private fun Canvas.drawVisionLine(line: Line, paint: Paint) = with(line.toScreen()) {
        drawLine(pointA.x, pointA.y, pointB.x, pointB.y, paint)
    }

    private fun Canvas.drawLicensePlate(textBlock: VisionTextBlock) {
        val rect = textBlock.boundary.toScreen()
        val center = PointF(rect.centerX(), rect.centerY())
        val licensePlateRect = RectF(
            center.x - 60f,
            center.y - 50f,
            center.x + 210f,
            center.y + 50f
        )
        drawRect(licensePlateRect, LICENSE_PLATE_RECT_FILL_PAINT)
        drawRect(licensePlateRect, LICENSE_PLATE_RECT_STROKE_PAINT)
        drawText(textBlock.text, center.x - 55, center.y + 15, LICENSE_PLATE_TEXT_PAINT)
    }

    private fun PointF.toScreen() = PointF(this.x * measuredWidth.toFloat(), this.y * measuredHeight.toFloat())
    private fun Line.toScreen() = Line(pointA.toScreen(), pointB.toScreen())
    private fun Boundary.toScreen(): RectF {
        if(virtualImgRect == null) {
            virtualImgRect = Rect(0, 0, width, height)
        }

        val rect = virtualImgRect as Rect

        val fwidth = rect.width().toFloat()
        val fheight = rect.height().toFloat()
        val ftop = rect.top.toFloat()
        val fleft = rect.left.toFloat()

        return RectF(
            fleft + xMin * fwidth,
            ftop + yMin * fheight,
            fleft + xMax * fwidth,
            ftop + yMax * fheight
        )
    }

}