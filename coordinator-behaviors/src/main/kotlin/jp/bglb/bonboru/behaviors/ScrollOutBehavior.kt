package jp.bglb.bonboru.behaviors

import android.content.Context
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.util.AttributeSet
import android.view.View

/**
 * Created by Tetsuya Masuda on 2016/12/30.
 */
class ScrollOutBehavior(context: Context?, attrs: AttributeSet?) : AppBarLayout.ScrollingViewBehavior(
    context, attrs) {

  var initialized = false
  var collapsedY = 0
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

  override fun layoutDependsOn(parent: CoordinatorLayout?, child: View?,
      dependency: View?): Boolean {
    val behavior = GoogleMapLikeBehavior.from(dependency)
    return behavior != null
  }

  override fun onLayoutChild(parent: CoordinatorLayout, child: View,
      layoutDirection: Int): Boolean {
    parent.onLayoutChild(child, layoutDirection)
    anchorPointY = parent.height - anchorTopMargin
    return true
  }

  override fun onDependentViewChanged(parent: CoordinatorLayout, child: View,
      dependency: View): Boolean {
    super.onDependentViewChanged(parent, child, dependency)
    val rate = (parent.height - dependency.y - peekHeight) / (anchorTopMargin)
    currentChildY = -((child.height + child.paddingTop + child.paddingBottom + child.top + child.bottom) * (rate)).toInt()
    if (currentChildY <= 0) {
      child.y = currentChildY.toFloat()
    } else {
      child.y = 0f
      currentChildY = 0
    }
    return true
  }
}
