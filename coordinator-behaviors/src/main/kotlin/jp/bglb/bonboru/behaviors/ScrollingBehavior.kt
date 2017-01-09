package jp.bglb.bonboru.behaviors

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.util.AttributeSet
import android.view.View

/**
 * Created by Tetsuya Masuda on 2017/01/02.
 */
class ScrollingBehavior<V : View>(context: Context?, attrs: AttributeSet?) : CoordinatorLayout.Behavior<V>(
    context, attrs) {

  var initialized = false
  var peekHeight = 300
  var anchorPointY = 600
  var currentChildY = 0
  var anchorTopMargin = 0

  init {
    attrs?.let {
      val typedArray = context?.obtainStyledAttributes(it, R.styleable.GoogleMapLikeBehaviorParam)
      peekHeight = typedArray?.getDimensionPixelSize(
          R.styleable.GoogleMapLikeBehaviorParam_peekHeight, 0)!!
      anchorTopMargin = typedArray?.getDimensionPixelSize(
          R.styleable.GoogleMapLikeBehaviorParam_anchorPoint, 0)!!
      typedArray?.recycle()
    }
  }

  override fun onDependentViewChanged(parent: CoordinatorLayout, child: V,
      dependency: View): Boolean {
    val rate = (parent.height - dependency.y - peekHeight) / (anchorPointY - peekHeight)
    currentChildY = ((child.height + child.paddingTop + child.paddingBottom + child.top + child.bottom) * (1f - rate)).toInt()
    if (currentChildY <= 0) {
      child.y = currentChildY.toFloat()
    } else {
      child.y = 0f
      currentChildY = 0
    }
    return true
  }

}
