package com.raflisalam.signlanguage.factory

import android.Manifest
import android.os.Environment
import android.util.Size
import java.io.File
import java.lang.StringBuilder

object YOLOv5Constants {

    val input_size : Size =Size(320, 320)
    val output_size : IntArray = UtilsClassifier.calculateOutputSize(input_size.width, 25) // Ubah ke 25 (sesuaikan sama banyak label sibi kamu )
    val detected_threshold : Float = 0.8f
    val IOU_threshold : Float = 0.8f
    val IOU_class_duplicated : Float = 0.8f
    val label_file : String = "label.txt" // ( Ubah kenama file labelnya )
    val model_file : String = "sibit_addedData.tflite" // Ubah kenama file tflitenya )
    //val model_file : String = "SignlanguageModel.tflite"
    val is_int_8 : Boolean = false;

    private val ABSOLUTE_PATH : String  =
        StringBuilder().apply {
            append(Environment.getDataDirectory().absolutePath)
            append("/user/")
            append("0")
            append("/com.raflisalam.signlanguage")
        }.toString()

    val arrayOfPermissions : Array<String> = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
}
