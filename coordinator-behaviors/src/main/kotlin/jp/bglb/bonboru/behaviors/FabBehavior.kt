package jp.bglb.bonboru.behaviors

import android.content.Context
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.util.AttributeSet
import android.view.View

/**
 * Created by tetsuya on 2017/01/15.
 */
class FabBehavior(context: Context?, attrs: AttributeSet?) : FloatingActionButton.Behavior(context,
    attrs) {

  private val isScrollOut: Boolean
  private var parentHeight = 0

  init {
    if (attrs == null) {
      isScrollOut = false
    } else {
      val fabBehaviorParams = context?.obtainStyledAttributes(attrs,
          R.styleable.FabBehaviorParam)!!
      isScrollOut = fabBehaviorParams.getBoolean(R.styleable.FabBehaviorParam_isScrollOut, false)
    }
  }

  override fun onLayoutChild(parent: CoordinatorLayout, child: FloatingActionButton,
      layoutDirection: Int): Boolean {
    parentHeight = parent.height
    return super.onLayoutChild(parent, child, layoutDirection)
  }

  override fun layoutDependsOn(parent: CoordinatorLayout?, child: FloatingActionButton?,
      dependency: View?): Boolean {
    val behavior = BottomNavigationBehavior.from(dependency)
    return super.layoutDependsOn(parent, child, dependency) || behavior != null
  }

  override fun onDependentViewChanged(parent: CoordinatorLayout, child: FloatingActionButton,
      dependency: View): Boolean {
    if (dependency is AppBarLayout) {
      super.onDependentViewChanged(parent, child, dependency)
    } else if (hasBottomNavigation(dependency)) {
      updateFabPosition(dependency, child)
    } else {
      super.onDependentViewChanged(parent, child, dependency)
    }
    return false
  }

  private fun hasBottomNavigation(dependency: View): Boolean {
    val behavior = BottomNavigationBehavior.from(dependency)
    return behavior != null
  }

  private fun updateFabPosition(dependency: View, child: FloatingActionButton) {
    val top = dependency.y
    val layoutParams = child.layoutParams as CoordinatorLayout.LayoutParams
    val rate = if (isScrollOut) (parentHeight - top) / dependency.height else 1f
    child.y = top - (child.height + layoutParams.bottomMargin) * rate
  }
}
