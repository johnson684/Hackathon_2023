package hackathon
import kotlin.math.sqrt

fun CalculateDistance(FacePosX: Float,FacePosY: Float, GoalPosX: Float, GoalPosY: Float):Float{
    val xDiff = FacePosX - GoalPosX
    val yDiff = FacePosY - GoalPosY
    val distance = sqrt(xDiff * xDiff + yDiff * yDiff)
    return distance
}