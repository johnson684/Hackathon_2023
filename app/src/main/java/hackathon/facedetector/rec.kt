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
    fun floatToPixel(value: Float): Int{
        val density = resources.displayMetrics.density
//        return (value * 1 / density + 0.5f).toInt()
        return value.toInt()
    }

    fun centerX(): Int{
        return floatToPixel((this.left + this.right)/2)
    }
    fun centerY(): Int{
        return floatToPixel((this.top + this.bottom)/2)
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint.color = Color.BLUE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 12f




        canvas.drawRect(left, top, right, bottom, paint)
    }
}