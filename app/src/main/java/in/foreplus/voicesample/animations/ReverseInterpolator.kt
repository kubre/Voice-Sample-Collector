package `in`.foreplus.voicesample.animations

import android.view.animation.Interpolator
import kotlin.math.abs

class ReverseInterpolator : Interpolator {
    override fun getInterpolation(input: Float): Float {
        return abs(input - 1f)
    }
}