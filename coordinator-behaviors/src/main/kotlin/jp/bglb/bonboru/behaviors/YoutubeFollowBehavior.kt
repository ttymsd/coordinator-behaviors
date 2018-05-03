package jp.bglb.bonboru.behaviors

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.CoordinatorLayout.Behavior
import android.support.v4.view.ViewCompat
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
class YoutubeFollowBehavior<V : View>(context: Context, attrs: AttributeSet? = null
) : Behavior<V>(context, attrs) {

  private val shrinkRate: Float
  private val mediaHeight: Float
  private val marginBottom: Int
  private val marginRight: Int

  private var shrinkContentMarginTop = 0
  private var parentHeight = 0

  init {
    if (attrs == null) {
      shrinkRate = 0.5f
      mediaHeight = 600f
      marginBottom = 0
      marginRight = 0
    } else {
      val youtubeBehaviorParams = context.obtainStyledAttributes(attrs,
          R.styleable.YoutubeLikeBehaviorParam)
      shrinkRate = youtubeBehaviorParams.getFloat(R.styleable.YoutubeLikeBehaviorParam_shrinkRate,
          0.5f)
      mediaHeight = youtubeBehaviorParams.getDimension(
          R.styleable.YoutubeLikeBehaviorParam_mediaHeight, 600f)
      marginBottom = youtubeBehaviorParams.getDimensionPixelSize(
          R.styleable.YoutubeLikeBehaviorParam_ylb_marginBottom,
          0)
      marginRight = youtubeBehaviorParams.getDimensionPixelSize(
          R.styleable.YoutubeLikeBehaviorParam_ylb_marginRight,
          0)
      youtubeBehaviorParams.recycle()
    }
  }

  override fun layoutDependsOn(parent: CoordinatorLayout?, child: V, dependency: View?): Boolean
      = YoutubeLikeBehavior.from(dependency) != null

  override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {
    parentHeight = parent.height
    shrinkContentMarginTop = Math.min(parentHeight,
        (parentHeight - mediaHeight + mediaHeight * shrinkRate / 2).toInt()) - marginBottom
    parent.onLayoutChild(child, layoutDirection)
    ViewCompat.offsetTopAndBottom(child, mediaHeight.toInt())
    val lp = child.layoutParams
    lp.height = (parentHeight - mediaHeight).toInt()
    child.layoutParams = lp
    return true
  }

  override fun onDependentViewChanged(parent: CoordinatorLayout, child: V,
      dependency: View): Boolean {
    super.onDependentViewChanged(parent, child, dependency)
    val rate = dependency.y / shrinkContentMarginTop
    child.alpha = 1f - rate
    child.y = dependency.y + mediaHeight
    return true
  }
}
