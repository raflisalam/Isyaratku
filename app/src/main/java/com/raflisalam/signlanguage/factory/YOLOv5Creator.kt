package com.raflisalam.signlanguage.factory

import android.graphics.RectF
import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.os.Build
import android.provider.ContactsContract
import android.util.Size
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.common.ops.QuantizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.lang.Float.max
import java.lang.Float.min
import java.util.*
import kotlin.collections.ArrayList

class YOLOv5Creator(
    input_size : Size,
    output_size :IntArray,
    detected_threshold : Float,
    IOU_THRESHOLD : Float,
    IOU_class_duplicated : Float,
    label_file : String,
    model_file : String,
    is_int_8 : Boolean
) : ModelTensorflowYOLO(
    input_size,
    output_size,
    detected_threshold,
    IOU_THRESHOLD,
    IOU_class_duplicated,
    label_file,
    model_file,
    is_int_8
) {
    override var tflite: Interpreter? = null
    override var associatedLabel: List<String>? = null

    override fun create(context: Context): YOLOv5Creator {
        initialModel(context)
        return this
    }

    override fun addGPUDelegate() {
    }

    override fun addNNApiDelegate() {
        val nnApiDelegate : NnApiDelegate
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        {
            val nnApiOptions : NnApiDelegate.Options = NnApiDelegate.Options()
            nnApiOptions.allowFp16 = true
            nnApiOptions.executionPreference = NnApiDelegate.Options.EXECUTION_PREFERENCE_SUSTAINED_SPEED
            nnApiDelegate = NnApiDelegate(nnApiOptions)
            options.addDelegate(nnApiDelegate)
        }

    }

    override fun initialModel(context: Context) {
        val tfliteByteBuffer = FileUtil.loadMappedFile(context, model_file)
        tflite = Interpreter(tfliteByteBuffer, options)
        associatedLabel = FileUtil.loadLabels(context,label_file)
    }

    override fun addThread(thread: Int) {
        options.numThreads = thread
    }

    override fun detect(bitmap: Bitmap): ArrayList<RecognitionResult> {

        var yoloTfliteInput : TensorImage
        val imageProcessor : ImageProcessor
        val probabilityBuffer : TensorBuffer
        if(!IS_INT_8)
        {
            imageProcessor  = ImageProcessor.Builder()
                .add(ResizeOp(inputSize.height,inputSize.width, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f,255f))
                .build()
            yoloTfliteInput = TensorImage(DataType.FLOAT32)

            probabilityBuffer = TensorBuffer.createFixedSize(outputSize, DataType.FLOAT32)
        }
        else
        {
            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(inputSize.height, inputSize.width, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f,255f))
                .add(
                    QuantizeOp(
                        0f,1f
                    )
                )
                .add(CastOp(DataType.UINT8))
                .build()
            yoloTfliteInput = TensorImage(DataType.UINT8)

            probabilityBuffer = TensorBuffer.createFixedSize(outputSize, DataType.UINT8)
        }

        yoloTfliteInput.load(bitmap)
        yoloTfliteInput = imageProcessor.process(yoloTfliteInput)

        tflite?.run(yoloTfliteInput.buffer, probabilityBuffer.buffer)

        if(IS_INT_8)
        {

        }

        val allRecognitions = doRecognition(probabilityBuffer)

        val nmsRecognitions = nmsAllClass(nonMaxSuppression(allRecognitions))

        for(recognition in nmsRecognitions)
        {
            val labelId = recognition.getLabelId()
            val labelName = associatedLabel?.get(labelId)
            if(labelName != null )
            {
                recognition.setLabelName(labelName)
            }
        }

        return nmsRecognitions
    }

    private fun doRecognition(probabilityBuffer : TensorBuffer) : ArrayList<RecognitionResult>
    {
        var recognitionArray = probabilityBuffer.floatArray
        val allRecognitions : ArrayList<RecognitionResult> = ArrayList<RecognitionResult>()

        /*
        https://stackoverflow.com/questions/56115874/how-to-convert-bounding-box-x1-y1-x2-y2-to-yolo-style-x-y-w-h
        pada dasarnya kita konversi bbox dari YOLO  format ke bbox yang biasanya
         */
        for(i in 0 until outputSize[1])
        {
            val gridStride = i * outputSize[2]

            val x = recognitionArray[0 + gridStride] * inputSize.width
            val y = recognitionArray[1 + gridStride] * inputSize.height

            val w = recognitionArray[2 + gridStride] * inputSize.width
            val h = recognitionArray[3 + gridStride] * inputSize.height

            // Convert YOLO  Format BBOX To Normal Normal BBOX
            val xMin : Float = Math.max(0.0, x - w / 2.0).toFloat()
            val yMin : Float = Math.max(0.0, y - h/ 2.0).toFloat()
            val yMax : Float = Math.min(inputSize.height.toDouble(), y + h / 2.0).toFloat()
            val xMax : Float = Math.min(inputSize.width.toDouble(), x + w / 2.0 ).toFloat()

            val confidence = recognitionArray[4 + gridStride]

            val classScores = Arrays.copyOfRange(recognitionArray, 5 + gridStride, outputSize[2] + gridStride)

            var labelId = 0
            var  maxLabelScores = 0f
            for (j in classScores.indices)
            {
                if(classScores[j] > maxLabelScores)
                {
                    maxLabelScores = classScores[j]
                    labelId = j
                }
            }
            var r = RecognitionResult(
                labelId,
                labelName = "",
                maxLabelScores,
                confidence,
                RectF(xMin,yMin,xMax,yMax)
            )
            allRecognitions.add(r)
        }
        return allRecognitions
    }
    private fun nonMaxSuppression(allRecognitions : ArrayList<RecognitionResult>) : ArrayList<RecognitionResult>
    {
        val nmsRecognitions : ArrayList<RecognitionResult> = ArrayList()

        for(label in 0 until outputSize[2] - 5)
        {
            val pq : PriorityQueue<RecognitionResult> = PriorityQueue(
                outputSize[1]
            ) { l, r ->
                java.lang.Float.compare(l.getConfidence(), r.getConfidence())
            }

            for (j in allRecognitions.indices)
            {
                if(allRecognitions[j].getLabelId() == label && allRecognitions[j].getConfidence() > this.detectThreshold)
                {
                    pq.add(allRecognitions[j])
                }
            }

            while (pq.size > 0)
            {
                val a : Array<RecognitionResult?> = arrayOfNulls<RecognitionResult>(pq.size)
                val detections : Array<RecognitionResult> = pq.toArray(a)
                val max : RecognitionResult = detections[0]
                nmsRecognitions.add(max)
                pq.clear()

                for( k in 1 until detections.size)
                {
                    val detection : RecognitionResult = detections[k]
                    if(boxIou(max.getLocation(), detection.getLocation()) < this.IOU_threshold)
                    {
                        pq.add(detection)
                    }
                }
            }
        }

        return nmsRecognitions
    }

    private fun nmsAllClass(allRecognitions : ArrayList<RecognitionResult>) : ArrayList<RecognitionResult>
    {
        val nmsRecognitions : ArrayList<RecognitionResult> = ArrayList<RecognitionResult>()
        val pq : PriorityQueue<RecognitionResult> = PriorityQueue(100) { l, r->
            r.getConfidence().compareTo(l.getConfidence())
        }

        for (j in allRecognitions.indices)
        {
            if(allRecognitions[j].getConfidence() > this.detectThreshold) {
                pq.add(allRecognitions[j])
            }
        }

        while(pq.size > 0)
        {
            val a : Array<RecognitionResult?> = arrayOfNulls(pq.size)
            val detections  : Array<RecognitionResult> = pq.toArray(a)
            val max : RecognitionResult = detections[0]
            nmsRecognitions.add(max)
            pq.clear()
            for (k in 1 until detections.size) {
                val detection: RecognitionResult = detections[k]
                val iou = boxIou(max.getLocation(), detection.getLocation())
                if (iou < this.IOU_class_duplicated_threshold) pq.add(detection)
            }
        }
        return nmsRecognitions
    }

    private fun boxIou(a : RectF, b : RectF) : Float{
        val intersection = boxIntersection(a,b)
        val union = boxUnion(a,b)
        return if(union <= 0 ) 1f else intersection / union
    }

    private fun boxIntersection(a : RectF, b :RectF) : Float
    {
        val maxLeft : Float= max(a.left, b.left)
        val maxTop : Float = max(a.top, b.top)
        val minRight : Float = min(a.right, b.right)
        val minBottom : Float = min(a.bottom, b.bottom)

        val w = minRight - maxLeft
        val h = minBottom - maxTop

        return if(w < 0 || h < 0) 0f else w*h
    }

    private fun boxUnion(a : RectF, b : RectF) : Float
    {
        val intersection = boxIntersection(a,b)

        val areaA = (a.bottom - a.top) *(a.right - a.left)
        val areaB = (b.bottom - b.top) * (b.right - b.left)
        return areaA + areaB - intersection

    }
}


