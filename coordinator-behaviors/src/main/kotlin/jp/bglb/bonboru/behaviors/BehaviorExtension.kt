package jp.bglb.bonboru.behaviors

import android.support.v4.view.NestedScrollingChild
import android.view.View
import android.view.ViewGroup

/**
 * view内を再帰的に調べ、NestedScrollingChildが実装されてるViewを探す
 */
fun findScrollingChild(view: View): View? = when (view) {
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

fun constrain(amount: Int, low: Int, high: Int): Int {
  return if (amount < low) {
    low
  } else if (amount > high) {
    high
  } else {
    amount
  }
}
