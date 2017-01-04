package jp.bglb.bonboru.behaviors

import android.content.Context
import android.support.annotation.IntDef
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.CoordinatorLayout.Behavior
import android.support.design.widget.CoordinatorLayout.LayoutParams
import android.support.v4.view.MotionEventCompat
import android.support.v4.view.NestedScrollingChild
import android.support.v4.view.VelocityTrackerCompat
import android.support.v4.view.ViewCompat
import android.support.v4.widget.ViewDragHelper
import android.support.v4.widget.ViewDragHelper.Callback
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewGroup
import java.lang.ref.WeakReference
import kotlin.annotation.AnnotationRetention.SOURCE

/**
 * Created by Tetsuya Masuda on 2016/09/14.
 */
class GoogleMapLikeBehavior<V : View>(context: Context?, attrs: AttributeSet?) : Behavior<V>(
    context, attrs) {

  companion object {
    @IntDef(STATE_DRAGGING,
        STATE_SETTLING,
        STATE_ANCHOR_POINT,
        STATE_EXPANDED,
        STATE_COLLAPSED,
        STATE_HIDDEN)
    @Retention(SOURCE)
    annotation class State

    const val STATE_DRAGGING = 1L
    const val STATE_SETTLING = 2L
    const val STATE_ANCHOR_POINT = 3L
    const val STATE_EXPANDED = 4L
    const val STATE_COLLAPSED = 5L
    const val STATE_HIDDEN = 6L

    @SuppressWarnings("unchecked")
    fun <V : View> from(view: V?): GoogleMapLikeBehavior<V>? {
      if (view == null) return null
      val params = view.layoutParams as? LayoutParams ?: throw IllegalArgumentException(
          "The view is not a child of CoordinatorLayout")
      return params.behavior as? GoogleMapLikeBehavior<V>
    }
  }

  var listener: OnBehaviorStateListener? = null
  var velocityTracker: VelocityTracker? = null
  var state = STATE_COLLAPSED

  var peekHeight: Int = 0
  var anchorPosition: Int = 0
  var activePointerId = MotionEvent.INVALID_POINTER_ID

  lateinit var viewRef: WeakReference<View>
  lateinit var nestedScrollingChildRef: WeakReference<View?>
  var skippedAnchorPoint = false
  var draggable = true
  var dragHelper: ViewDragHelper? = null
  var hideable = false
  var ignoreEvents = false
  var initialY = 0
  var touchingScrollingChild = false
  var lastNestedScrollDy = 0
  var nestedScrolled = false
  val dragCallback = DragCallback()
  var maxOffset = 0
  var minOffset = 0
  var parentHeight = 0
  var anchorTopMargin = 0

  init {
    attrs?.let {
      val typedArray = context?.obtainStyledAttributes(it, R.styleable.GoogleMapLikeBehaviorParam)
      peekHeight = typedArray?.getDimensionPixelSize(
          R.styleable.GoogleMapLikeBehaviorParam_peekHeight, 0)!!
      anchorTopMargin = typedArray?.getDimensionPixelSize(
          R.styleable.GoogleMapLikeBehaviorParam_anchorPoint, 0)!!
      draggable = typedArray?.getBoolean(
          R.styleable.GoogleMapLikeBehaviorParam_draggable, false)!!
      skippedAnchorPoint = typedArray?.getBoolean(
          R.styleable.GoogleMapLikeBehaviorParam_skipAnchorPoint, false)!!
//      hideable = typedArray?.getBoolean(
//          R.styleable.GoogleMapLikeBehaviorParam_hideable, false)!!
      typedArray?.recycle()
    }
  }

  // 委譲されるLayout処理
  override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {
    if (state != STATE_DRAGGING && state != STATE_SETTLING) {
      if (ViewCompat.getFitsSystemWindows(parent) && !ViewCompat.getFitsSystemWindows(child)) {
        ViewCompat.setFitsSystemWindows(child, true)
      }
      parent.onLayoutChild(child, layoutDirection)
    }
    parentHeight = parent.height
    minOffset = Math.max(0, parentHeight - child.height)
    maxOffset = Math.max(minOffset, parentHeight - peekHeight)
    anchorPosition = parentHeight - anchorTopMargin

    when (state) {
      STATE_ANCHOR_POINT -> {
        ViewCompat.offsetTopAndBottom(child, parentHeight - anchorPosition)
      }

      STATE_EXPANDED -> {
        ViewCompat.offsetTopAndBottom(child, minOffset)
      }

      STATE_HIDDEN -> {
        ViewCompat.offsetTopAndBottom(child, parentHeight)
      }

      STATE_COLLAPSED -> {
        ViewCompat.offsetTopAndBottom(child, maxOffset)
      }

      else -> {
        // do nothing
      }
    }

    if (dragHelper == null) {
      dragHelper = ViewDragHelper.create(parent, dragCallback)
    }
    viewRef = WeakReference(child)
    val found: View? = findScrollingChild(child)
    nestedScrollingChildRef = WeakReference(found)
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

    val action = MotionEventCompat.getActionMasked(ev)
    if (action == MotionEvent.ACTION_DOWN) {
      reset()
    }
    if (velocityTracker == null) {
      velocityTracker = VelocityTracker.obtain()
    }
    velocityTracker?.addMovement(ev)

    when (action) {
      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
        touchingScrollingChild = false
        activePointerId = MotionEvent.INVALID_POINTER_ID
        if (ignoreEvents) {
          ignoreEvents = false
          return false
        }
      }

      MotionEvent.ACTION_DOWN -> {
        val initialX = ev.x.toInt()
        initialY = ev.y.toInt()
        if (state == STATE_ANCHOR_POINT) {
          activePointerId = ev.getPointerId(ev.actionIndex)
          touchingScrollingChild = true
        } else {
          val scroll = nestedScrollingChildRef.get()
          if (scroll != null && parent.isPointInChildBounds(scroll, initialX, initialY)) {
            activePointerId = ev.getPointerId(ev.actionIndex)
            touchingScrollingChild = true
          }
        }
        ignoreEvents = activePointerId == MotionEvent.INVALID_POINTER_ID
            && !parent.isPointInChildBounds(child, initialX, initialY)
      }

      else -> {
        // do nothing
      }
    }

    if (!ignoreEvents && dragHelper?.shouldInterceptTouchEvent(ev)!!) {
      return true
    }

    val scroll = nestedScrollingChildRef.get()
    var touchSlop = 0
    dragHelper?.let {
      touchSlop = it.touchSlop
    }
    return action == MotionEvent.ACTION_MOVE
        && scroll != null
        && !ignoreEvents
        && state != STATE_DRAGGING
        && !parent.isPointInChildBounds(scroll, ev.x.toInt(), ev.y.toInt())
        && Math.abs(initialY - ev.y) > touchSlop
  }

  // interceptでtrueを返した場合に,CoordinatorLayoutBのscrollを考える
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
      if (Math.abs(initialY - ev.y) > touchSlop.toFloat()) {
        dragHelper?.captureChildView(child, ev.getPointerId(ev.actionIndex))
      }
    }
    return !ignoreEvents
  }

  override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout?, child: V,
      directTargetChild: View?, target: View?, nestedScrollAxes: Int): Boolean {
    lastNestedScrollDy = 0
    nestedScrolled = false
    return ((nestedScrollAxes and ViewCompat.SCROLL_AXIS_VERTICAL) != 0)
  }

  override fun onNestedPreScroll(coordinatorLayout: CoordinatorLayout?, child: V, target: View?,
      dx: Int, dy: Int, consumed: IntArray) {
    val scrollChild = nestedScrollingChildRef.get() ?: return
    if (target != scrollChild) {
      return
    }
    val currentTop = child.top
    val newTop = currentTop - dy
    if (dy > 0) {
      if (newTop < minOffset) {
        consumed[1] = currentTop - minOffset
        ViewCompat.offsetTopAndBottom(child, -consumed[1])
        setStateInternal(STATE_EXPANDED)
      } else {
        consumed[1] = dy
        ViewCompat.offsetTopAndBottom(child, -dy)
        setStateInternal(STATE_DRAGGING)
      }
    } else if (dy < 0) {
      if (!ViewCompat.canScrollVertically(target, -1)) {
        if (newTop <= maxOffset || hideable) {
          consumed[1] = dy
          ViewCompat.offsetTopAndBottom(child, -dy)
          setStateInternal(STATE_DRAGGING)
        } else {
          consumed[1] = currentTop - maxOffset
          ViewCompat.offsetTopAndBottom(child, -consumed[1])
          setStateInternal(STATE_COLLAPSED)
        }
      }
    }

    lastNestedScrollDy = dy
    nestedScrolled = true
  }

  override fun onStopNestedScroll(coordinatorLayout: CoordinatorLayout?, child: V, target: View?) {
    if (child.top == minOffset) {
      setStateInternal(STATE_EXPANDED)
      return
    }
    if (target != nestedScrollingChildRef.get() || !nestedScrolled) {
      return
    }
    val top: Int
    val targetState: Long
    if (lastNestedScrollDy > 0) {
      val currentTop = child.top
      if (currentTop > parentHeight - anchorPosition) {
        if (skippedAnchorPoint) {
          top = minOffset
          targetState = STATE_EXPANDED
        } else {
          top = parentHeight - anchorPosition
          targetState = STATE_ANCHOR_POINT
        }
      } else {
        top = minOffset
        targetState = STATE_EXPANDED
      }
    } else if (hideable && shouldHide(child, getYvelocity())) {
      top = parentHeight
      targetState = STATE_HIDDEN
    } else if (lastNestedScrollDy == 0) {
      val currentTop = child.top
      if (Math.abs(currentTop - minOffset) < Math.abs(currentTop - maxOffset)) {
        top = minOffset
        targetState = STATE_EXPANDED
      } else {
        if (skippedAnchorPoint) {
          top = minOffset
          targetState = STATE_EXPANDED
        } else {
          top = maxOffset
          targetState = STATE_COLLAPSED
        }
      }
    } else {
      val currentTop = child.top
      if (currentTop > parentHeight - anchorPosition) {
        top = maxOffset
        targetState = STATE_COLLAPSED
      } else {
        if (skippedAnchorPoint) {
          top = maxOffset
          targetState = STATE_COLLAPSED
        } else {
          top = parentHeight - anchorPosition
          targetState = STATE_ANCHOR_POINT
        }
      }
    }
    if (dragHelper?.smoothSlideViewTo(child, child.left, top)!!) {
      setStateInternal(STATE_SETTLING)
      ViewCompat.postOnAnimation(child, SettleRunnable(child, targetState))
    } else {
      setStateInternal(targetState)
    }
    nestedScrolled = false
  }

  override fun onNestedPreFling(coordinatorLayout: CoordinatorLayout?, child: V, target: View?,
      velocityX: Float, velocityY: Float): Boolean {
    return target == nestedScrollingChildRef.get()
        && (state != STATE_EXPANDED) ||
        super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY)
  }

  private fun getYvelocity(): Float {
    velocityTracker?.computeCurrentVelocity(1000, 2000.0f);
    return VelocityTrackerCompat.getYVelocity(velocityTracker, activePointerId);
  }

  fun updateState(value: Long) {
    if (this.state == value) {
      return
    }

    this.state = value

    val sheet = viewRef.get()
    val parent = sheet.parent
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

  /**
   * view内を再帰的に調べ、NestedScrollingChildが実装されてるViewを探す
   */
  private fun findScrollingChild(view: View): View? = when (view) {
    is NestedScrollingChild -> view

    is ViewGroup -> {
      var result: View? = null
      val group = view
      (0..group.childCount - 1)
          .map { findScrollingChild(group.getChildAt(it)) }
          .forEach { v ->
            v?.let {
              result = it
            }
          }
      result
    }

    else -> null
  }

  private fun startSettlingAnimation(child: View, @State state: Long) {
    val top: Int
    if (state == STATE_COLLAPSED) {
      top = maxOffset
    } else if (state == STATE_EXPANDED) {
      top = minOffset
    } else if (state == STATE_HIDDEN) {
      top = parentHeight
    } else if (state == STATE_ANCHOR_POINT) {
      top = parentHeight - anchorPosition
    } else {
      throw IllegalArgumentException("Illegal state argument: " + state)
    }
    setStateInternal(STATE_SETTLING)
    if (dragHelper!!.smoothSlideViewTo(child, child.left, top)) {
      ViewCompat.postOnAnimation(child, SettleRunnable(child, state))
    }
  }

  // update state
  private fun setStateInternal(@State state: Long) {
    if (this.state == state) {
      return
    }
    this.state = state
    if (!(this.state == STATE_DRAGGING || this.state == STATE_SETTLING)) {
      Log.d("AAA", "notify:$state")
      this.listener?.onBehaviorStateChanged(state)
    }
  }

  private fun dispatchOnSlide(offset: Int) {
    // TODO: notify position to listener
  }

  private fun reset() {
    activePointerId = ViewDragHelper.INVALID_POINTER
    velocityTracker?.let {
      it.recycle()
      velocityTracker = null
    }
  }

  private fun shouldHide(view: View, yvel: Float): Boolean {
    return false
  }

  interface OnBehaviorStateListener {
    fun onBehaviorStateChanged(newState: Long)
  }

  inner class SettleRunnable(val view: View, @State val state: Long) : Runnable {
    override fun run() {
      if (dragHelper != null && dragHelper?.continueSettling(true)!!) {
        ViewCompat.postOnAnimation(view, this)
      } else {
        setStateInternal(state)
      }
    }
  }

  inner class DragCallback() : Callback() {

    // 対象のviewをdrag可能にするかどうか
    override fun tryCaptureView(child: View?, pointerId: Int): Boolean {
      if (state == STATE_DRAGGING) {
        return false
      }
      if (touchingScrollingChild) {
        return false
      }
      if (state == STATE_EXPANDED && activePointerId == pointerId) {
        val scroll = nestedScrollingChildRef.get()
        if (scroll != null && ViewCompat.canScrollVertically(scroll, -1)) {
          return false
        }
      }
      return viewRef.get() != null
    }

    override fun onViewPositionChanged(changedView: View?, left: Int, top: Int, dx: Int, dy: Int) {
      dispatchOnSlide(top)
    }

    override fun onViewDragStateChanged(state: Int) {
      if (state == ViewDragHelper.STATE_DRAGGING) {
        setStateInternal(STATE_DRAGGING)
      }
    }

    override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
      @State var targetState = 0L
      var top = 0
      if (yvel < 0) {
        val currentTop = releasedChild.top
        if (Math.abs(currentTop - minOffset) < Math.abs(
            currentTop - parentHeight + anchorPosition)) {
          top = minOffset
          targetState = STATE_EXPANDED
        } else {
          top = parentHeight - anchorPosition
          targetState = STATE_ANCHOR_POINT
        }
      } else if (hideable && shouldHide(releasedChild, yvel)) {
        top = parentHeight
        targetState = STATE_HIDDEN
      } else if (yvel == 0.0f) {
        val currentTop = releasedChild.top
        if (Math.abs(currentTop - minOffset) < Math.abs(
            currentTop - parentHeight + anchorPosition)) {
          top = minOffset
          targetState = STATE_EXPANDED
        } else if (Math.abs(currentTop - parentHeight + anchorPosition) < Math.abs(
            currentTop - maxOffset)) {
          if (skippedAnchorPoint) {
            top = maxOffset
            targetState = STATE_COLLAPSED
          } else {
            top = parentHeight - anchorPosition
            targetState = STATE_ANCHOR_POINT
          }
        } else {
          top = maxOffset
          targetState = STATE_COLLAPSED
        }
      } else {
        val currentTop = releasedChild.top
        if (Math.abs(currentTop - parentHeight + anchorPosition) < Math.abs(
            currentTop - maxOffset)) {
          if (skippedAnchorPoint) {
            top = maxOffset
            targetState = STATE_COLLAPSED
          } else {
            top = parentHeight - anchorPosition
            targetState = STATE_ANCHOR_POINT
          }
        } else {
          top = maxOffset
          targetState = STATE_COLLAPSED
        }
      }
      val settleCaptureViewAt = dragHelper?.settleCapturedViewAt(releasedChild.left, top)!!
      if (settleCaptureViewAt) {
        setStateInternal(STATE_SETTLING)
        ViewCompat.postOnAnimation(releasedChild, SettleRunnable(releasedChild, targetState))
      } else {
        setStateInternal(targetState)
      }
    }

    override fun clampViewPositionVertical(child: View?, top: Int, dy: Int): Int {
      val offset = if (hideable) {
        parentHeight
      } else {
        maxOffset
      }
      return constrain(top, minOffset, offset)
    }

    override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
      return child.left
    }

    private fun constrain(amount: Int, low: Int, high: Int): Int {
      return if (amount < low) {
        low
      } else if (amount > high) {
        high
      } else {
        amount
      }
    }

    override fun getViewVerticalDragRange(child: View?): Int {
      return if (hideable) {
        parentHeight - minOffset;
      } else {
        maxOffset - minOffset;
      }
    }
  }
}
