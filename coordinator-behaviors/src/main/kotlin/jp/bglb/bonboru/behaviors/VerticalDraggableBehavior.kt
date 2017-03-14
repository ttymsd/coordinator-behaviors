package jp.bglb.bonboru.behaviors

import android.content.Context
import android.support.annotation.IntDef
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
import java.lang.ref.WeakReference
import kotlin.annotation.AnnotationRetention.SOURCE

/**
 * Created by tetsuya on 2017/01/05.
 */
class VerticalDraggableBehavior<V : View>(context: Context?,
    attrs: AttributeSet?) : CoordinatorLayout.Behavior<V>(context, attrs) {

  companion object {
    @IntDef(STATE_DRAGGING,
        STATE_SETTLING,
        STATE_HIDE_TOP,
        STATE_EXPANDED, // parent top
        STATE_COLLAPSED, // parent bottom
        STATE_HIDE_BOTTOM)
    @Retention(SOURCE)
    annotation class State

    const val STATE_DRAGGING = 1L
    const val STATE_SETTLING = 2L
    const val STATE_HIDE_TOP = 3L
    const val STATE_EXPANDED = 4L
    const val STATE_COLLAPSED = 5L
    const val STATE_HIDE_BOTTOM = 6L

    @SuppressWarnings("unchecked")
    fun <V : View> from(view: V?): VerticalDraggableBehavior<V>? {
      if (view == null) return null
      val params = view.layoutParams as? LayoutParams ?: throw IllegalArgumentException(
          "The view is not a child of CoordinatorLayout")
      return params.behavior as? VerticalDraggableBehavior<V>
    }
  }

  var listener: OnBehaviorStateListener? = null
  var velocityTracker: VelocityTracker? = null
  private lateinit var viewRef: WeakReference<View>
  private val dragCallback = DragCallback()
  private var dragHelper: ViewDragHelper? = null

  @State var state = STATE_COLLAPSED
  private var activePointerId = MotionEvent.INVALID_POINTER_ID
  private var ignoreEvents = false
  private var draggable = true
  private var initialY = 0

  private var parentHeight = 0
  private var peekHeight = 0
  private var anchorTopMargin = 0
  private var skippedAnchorPoint = false

  private var minOffset = 0
  private var maxOffset = 0
  private var anchorPosition = 0

  init {
    attrs?.let {
      val typedArray = context?.obtainStyledAttributes(it, R.styleable.GoogleMapLikeBehaviorParam)
      peekHeight = typedArray?.getDimensionPixelSize(
          R.styleable.GoogleMapLikeBehaviorParam_peekHeight, 0)!!
      anchorTopMargin = typedArray.getDimensionPixelSize(
          R.styleable.GoogleMapLikeBehaviorParam_anchorPoint, 0)
      draggable = typedArray.getBoolean(
          R.styleable.GoogleMapLikeBehaviorParam_draggable, false)
      skippedAnchorPoint = typedArray.getBoolean(
          R.styleable.GoogleMapLikeBehaviorParam_skipAnchorPoint, false)
//      hideable = typedArray?.getBoolean(
//          R.styleable.GoogleMapLikeBehaviorParam_hideable, false)!!
      typedArray.recycle()
    }
  }

  fun updateState(@State value: Long) {
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

    parentHeight = parent.height
    minOffset = Math.max(0, parentHeight - child.height)
    maxOffset = Math.max(minOffset, parentHeight - child.height)
    anchorPosition = parentHeight - anchorTopMargin

    when (state) {
      STATE_HIDE_TOP -> {
        ViewCompat.offsetTopAndBottom(child, -child.height)
      }

      STATE_EXPANDED -> {
        ViewCompat.offsetTopAndBottom(child, 0)
      }

      STATE_COLLAPSED -> {
        ViewCompat.offsetTopAndBottom(child, maxOffset)
      }

      STATE_HIDE_BOTTOM -> {
        ViewCompat.offsetTopAndBottom(child, parentHeight)
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
        activePointerId = MotionEvent.INVALID_POINTER_ID
        if (ignoreEvents) {
          ignoreEvents = false
          return false
        }
      }

      MotionEvent.ACTION_DOWN -> {
        initialY = ev.y.toInt()
        ignoreEvents = activePointerId == MotionEvent.INVALID_POINTER_ID
            && !parent.isPointInChildBounds(child, 0, initialY)
      }

      else -> {
        // do nothing
      }
    }

    if (!ignoreEvents && dragHelper?.shouldInterceptTouchEvent(ev)!!) {
      return true
    }

    var touchSlop = 0
    dragHelper?.let {
      touchSlop = it.touchSlop
    }

    return action == MotionEvent.ACTION_MOVE
        && !ignoreEvents
        && state != STATE_DRAGGING
        && Math.abs(initialY - ev.y) > touchSlop
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
      if (Math.abs(initialY - ev.y) > touchSlop.toFloat()) {
        dragHelper?.captureChildView(child, ev.getPointerId(ev.actionIndex))
      }
    }
    return !ignoreEvents
  }

  private fun setStateInternal(@State state: Long) {
    if (this.state == state) {
      return
    }
    this.state = state
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

  private fun startSettlingAnimation(child: View, @State state: Long) {
    val top = if (state == STATE_HIDE_TOP) {
      -child.height
    } else if (state == STATE_EXPANDED) {
      0
    } else if (state == STATE_COLLAPSED) {
      parentHeight - child.height
    } else {
      throw IllegalArgumentException("Illegal state argument: " + state)
    }
    setStateInternal(STATE_SETTLING)
    if (dragHelper!!.smoothSlideViewTo(child, child.left, top)) {
      ViewCompat.postOnAnimation(child, SettleRunnable(child, state))
    }
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

  inner class DragCallback : Callback() {

    override fun tryCaptureView(child: View?, pointerId: Int): Boolean {
      if (from(child) == null) {
        return false
      }
      if (state == STATE_DRAGGING) {
        return false
      }
      return viewRef.get() != null
    }

    override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
      dispatchOnSlide(top)
    }

    override fun onViewDragStateChanged(state: Int) {
      val currentState = this@VerticalDraggableBehavior.state
      if (state == ViewDragHelper.STATE_DRAGGING) {
        if (currentState == STATE_DRAGGING) {
          setStateInternal(STATE_DRAGGING)
        }
      }
    }

    /**
     * --- 0
     * 1
     * --- anchorMarginTop/2
     * 2
     * --- anchorMarginTop
     * 3
     *
     * --
     * 4
     * ■■■ View
     * --- ParentHeight
     * 5
     */
    override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
      @State var targetState = state
      val currentTop = releasedChild.top
      var top = currentTop
      if (yvel >= 0) {
        if (currentTop < 0) {
          top = -releasedChild.height
          targetState = STATE_HIDE_TOP
        } else if (Math.abs(currentTop) < anchorTopMargin) {
          top = 0
          targetState = STATE_EXPANDED
        } else if (Math.abs(currentTop) < maxOffset) {
          top = maxOffset
          targetState = STATE_COLLAPSED
        } else {
          top = parentHeight
          targetState = STATE_HIDE_BOTTOM
        }
      } else {
        if (Math.abs(currentTop) < anchorTopMargin) {
          top = 0
          targetState = STATE_EXPANDED
        } else if (Math.abs(currentTop) < maxOffset) {
          top = maxOffset
          targetState = STATE_COLLAPSED
        } else if (Math.abs(currentTop) >= parentHeight - releasedChild.height) {
          top = parentHeight
          targetState = STATE_HIDE_BOTTOM
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

    override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int
        = constrain(top, -child.height, parentHeight)

    override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int = child.left

    private fun dispatchOnSlide(offset: Int) {
      // TODO: notify position to listener
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
  }

  interface OnBehaviorStateListener {
    fun onBehaviorStateChanged(newState: Long)
  }
}
