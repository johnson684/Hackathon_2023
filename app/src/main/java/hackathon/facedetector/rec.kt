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

    open fun changeLocation(location: FaceLocation) {
        if(location == FaceLocation("Center") {

        }
        else if(location == FaceLocation("Left")) {
            left = 300f
            top = 300f
            right = 500f
            bottom = 500f

        }
        else if(location == FaceLocation("Right")) {

        }
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint.color = Color.BLUE
        paint.style = Paint.Style.STROKE





        canvas.drawRect(left, top, right, bottom, paint)
    }
}