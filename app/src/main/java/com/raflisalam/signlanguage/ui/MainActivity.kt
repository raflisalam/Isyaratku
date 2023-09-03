package com.raflisalam.signlanguage.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.viewModels
import androidx.camera.view.PreviewView
import androidx.core.view.isGone
import com.raflisalam.signlanguage.databinding.ActivityMainBinding
import com.raflisalam.signlanguage.utils.factory.CameraProcess
import com.raflisalam.signlanguage.utils.factory.ImageAnalyse
import java.util.*

class MainActivity : AppCompatActivity() {

    private var _binding : ActivityMainBinding ?= null
    private val binding : ActivityMainBinding get ()= _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()
        Handler().postDelayed({
            val intent = Intent(this@MainActivity, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)

    }

}


