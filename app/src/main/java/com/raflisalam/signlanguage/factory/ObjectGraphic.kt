package com.raflisalam.signlanguage.factory
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Color
import android.graphics.Canvas
import java.util.*
import java.util.concurrent.BlockingDeque


class ObjectGraphic (

    private var graphic : GraphicOverlay, private var objs : RecognitionResult) : GraphicOverlay.Graphic(graphic)
{
    private var boxPaints = Array(NUM_COLORS){Paint()}
    private var textPaints = Array(NUM_COLORS){Paint()}
    private var labelPaints = Array(NUM_COLORS){Paint()}

    init {

        for(i in 0 until NUM_COLORS)
        {
            textPaints[i] = Paint()
            boxPaints[i] = Paint()
            labelPaints[i] = Paint();

            // Define Text options
            textPaints[i].color = COLORS[i][0]
            textPaints[i].textSize = TEXT_SIZE


            //define Bbox Paints
            boxPaints[i].color = COLORS[i][1]
            boxPaints[i].textSize = TEXT_SIZE
            boxPaints[i].style= Paint.Style.STROKE
            boxPaints[i].strokeWidth = STROKE_WIDTH

            // Label Paints
            labelPaints[i].color = COLORS[i][1]
            labelPaints[i].textSize = TEXT_SIZE
            labelPaints[i].style = Paint.Style.FILL
        }
    }


    override fun draw(canvas: Canvas?) {
        val colorID = Math.abs(objs.getLabelId() % NUM_COLORS)

        var textWidth = textPaints[colorID].measureText("ID : " + objs.getLabelId())
        val lineHeight = TEXT_SIZE + STROKE_WIDTH
        var yLabelOffset = -lineHeight // Ini kek buat space setiap Content

        textWidth = Math.max(textWidth, textPaints[colorID].measureText(
            String.format(Locale.US, LABEL_FORMAT, objs.getConfidence() * 100, 1)
        ))

        yLabelOffset -= 2*lineHeight

        val rect = RectF(objs.getLocation())
        canvas?.drawRect(rect, boxPaints[colorID])

        canvas?.drawRect(
            rect.left - STROKE_WIDTH,
            rect.top + yLabelOffset,
            rect.left + textWidth + 2 * STROKE_WIDTH,
            rect.top,
            labelPaints[colorID]
        )

        yLabelOffset += lineHeight

        canvas?.drawText(
            "Label: " + objs.getLabelName(),
            rect.left,rect.top + yLabelOffset,
            textPaints[colorID]
        )

        yLabelOffset += lineHeight
        canvas?.drawText(
            String.format(
                Locale.US,
                LABEL_FORMAT,
                objs.getConfidence() * 100
            ),rect.left, rect.top + yLabelOffset,
            textPaints[colorID]
        )

        yLabelOffset+= lineHeight
    }

    companion object {

        private const val TEXT_SIZE : Float = 24.0f;
        private const val STROKE_WIDTH : Float = 4.0f;
        private const val NUM_COLORS : Int = 10

        private val COLORS = arrayOf(
            intArrayOf(Color.BLACK, Color.WHITE),
            intArrayOf(Color.WHITE, Color.MAGENTA),
            intArrayOf(Color.BLACK, Color.LTGRAY),
            intArrayOf(Color.WHITE, Color.RED),
            intArrayOf(Color.WHITE, Color.BLUE),
            intArrayOf(Color.WHITE, Color.DKGRAY),
            intArrayOf(Color.BLACK, Color.CYAN),
            intArrayOf(Color.BLACK, Color.YELLOW),
            intArrayOf(Color.WHITE, Color.BLACK),
            intArrayOf(Color.BLACK, Color.GREEN)
        )

        private const val LABEL_FORMAT ="%.2f%% Confidence "

    }

}