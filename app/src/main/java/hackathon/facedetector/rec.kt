package hackathon.facedetector

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.util.DisplayMetrics
import android.view.WindowManager

open class Rec (context: Context?, attrs: AttributeSet?): View(context, attrs) {
    val paint = Paint()
    private var Width = 1080
    private var Height = 2154
    private var left = Width/3
    private var top = Height/3
    private var right = Width/3*2
    private var bottom = Height/3*2

    open fun setScreenSize(width: Int, height: Int) {
        Width = width
        Height = height
    }
    open fun changeLocation(location: String) {
        val offset = 120
        if(location == "Center") {
            left = Width/3
            top = Height/3
            right = Width/3*2
            bottom = Height/3*2
        }
        else if(location == "Left_Top") {
            left = 0+offset
            top = 0+offset
            right =  Width/3+offset
            bottom = Height/3+offset
        }
        else if(location == "Top") {
            left = Width/3
            top = 0+offset
            right =  Width/3*2
            bottom = Height/3+offset
        }
        else if(location == "Right_Top") {
            left = Width/3*2-offset
            top = 0+offset
            right =  Width+-offset
            bottom = Height/3+offset
        }
        else if(location == "Left") {
            left = 0+offset
            top = Height/3
            right =  Width/3+offset
            bottom = Height/3*2
        }
        else if(location == "Right") {
            left = Width/3*2-offset
            top = Height/3
            right =  Width-offset
            bottom = Height/3*2
        }
        else if(location == "Left_Bottom") {
            left = 0+offset
            top = Height/3*2-offset
            right =  Width/3+offset
            bottom = Height-offset
        }
        else if(location == "Bottom") {
            left = Width/3
            top = Height/3*2-offset
            right =  Width/3*2
            bottom = Height-offset
        }
        else if(location == "Right_Bottom") {
            left = Width/3*2-offset
            top = Height/3*2-offset
            right =  Width-offset
            bottom = Height-offset
        }
    }

    fun centerX(): Float{
        return (this.left.toFloat() + this.right.toFloat())/2
    }
    fun centerY(): Float{
        return (this.top.toFloat() + this.bottom.toFloat())/2
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint.color = Color.BLUE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 12f

        canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
    }
}



open class MyUtilityClass(private val context: Context) {
    public fun getScreenSize(): Pair<Int, Int> {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)


        val widthPixels = displayMetrics.widthPixels
        val heightPixels = displayMetrics.heightPixels

        return Pair(widthPixels, heightPixels)
    }

}