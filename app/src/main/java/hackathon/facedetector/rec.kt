package hackathon.facedetector

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.ceil

open class Rec (context: Context?, attrs: AttributeSet?): View(context, attrs) {
    val paint = Paint()
    private var left = 500f
    private var top = 500f
    private var right = 700f
    private var bottom = 700f

    open fun changeLocation(location: String) {
        if(location == "Center") {
            left = 500f
            top = 500f
            right = 700f
            bottom = 700f
        }
        else if(location == "Left") {
            left = 100f
            top = 100f
            right = 300f
            bottom = 300f

        }
        else if(location == "Right") {

        }
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint.color = Color.BLUE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 12f




        canvas.drawRect(left, top, right, bottom, paint)
    }
}