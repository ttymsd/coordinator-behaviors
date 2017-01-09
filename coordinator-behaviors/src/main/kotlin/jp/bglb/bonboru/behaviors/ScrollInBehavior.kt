package jp.bglb.bonboru.behaviors

import android.content.Context
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.CoordinatorLayout.Behavior
import android.support.v7.widget.Toolbar
import android.util.AttributeSet
import android.view.View

/**
 * Created by Tetsuya Masuda on 2016/12/23.
 */
class ScrollInBehavior<V : View>(context: Context, attrs: AttributeSet?) : Behavior<V>(context,
    attrs) {
  var initialized = false
  var peekHeight = 300
  var anchorPointY = 600
  var currentChildY = 0
  var anchorTopMargin = 0

  var toolbar: Toolbar? = null
  var title = ""

  init {
    attrs?.let {
      val googleMapLikeBehaviorParam = context.obtainStyledAttributes(it,
          R.styleable.GoogleMapLikeBehaviorParam)
      peekHeight = googleMapLikeBehaviorParam?.getDimensionPixelSize(
          R.styleable.GoogleMapLikeBehaviorParam_peekHeight, 0)!!
      anchorTopMargin = googleMapLikeBehaviorParam?.getDimensionPixelSize(
          R.styleable.GoogleMapLikeBehaviorParam_anchorPoint, 0)!!

      val scrollInBehaviorParam = context.obtainStyledAttributes(it,
          R.styleable.ScrollInBehaviorParam)
      title = scrollInBehaviorParam?.getString(R.styleable.ScrollInBehaviorParam_toolbarTitle)!!
      googleMapLikeBehaviorParam?.recycle()
      scrollInBehaviorParam?.recycle()
    }
  }

  override fun layoutDependsOn(parent: CoordinatorLayout?, child: V, dependency: View?): Boolean {
    val behavior = GoogleMapLikeBehavior.from(dependency)
    return behavior != null
  }

  override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {
    parent.onLayoutChild(child, layoutDirection)
    anchorPointY = parent.height - anchorTopMargin
    if (child is AppBarLayout) {
      (0..child.childCount - 1).map {
        child.getChildAt(it)
      }.find {
        it is Toolbar
      }.let {
        toolbar = it as Toolbar
      }
    }
    return true
  }

  override fun onDependentViewChanged(parent: CoordinatorLayout, child: V,
      dependency: View): Boolean {
    super.onDependentViewChanged(parent, child, dependency)
    val rate = (dependency.y - anchorTopMargin) / (parent.height - anchorTopMargin - peekHeight)
    currentChildY = -((child.height + child.paddingTop + child.paddingBottom + child.top + child.bottom) * (rate)).toInt()
    if (currentChildY <= 0) {
      child.y = currentChildY.toFloat()
    } else {
      child.y = 0f
      currentChildY = 0
    }

    val alphaRate = (anchorTopMargin * 2 - dependency.y) / (anchorTopMargin)
    val alpha = ((alphaRate - 1f) * 255).toInt()
//    drawable.alpha = if (alpha > 255) {
//      255
//    } else if (alpha < 0) {
//      0
//    } else {
//      alpha
//    }

    val drawable = child.background.mutate()
    val bounds = drawable.bounds
    var heightRate = (bounds.bottom * 2 - dependency.y) / (bounds.bottom) - 1f
    heightRate = if (heightRate > 1f) {
      1f
    } else if (heightRate < 0f) {
      0f
    } else {
      heightRate
    }
    if (heightRate >= 1f) {
      toolbar?.title = title
    } else {
      toolbar?.title = ""
    }
    drawable.setBounds(0, (bounds.bottom - bounds.bottom * heightRate).toInt(), bounds.right,
        bounds.bottom)
    child.background = drawable
    return true
  }
}
