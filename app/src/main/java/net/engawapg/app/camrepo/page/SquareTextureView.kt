package net.engawapg.app.camrepo.page

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView

class SquareTextureView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : TextureView(context, attrs, defStyle) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val wMode = MeasureSpec.getMode(widthMeasureSpec)
        val hMode = MeasureSpec.getMode(heightMeasureSpec)
        var wSize = MeasureSpec.getSize(widthMeasureSpec)
        var hSize = MeasureSpec.getSize(heightMeasureSpec)
        val minSize = if (wSize > hSize) hSize else wSize

        if (((wMode == MeasureSpec.AT_MOST) && (hMode != MeasureSpec.UNSPECIFIED)) ||
            ((wMode == MeasureSpec.UNSPECIFIED ) && (hMode == MeasureSpec.UNSPECIFIED))) {
            wSize = minSize
        }
        else if (wMode == MeasureSpec.UNSPECIFIED) {
            wSize = hSize
        }

        if (((hMode == MeasureSpec.AT_MOST) && (wMode != MeasureSpec.UNSPECIFIED)) ||
            ((hMode == MeasureSpec.UNSPECIFIED ) && (wMode == MeasureSpec.UNSPECIFIED))) {
            hSize = minSize
        }
        else if (hMode == MeasureSpec.UNSPECIFIED) {
            hSize = wSize
        }

        setMeasuredDimension(wSize, hSize)
        Log.d(TAG, "w ${MeasureSpec.toString(widthMeasureSpec)}, h ${MeasureSpec.toString(heightMeasureSpec)}")
    }

    companion object {
        const val TAG = "SquareTextureView"
    }
}