package jp.bglb.bonboru.behaviors

import android.content.Context
import android.support.annotation.IntDef
import android.support.design.widget.CoordinatorLayout
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
 * Created by tetsuya on 2017/01/20.
 */
class PanelBehavior<V : View>(context: Context?,
    attrs: AttributeSet?) : CoordinatorLayout.Behavior<V>(context, attrs) {

  companion object {
    @IntDef(STATE_DRAGGING,
        STATE_SETTLING,
        STATE_EXPANDED,
        STATE_COLLAPSED)
    @Retention(SOURCE)
    annotation class State

    const val STATE_SETTLING = 0L
    const val STATE_EXPANDED = 1L
    const val STATE_DRAGGING = 2L
    const val STATE_COLLAPSED = 3L
    const val STATE_COLLAPSED_DRAGGING = 4L
    const val STATE_SLIDED_TO_RIGHT = 5L
    const val STATE_SLIDED_TO_LEFT = 6L

    @SuppressWarnings("unchecked")
    fun <V : View> from(view: V?): PanelBehavior<V>? {
      if (view == null) return null
      val params = view.layoutParams as? CoordinatorLayout.LayoutParams ?: throw IllegalArgumentException(
          "The view is not a child of CoordinatorLayout")
      return params.behavior as? PanelBehavior<V>
    }
  }


  var listener: OnPositionChangedListener? = null
  var velocityTracker: VelocityTracker? = null
  private lateinit var viewRef: WeakReference<View>
  private val dragCallback = DragCallback()
  private var dragHelper: ViewDragHelper? = null

  @State var state = STATE_COLLAPSED
  private var activePointerId = MotionEvent.INVALID_POINTER_ID
  private var ignoreEvents = false
  private var draggable = true
  private var initialX = 0
  private var initialY = 0

  private val shrinkRate: Float
  private val peekHeight: Int
  var position: Position = Position.Center

  private var parentHeight = 0
  private var parentWidth = 0
  private var minOffset = 0

  init {
    shrinkRate = 0.8f
    if (attrs == null) {
      peekHeight = 200 * 3
    } else {
      val panelBehaviorParam = context?.obtainStyledAttributes(attrs,
          R.styleable.PanelBehaviorParam)!!
      peekHeight = panelBehaviorParam.getDimensionPixelSize(
          R.styleable.PanelBehaviorParam_pbm_peekHeight, 200 * 3)!!
      val index = panelBehaviorParam.getInt(R.styleable.PanelBehaviorParam_pbm_position, 0)!!
      position = when (index) {
        0 -> Position.Left
        1 -> Position.Center
        2 -> Position.Right
        else -> Position.Center
      }
      panelBehaviorParam.recycle()
    }
  }

  fun updateState(@State value: Long) {
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

  override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {
    if (state != STATE_DRAGGING && state != STATE_SETTLING) {
      if (ViewCompat.getFitsSystemWindows(parent) && !ViewCompat.getFitsSystemWindows(child)) {
        ViewCompat.setFitsSystemWindows(child, true)
      }
      parent.onLayoutChild(child, layoutDirection)
    }

    parentHeight = parent.height
    parentWidth = parent.width
    minOffset = parentHeight - peekHeight

    when (state) {
      STATE_COLLAPSED -> {
        ViewCompat.setScaleX(child, shrinkRate)
        ViewCompat.offsetTopAndBottom(child, minOffset)
      }

      STATE_EXPANDED -> {
        ViewCompat.setScaleX(child, 1f)
        ViewCompat.offsetTopAndBottom(child, 0)
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

  override fun layoutDependsOn(parent: CoordinatorLayout?, child: V, dependency: View?): Boolean {
    val behavior = from(child)
    val dependencyBehavior = from(dependency)
    val depend = if (behavior != null && dependencyBehavior != null) {
      dependencyBehavior.position == Position.Center
          && (behavior.position == Position.Left || behavior.position == Position.Right)
    } else {
      false
    }
    return depend
  }

  override fun onDependentViewChanged(parent: CoordinatorLayout, child: V,
      dependency: View): Boolean {
    child.x = when (position) {
      Position.Left -> (dependency.left - parentWidth).toFloat()

      Position.Right -> dependency.right.toFloat()

      else -> 0f
    }
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
        initialX = ev.x.toInt()
        initialY = ev.y.toInt()
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

    var touchSlop = 0
    dragHelper?.let {
      touchSlop = it.touchSlop
    }

    return action == MotionEvent.ACTION_MOVE
        && !ignoreEvents
        && state != STATE_DRAGGING
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

  private fun setStateInternal(view: View, @State state: Long) {
    if (this.state == state) {
      return
    }
    this.state = state
    if ((this.state == STATE_SLIDED_TO_LEFT || this.state == STATE_SLIDED_TO_RIGHT)) {
      this.listener?.onPositionChanged(view, state)
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
    val top: Int
    val left: Int
    if (state == STATE_COLLAPSED) {
      top = minOffset
      left = 0
    } else if (state == STATE_EXPANDED) {
      top = 0
      left = 0
    } else {
      throw IllegalArgumentException("Illegal state argument: " + state)
    }
    setStateInternal(child, STATE_SETTLING)
    if (dragHelper!!.smoothSlideViewTo(child, left, top)) {
      ViewCompat.postOnAnimation(child, SettleRunnable(child, state))
    }
  }

  inner class SettleRunnable(val view: View, @State val state: Long) : Runnable {
    override fun run() {
      if (dragHelper != null && dragHelper?.continueSettling(true)!!) {
        ViewCompat.postOnAnimation(view, this)
      } else {
        setStateInternal(view, state)
      }
    }
  }

  inner class DragCallback : Callback() {

    override fun tryCaptureView(child: View?, pointerId: Int): Boolean {
      if (YoutubeLikeBehavior.from(child) == null) {
        return false
      }
      if (state == STATE_DRAGGING) {
        return false
      }
      return viewRef.get() != null
    }

    override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
      val scale = (shrinkRate - 1f) * top / minOffset + 1f
      ViewCompat.setScaleX(changedView, scale)
    }

    override fun onViewDragStateChanged(state: Int) {
      val currentState = this@PanelBehavior.state
      if (state == ViewDragHelper.STATE_DRAGGING) {
        if (currentState == STATE_EXPANDED || currentState == STATE_DRAGGING) {
          setStateInternal(viewRef.get(), STATE_DRAGGING)
        } else if (currentState == STATE_COLLAPSED || currentState == STATE_COLLAPSED_DRAGGING) {
          setStateInternal(viewRef.get(), STATE_COLLAPSED_DRAGGING)
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
        if (yvel > 0) {
          if (Math.abs(currentTop) < minOffset * 0.1f) {
            top = 0
            targetState = STATE_EXPANDED
          } else {
            top = minOffset
            targetState = STATE_COLLAPSED
          }
        } else {
          if (Math.abs(currentTop) > minOffset * 0.9f) {
            top = minOffset
            targetState = STATE_COLLAPSED
          } else {
            top = 0
            targetState = STATE_EXPANDED
          }
        }
      } else if (state == STATE_COLLAPSED_DRAGGING) {
        if (currentLeft < -300) {
          left = -parentWidth
          targetState = STATE_SLIDED_TO_LEFT
        } else if (300 < currentLeft) {
          left = parentWidth
          targetState = STATE_SLIDED_TO_RIGHT
        } else {
          left = 0
          targetState = STATE_COLLAPSED
        }
      }

      val settleCaptureViewAt = dragHelper?.settleCapturedViewAt(left, top)!!
      if (settleCaptureViewAt) {
        setStateInternal(releasedChild, STATE_SETTLING)
        ViewCompat.postOnAnimation(releasedChild, SettleRunnable(releasedChild, targetState))
      } else {
        setStateInternal(releasedChild, targetState)
      }
    }

    override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
      return if (state == STATE_COLLAPSED_DRAGGING || state == STATE_COLLAPSED) {
        if (Math.abs(minOffset - top) >= 10) {
          setStateInternal(child, STATE_DRAGGING)
          constrain(top, 0, minOffset)
        } else {
          minOffset
        }
      } else {
        constrain(top, 0, minOffset)
      }
    }

    override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
      return if (state == STATE_COLLAPSED || state == STATE_COLLAPSED_DRAGGING) {
        constrain(left, -parentWidth, parentWidth)
      } else {
        0
      }
    }

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

  enum class Position {
    Left, Center, Right
  }

  interface OnPositionChangedListener {
    fun onPositionChanged(view: View, @State state: Long)
  }
}
