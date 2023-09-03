package com.raflisalam.signlanguage

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.camera.view.PreviewView
import androidx.core.view.isGone
import com.raflisalam.signlanguage.databinding.ActivityMainBinding
import com.raflisalam.signlanguage.factory.CameraProcess
import com.raflisalam.signlanguage.factory.ImageAnalyse
import com.raflisalam.signlanguage.factory.ModelTensorflowYOLO

class MainActivity : AppCompatActivity() {

    private var _binding : ActivityMainBinding ?= null
    private val binding : ActivityMainBinding get ()= _binding!!


    private val viewModel : MainActivityViewModel by viewModels()
    public lateinit var  cameraPreviewView: PreviewView
    lateinit var cameraProcess : CameraProcess
    public lateinit var fullImageAnalyse: ImageAnalyse


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initiate The Preparation
        viewModel.initModel(baseContext)
        viewModel.isLoading.observe(this@MainActivity) {
            binding.progressBar.isGone = !it;
        }
        cameraProcess = CameraProcess()
        cameraPreviewView = binding.cameraPreviewWrap
        if(!cameraProcess.allPermissionGranted(this@MainActivity)) {
            cameraProcess.requestPermission(this@MainActivity)
        }

    }

    override fun onResume() {
        super.onResume()
            viewModel.yoloV5Model.observe(this@MainActivity) {
                fullImageAnalyse = ImageAnalyse(
                    this,
                    cameraPreviewView,
                    this.windowManager.defaultDisplay.rotation,
                    it,
                    graphicOverlay = binding.graphicOverlay,
                    onResult = {label ->
                        Log.d("TAG",label.toString())
                    }
                )
                cameraProcess.startCamera(this, fullImageAnalyse, cameraPreviewView)
            }

    }
    private fun initGraphicListenerHandler()
    {

    }




}