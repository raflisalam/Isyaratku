package com.raflisalam.signlanguage.ui

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.raflisalam.signlanguage.R
import com.raflisalam.signlanguage.databinding.ActivityHomeBinding
import com.raflisalam.signlanguage.utils.button.ButtonPressed
import java.util.*


class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var btnDetection: View
    private lateinit var btnBelajar: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        btnDetection = findViewById(R.id.btnDetection)
        btnBelajar = findViewById(R.id.btnBelajar)

        val actionBar = supportActionBar
        actionBar?.hide()

        grettingsText()
        setupButton()
    }

    private fun setupButton() {
        val button = ButtonPressed(this, btnDetection)
        btnDetection.setOnClickListener {
            button.isPressed()
            startActivity(Intent(this, FiturTranslateActivity::class.java))
            button.afterPressed()
        }

        btnBelajar.setOnClickListener {
            button.isPressed()
            val linkUrl = "https://pmpk.kemdikbud.go.id/sibi"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(linkUrl)
            startActivity(intent)
            button.afterPressed()
        }
    }

    private fun grettingsText() {
        val date = Calendar.getInstance()
        val hour = date.get(Calendar.HOUR_OF_DAY)
        if (hour in 3..10) {
            binding.textGreetings.text = "Selamat Pagi"
        } else if (hour in 10..14) {
            binding.textGreetings.text = "Selamat Siang"
        } else if (hour in 15..18) {
            binding.textGreetings.text = "Selamat Sore"
        } else {
            binding.textGreetings.text = "Selamat Malam"
        }
    }

}