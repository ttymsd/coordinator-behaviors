package jp.bglb.bonboru.behaviors

import android.content.Context
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.util.AttributeSet
import android.view.View

/**
* Copyright (C) 2017 Tetsuya Masuda
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
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
