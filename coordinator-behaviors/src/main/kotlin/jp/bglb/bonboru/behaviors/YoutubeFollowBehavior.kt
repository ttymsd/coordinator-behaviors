package jp.bglb.bonboru.behaviors

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.CoordinatorLayout.Behavior
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.View

/**
 * Created by tetsuya on 2017/01/07.
 */
class YoutubeFollowBehavior<V : View>(context: Context?, attrs: AttributeSet?) : Behavior<V>(
    context, attrs) {

  private val shrinkRate: Float
  private val mediaHeight: Float
  private var shrinkContentMarginTop = 0
  private var parentHeight = 0

  init {
    if (attrs == null) {
      shrinkRate = 0.5f
      mediaHeight = 600f
    } else {
      val youtubeBehaviorParams = context?.obtainStyledAttributes(attrs,
          R.styleable.YoutubeLikeBehaviorParam)!!
      shrinkRate = youtubeBehaviorParams.getFloat(R.styleable.YoutubeLikeBehaviorParam_shrinkRate,
          0.5f)
      mediaHeight = youtubeBehaviorParams.getDimension(
          R.styleable.YoutubeLikeBehaviorParam_mediaHeight, 600f)
    }
  }

  override fun layoutDependsOn(parent: CoordinatorLayout?, child: V, dependency: View?): Boolean {
    val behavior = YoutubeLikeBehavior.from(dependency)
    return behavior != null
  }

  override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {
    parentHeight = parent.height
    shrinkContentMarginTop = Math.min(parentHeight,
        (parentHeight - mediaHeight + mediaHeight * shrinkRate / 2).toInt())
    parent.onLayoutChild(child, layoutDirection)
    ViewCompat.offsetTopAndBottom(child, mediaHeight.toInt())
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
