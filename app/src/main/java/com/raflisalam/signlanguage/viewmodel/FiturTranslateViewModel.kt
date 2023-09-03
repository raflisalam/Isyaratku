package com.raflisalam.signlanguage.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raflisalam.signlanguage.utils.factory.ModelTensorflowYOLO
import com.raflisalam.signlanguage.utils.factory.YOLOv5Constants
import com.raflisalam.signlanguage.utils.factory.YOLOv5Creator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FiturTranslateViewModel : ViewModel() {

    private val _yoloV5Model : MutableLiveData<ModelTensorflowYOLO> = MutableLiveData()
    val yoloV5Model : LiveData<ModelTensorflowYOLO> = _yoloV5Model

    private val _isLoading : MutableLiveData<Boolean> = MutableLiveData(false)
    val  isLoading : LiveData<Boolean> = _isLoading

    fun initModel(context : Context)
    {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO)
        {
            try {
                val model = YOLOv5Creator(
                    YOLOv5Constants.input_size,
                    YOLOv5Constants.output_size,
                    YOLOv5Constants.detected_threshold,
                    YOLOv5Constants.IOU_threshold,
                    YOLOv5Constants.IOU_class_duplicated,
                    YOLOv5Constants.label_file,
                    YOLOv5Constants.model_file,
                    YOLOv5Constants.is_int_8
                ).create(context).apply {
                    addGPUDelegate()
                    initialModel(context)
                }
                _yoloV5Model.postValue(model)
                _isLoading.postValue(false)
            }
            catch (e : Exception) {
                e.printStackTrace()
            }
        }
    }
}