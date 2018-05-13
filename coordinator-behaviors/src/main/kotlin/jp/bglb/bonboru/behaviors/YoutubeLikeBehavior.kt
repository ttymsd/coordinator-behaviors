package jp.bglb.bonboru.behaviors

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.support.annotation.IntDef
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.CoordinatorLayout.LayoutParams
import android.support.v4.view.MotionEventCompat
import android.support.v4.view.ViewCompat
import android.support.v4.widget.ViewDragHelper
import android.support.v4.widget.ViewDragHelper.Callback
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import jp.bglb.bonboru.behaviors.BottomNavigationBehavior.Companion.ANIMATION_DURATION
import jp.bglb.bonboru.behaviors.BottomNavigationBehavior.Companion.SCROLL_DOWN
import jp.bglb.bonboru.behaviors.BottomNavigationBehavior.Companion.SCROLL_UP
import java.lang.ref.WeakReference
import kotlin.annotation.AnnotationRetention.SOURCE

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
class YoutubeLikeBehavior<V : View>(context: Context,
  attrs: AttributeSet? = null) : CoordinatorLayout.Behavior<V>(context, attrs) {

  companion object {
    @IntDef(STATE_DRAGGING,
      STATE_SETTLING,
      STATE_EXPANDED,
      STATE_SHRINK,
      STATE_TO_LEFT,
      STATE_TO_RIGHT,
      STATE_HIDDEN,
      STATE_SHRINK_DRAGGING)
    @Retention(SOURCE)
    annotation class State

    const val STATE_DRAGGING = 1
    const val STATE_SETTLING = 2
    const val STATE_EXPANDED = 3
    const val STATE_SHRINK = 4
    const val STATE_TO_LEFT = 5
    const val STATE_TO_RIGHT = 6
    const val STATE_HIDDEN = 7
    const val STATE_SHRINK_DRAGGING = 8

    const val REMOVE_THRETHOLD = 30 * 3

    @SuppressWarnings("unchecked")
    fun <V : View> from(view: V?): YoutubeLikeBehavior<V>? {
      if (view == null) return null
      val params = view.layoutParams as? LayoutParams ?: throw IllegalArgumentException(
        "The view is not a child of CoordinatorLayout")
      return params.behavior as? YoutubeLikeBehavior<V>
    }
  }

  var listener: OnBehaviorStateListener? = null
  var velocityTracker: VelocityTracker? = null
  private lateinit var viewRef: WeakReference<View>
  private val dragCallback = DragCallback()
  private var dragHelper: ViewDragHelper? = null

  @State var state = STATE_EXPANDED
  private var activePointerId = MotionEvent.INVALID_POINTER_ID
  private var ignoreEvents = false
  private var draggable = true
  private var initialX = 0
  private var initialY = 0

  private val shrinkRate: Float
  private val marginBottom: Int
  private val marginRight: Int

  private var parentHeight = 0
  private var parentWidth = 0
  private var leftMargin = 0
  private var shrinkMarginTop = 0
  private var bottomNavigationHeight = 0

  init {
    if (attrs == null) {
      shrinkRate = 0.5f
      marginBottom = 0
      marginRight = 0
    } else {
      val youtubeBehaviorParams = context.obtainStyledAttributes(attrs,
        R.styleable.YoutubeLikeBehaviorParam)
      shrinkRate = youtubeBehaviorParams.getFloat(R.styleable.YoutubeLikeBehaviorParam_shrinkRate,
        0.5f)
      marginBottom = youtubeBehaviorParams.getDimensionPixelSize(
        R.styleable.YoutubeLikeBehaviorParam_ylb_marginBottom,
        0)
      marginRight = youtubeBehaviorParams.getDimensionPixelSize(
        R.styleable.YoutubeLikeBehaviorParam_ylb_marginRight,
        0)
      state = youtubeBehaviorParams.getInt(R.styleable.YoutubeLikeBehaviorParam_start_state,
        STATE_EXPANDED)
      youtubeBehaviorParams.recycle()
    }
  }

  fun updateState(@State value: Int) {
    if (this.state == value) {
      return
    }

    this.state = value

    val sheet = viewRef.get()
    val parent = sheet?.parent
    parent?.let {
      if (it.isLayoutRequested && ViewCompat.isAttachedToWindow(sheet)) {
        sheet.post {
          startSettlingAnimation(sheet, state)
        }
      } else {
        startSettlingAnimation(sheet, state)
      }
    }
  }

  override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {
    if (state != STATE_DRAGGING && state != STATE_SETTLING) {
      if (ViewCompat.getFitsSystemWindows(parent) && !ViewCompat.getFitsSystemWindows(child)) {
        ViewCompat.setFitsSystemWindows(child, true)
      }
      parent.onLayoutChild(child, layoutDirection)
    }

    (0 until parent.childCount).forEach {
      val view = parent.getChildAt(it)
      if (view is BottomNavigationView) {
        bottomNavigationHeight = view.height
      }
    }

    scrollOutAnimator = ObjectAnimator.ofFloat(child, "translationY", 0f,
      bottomNavigationHeight.toFloat()).apply {
      duration = ANIMATION_DURATION
      interpolator = AccelerateDecelerateInterpolator()
      addListener(animationListener)
    }
    scrollInAnimator = ObjectAnimator.ofFloat(child, "translationY",
      bottomNavigationHeight.toFloat(), 0f).apply {
      duration = ANIMATION_DURATION
      interpolator = AccelerateDecelerateInterpolator()
      addListener(animationListener)
    }

    parentHeight = parent.height - bottomNavigationHeight
    parentWidth = parent.width
    val halfChildHeight = child.height / 2f
    shrinkMarginTop = Math.min(parentHeight,
      (parentHeight - (halfChildHeight * (1 + shrinkRate))).toInt()) - marginBottom
    // current(2017/01/11) support media width is "screen width" only
    leftMargin = Math.min(parentWidth, (child.width / 4f).toInt()) - marginRight

    when (state) {
      STATE_EXPANDED -> {
        ViewCompat.offsetTopAndBottom(child, 0)
      }

      STATE_SHRINK -> {
        ViewCompat.setScaleX(child, shrinkRate)
        ViewCompat.setScaleY(child, shrinkRate)
        child.translationX = (leftMargin * 2f * (1f - shrinkRate))
        ViewCompat.offsetTopAndBottom(child, shrinkMarginTop)
      }

      STATE_TO_LEFT -> {
        ViewCompat.setScaleX(child, shrinkRate)
        ViewCompat.setScaleY(child, shrinkRate)
        ViewCompat.offsetTopAndBottom(child, shrinkMarginTop)
      }

      STATE_TO_RIGHT -> {
        ViewCompat.setScaleX(child, shrinkRate)
        ViewCompat.setScaleY(child, shrinkRate)
        ViewCompat.offsetTopAndBottom(child, shrinkMarginTop)
      }

      else -> {
        // do nothing
      }
    }

    if (dragHelper == null) {
      dragHelper = ViewDragHelper.create(parent, dragCallback)
    }
    viewRef = WeakReference(child)
    return true
  }

  override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: V,
    ev: MotionEvent): Boolean {
    if (!draggable) {
      return false
    }
    if (!child.isShown) {
      return false
    }
    val action = ev.action
    if (action == MotionEvent.ACTION_DOWN) {
      reset()
    }
    if (velocityTracker == null) {
      velocityTracker = VelocityTracker.obtain()
    }

    when (action) {
      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
        activePointerId = MotionEvent.INVALID_POINTER_ID
        if (ignoreEvents) {
          ignoreEvents = false
          return false
        }
      }

      MotionEvent.ACTION_DOWN -> {
        initialX = ev.x.toInt()
        initialY = ev.y.toInt()
        ignoreEvents = activePointerId == MotionEvent.INVALID_POINTER_ID
          && !parent.isPointInChildBounds(child, initialX, initialY)
      }

      else -> {
        // do nothing
      }
    }
    velocityTracker?.addMovement(ev)

    if (!ignoreEvents && dragHelper?.shouldInterceptTouchEvent(ev)!!) {
      return true
    }

    var touchSlop = 0
    dragHelper?.let {
      touchSlop = it.touchSlop
    }

    return action == MotionEvent.ACTION_MOVE
      && !ignoreEvents
      && (state != STATE_DRAGGING || state != STATE_SHRINK_DRAGGING)
      && (Math.abs(initialX - ev.x) > touchSlop || Math.abs(initialY - ev.y) > touchSlop)
  }

  override fun onTouchEvent(parent: CoordinatorLayout, child: V, ev: MotionEvent): Boolean {
    if (!draggable) {
      return false
    }
    if (!child.isShown) {
      return false
    }
    val action = MotionEventCompat.getActionMasked(ev)
    if (state == STATE_DRAGGING && action == MotionEvent.ACTION_DOWN) {
      return true
    }

    dragHelper?.processTouchEvent(ev)
    if (action == MotionEvent.ACTION_DOWN) {
      reset()
    }
    if (velocityTracker == null) {
      velocityTracker = VelocityTracker.obtain()
    }
    velocityTracker?.addMovement(ev)

    if (action == MotionEvent.ACTION_MOVE && !ignoreEvents) {
      var touchSlop = 0
      dragHelper?.let {
        touchSlop = it.touchSlop
      }
      if (Math.abs(initialX - ev.x) > touchSlop.toFloat()
        || Math.abs(initialY - ev.y) > touchSlop.toFloat()) {
        dragHelper?.captureChildView(child, ev.getPointerId(ev.actionIndex))
      }
    }
    return !ignoreEvents
  }

  private var animating = false
  private var animatingDirection = 0

  private val animationListener = object : AnimatorListenerAdapter() {
    override fun onAnimationStart(animation: Animator?) {
      animating = true
    }

    override fun onAnimationCancel(animation: Animator?) {
      animating = true
    }

    override fun onAnimationEnd(animation: Animator?) {
      animating = false
    }
  }

  private lateinit var scrollOutAnimator: ObjectAnimator
  private lateinit var scrollInAnimator: ObjectAnimator

  override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout, child: V,
    directTargetChild: View, target: View, nestedScrollAxes: Int, type: Int): Boolean {
    return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL
  }

  override fun onNestedPreScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View,
    dx: Int, dy: Int, consumed: IntArray, type: Int) {
    super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
    if (animating) return

    val direction = if (dy >= 0) SCROLL_DOWN else SCROLL_UP
    if (animatingDirection == direction) return

    animatingDirection = direction
    if (direction == SCROLL_DOWN) {
      scrollOutAnimation()
    } else {
      scrollInAnimation()
    }
  }

  private fun scrollOutAnimation() {
    // TODO: BottomNavigationのいちに依存させる
    scrollOutAnimator.start()
  }

  private fun scrollInAnimation() {
    scrollInAnimator.start()
  }

  private fun setStateInternal(@State state: Int) {
    if (this.state == state) {
      return
    }
    this.state = state
    if (state == STATE_EXPANDED) {
      viewRef.get()?.let {
        it.translationY = 0f
      }
    } else if (state == STATE_SHRINK) {
      if (animatingDirection == SCROLL_DOWN) {
        viewRef.get()?.let {
          it.translationY = bottomNavigationHeight.toFloat()
        }
      }
    }
    if (!(this.state == STATE_DRAGGING || this.state == STATE_SETTLING)) {
      this.listener?.onBehaviorStateChanged(state)
    }
  }

  private fun reset() {
    activePointerId = ViewDragHelper.INVALID_POINTER
    velocityTracker?.let {
      it.recycle()
      velocityTracker = null
    }
  }

  private fun startSettlingAnimation(child: View, @State state: Int) {
    val top: Int
    val left: Int
    if (state == STATE_EXPANDED) {
      top = 0
      left = 0
    } else if (state == STATE_SHRINK) {
      top = shrinkMarginTop
      left = 0
    } else {
      throw IllegalArgumentException("Illegal state argument: " + state)
    }
    setStateInternal(STATE_SETTLING)
    if (dragHelper!!.smoothSlideViewTo(child, left, top)) {
      ViewCompat.postOnAnimation(child, SettleRunnable(child, state))
    }
  }

  inner class SettleRunnable(val view: View, @State val state: Int) : Runnable {
    override fun run() {
      if (dragHelper != null && dragHelper?.continueSettling(true)!!) {
        ViewCompat.postOnAnimation(view, this)
      } else {
        setStateInternal(state)
      }
    }
  }

  inner class DragCallback : Callback() {
    override fun tryCaptureView(child: View, pointerId: Int): Boolean {
      if (YoutubeLikeBehavior.from(child) == null) {
        return false
      }
      if (state == STATE_DRAGGING) {
        return false
      }
      return viewRef.get() != null
    }

    override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
      dispatchOnSlide(top)
      val rate = (shrinkRate - 1) / shrinkMarginTop * top.toFloat() + 1
      ViewCompat.setScaleX(changedView, rate)
      ViewCompat.setScaleY(changedView, rate)
      changedView.translationX = (leftMargin * 2f * (1f - rate))
    }

    override fun onViewDragStateChanged(state: Int) {
      val currentState = this@YoutubeLikeBehavior.state
      if (state == ViewDragHelper.STATE_DRAGGING) {
        if (currentState == STATE_EXPANDED || currentState == STATE_DRAGGING) {
          setStateInternal(STATE_DRAGGING)
        } else if (currentState == STATE_SHRINK_DRAGGING || currentState == STATE_SHRINK) {
          setStateInternal(STATE_SHRINK_DRAGGING)
        }
      }
    }

    override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
      @State var targetState = state
      val currentTop = releasedChild.top
      val currentLeft = releasedChild.left
      var top = currentTop
      var left = currentLeft
      if (state == STATE_DRAGGING) {
        if (Math.abs(currentTop) < Math.abs(parentHeight / 2f)) {
          top = 0
          targetState = STATE_EXPANDED
        } else {
          top = shrinkMarginTop
          targetState = STATE_SHRINK
        }
      } else if (state == STATE_SHRINK_DRAGGING) {
        if (currentLeft < -REMOVE_THRETHOLD) {
          left = -parentWidth
          targetState = STATE_TO_LEFT
        } else if (REMOVE_THRETHOLD < currentLeft) {
          left = parentWidth
          targetState = STATE_TO_RIGHT
        } else {
          left = 0
          targetState = STATE_SHRINK
        }
      }

      val settleCaptureViewAt = dragHelper?.settleCapturedViewAt(left, top)!!
      if (settleCaptureViewAt) {
        setStateInternal(STATE_SETTLING)
        ViewCompat.postOnAnimation(releasedChild, SettleRunnable(releasedChild, targetState))
      } else {
        setStateInternal(targetState)
      }
    }

    override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
      return if (state == STATE_SHRINK_DRAGGING || state == STATE_SHRINK) {
        if (Math.abs(shrinkMarginTop - top) >= 10) {
          setStateInternal(STATE_DRAGGING)
          constrain(top, 0, shrinkMarginTop)
        } else {
          shrinkMarginTop
        }
      } else {
        constrain(top, 0, shrinkMarginTop)
      }
    }

    override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
      return if (state == STATE_SHRINK_DRAGGING || state == STATE_SHRINK) {
        constrain(left, -parentWidth, parentWidth)
      } else {
        0
      }
    }

    private fun dispatchOnSlide(offset: Int) {
      // TODO: notify position to listener
    }

    private fun constrain(amount: Int, low: Int, high: Int): Int = if (amount < low) {
      low
    } else if (amount > high) {
      high
    } else {
      amount
    }
  }

  interface OnBehaviorStateListener {
    fun onBehaviorStateChanged(@State newState: Int)
  }
}
