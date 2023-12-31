package com.raflisalam.signlanguage.factory

import android.content.Context
import android.graphics.*
import android.icu.lang.UCharacter
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import java.lang.Double.max


class ImageAnalyse(
    val context: Context,
    private val previewView: PreviewView,
    private val rotation: Int,
    private val yolov5TFLiteDetector: ModelTensorflowYOLO,
    private val ImageProcess: ImageProcess = ImageProcess(),
    private val graphicOverlay: GraphicOverlay,
    private val onResult: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private var onDetect: Boolean = true

    class Result(var costTime: Long, var bitmap: Bitmap)



    override fun analyze(image: ImageProxy) {

        val previewHeight = previewView.height
        val previewWidth = previewView.width
        val startTime = System.currentTimeMillis()
        val yuvBytes = arrayOfNulls<ByteArray>(3)
        val planes = image.planes
        val imageHeight = image.height
        val imageWidth = image.width

        // NOTE : HardCODED False
        graphicOverlay.setImageSourceInfo(imageWidth, imageHeight, false)
        ImageProcess.fillBytes(planes, yuvBytes)
        val yRowStride = planes[0].rowStride
        val uvRowStride = planes[1].rowStride
        val uvPixelStride = planes[1].pixelStride

        val rgbBytes = IntArray(imageHeight * imageWidth)
        ImageProcess.YUV420ToARGB8888(
            yuvBytes[0] as ByteArray,
            yuvBytes[1] as ByteArray,
            yuvBytes[2] as ByteArray,
            imageWidth,
            imageHeight,
            yRowStride,
            uvRowStride,
            uvPixelStride,
            rgbBytes
        )

        val imageBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
        imageBitmap.setPixels(rgbBytes, 0, imageWidth, 0, 0, imageWidth, imageHeight)

        val scale: Double = java.lang.Double.max(
            previewHeight / (if (rotation % 180 == 0) imageWidth else imageHeight).toDouble(),
            previewWidth / (if (rotation % 180 == 0) imageHeight else imageWidth).toDouble()
        )
        val fullScreenTransform: Matrix = ImageProcess.getTransformationMatrix(
            imageWidth, imageHeight, (scale * imageHeight).toInt(), (scale * imageWidth).toInt(),
            if (rotation % 180 == 0) 90 else 0, false
        )

        val fullImageBitmap = Bitmap.createBitmap(
            imageBitmap,
            0,
            0,
            imageWidth,
            imageHeight,
            fullScreenTransform,
            false
        )

        val cropImageBitmap =
            Bitmap.createBitmap(fullImageBitmap, 0, 0, previewWidth, previewHeight)

        val previewToModelTransform: Matrix = ImageProcess.getTransformationMatrix(
            previewWidth, previewHeight,
            yolov5TFLiteDetector.inputSize.width,
            yolov5TFLiteDetector.inputSize.height,
            0, false
        )
        val modelInputBitmap = Bitmap.createBitmap(
            cropImageBitmap, 0, 0,
            cropImageBitmap.width, cropImageBitmap.height,
            previewToModelTransform, false
        )

        val modelToPreviewTransform = Matrix()
        previewToModelTransform.invert(modelToPreviewTransform)
        if (onDetect) {
            val recognitions: ArrayList<RecognitionResult> =
                yolov5TFLiteDetector.detect(modelInputBitmap)
            val boxPaint = Paint()
            boxPaint.strokeWidth = 5F
            boxPaint.style = Paint.Style.STROKE
            boxPaint.color = Color.RED

            val textPain = Paint()
            textPain.textSize = 50F
            textPain.color = Color.RED
            textPain.style = Paint.Style.FILL

            graphicOverlay.clear()
            for (res in recognitions) {
                var location: RectF = res.getLocation()
                modelToPreviewTransform.mapRect(location, location)
                res.setLocation(location)
                graphicOverlay.add(ObjectGraphic(this.graphicOverlay, res))
            }

            recognitions.toSet().forEach {
                onResult(it.getLabelName())
            }
        }

        val endTime = System.currentTimeMillis()
        val costTime = endTime - startTime

        image.close()
    }


    private var lastPlayTime: Long = 0L

    companion object {
        private const val TIME_INTERVAL_WORD: Long = 500
        private const val TIME_INTERVAL_CLEAR: Long = 10
    }
}
