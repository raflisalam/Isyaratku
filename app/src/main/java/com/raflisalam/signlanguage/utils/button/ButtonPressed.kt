package com.raflisalam.signlanguage.utils.button

import android.content.Context
import android.view.View
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import com.raflisalam.signlanguage.R

class ButtonPressed (context: Context, view: View) {

    private val cardView: CardView = view.findViewById(R.id.cardView2)
    private val constraintLayout: ConstraintLayout = view.findViewById(R.id.constraint_layout2)

    fun isPressed() {
        constraintLayout.setBackgroundColor(cardView.resources.getColor(R.color.button_pressed))
    }

    fun afterPressed() {
        constraintLayout.setBackgroundColor(cardView.resources.getColor(R.color.button_focused))
    }
}