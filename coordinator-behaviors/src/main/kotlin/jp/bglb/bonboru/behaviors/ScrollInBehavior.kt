package jp.bglb.bonboru.behaviors

import android.content.Context
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.CoordinatorLayout.Behavior
import android.support.v7.widget.Toolbar
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
class ScrollInBehavior<V : View>(context: Context, attrs: AttributeSet?) : Behavior<V>(context,
    attrs) {
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
      peekHeight = googleMapLikeBehaviorParam.getDimensionPixelSize(
          R.styleable.GoogleMapLikeBehaviorParam_peekHeight, 0)
      anchorTopMargin = googleMapLikeBehaviorParam.getDimensionPixelSize(
          R.styleable.GoogleMapLikeBehaviorParam_anchorPoint, 0)
      googleMapLikeBehaviorParam.recycle()

      val scrollInBehaviorParam = context.obtainStyledAttributes(it,
          R.styleable.ScrollInBehaviorParam)
      title = scrollInBehaviorParam?.getString(R.styleable.ScrollInBehaviorParam_toolbarTitle)!!
      scrollInBehaviorParam.recycle()
    }
  }

  override fun layoutDependsOn(parent: CoordinatorLayout?, child: V,
      dependency: View?): Boolean = GoogleMapLikeBehavior.from(dependency) != null

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
