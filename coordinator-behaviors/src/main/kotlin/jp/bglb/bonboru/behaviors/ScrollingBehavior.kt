package jp.bglb.bonboru.behaviors

import android.content.Context
import android.support.design.widget.CoordinatorLayout
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
