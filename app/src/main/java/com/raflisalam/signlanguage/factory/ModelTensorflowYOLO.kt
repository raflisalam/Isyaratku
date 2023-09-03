package com.raflisalam.signlanguage.factory

import android.graphics.Bitmap
import android.content.Context
import android.util.Size
import org.tensorflow.lite.Interpreter
import java.io.File
import java.security.ProtectionDomain

open abstract class ModelTensorflowYOLO(
    var inputSize : Size,
    var outputSize : IntArray,
    var detectThreshold : Float,
    var IOU_threshold : Float,
    var IOU_class_duplicated_threshold : Float,
    var label_file : String,
    var model_file : String,
    var IS_INT_8 : Boolean = false,
    )
{

    abstract var tflite : Interpreter?
    abstract var associatedLabel : List<String>?
    protected var options = Interpreter.Options()

    //*Should  Suspend in herea
    abstract fun create(context : Context) : YOLOv5Creator
    abstract fun addGPUDelegate()
    abstract fun addNNApiDelegate()
    abstract fun initialModel(context : Context)
    abstract fun addThread(thread : Int)
    abstract fun detect(bitmap : Bitmap) : ArrayList<RecognitionResult>


}

