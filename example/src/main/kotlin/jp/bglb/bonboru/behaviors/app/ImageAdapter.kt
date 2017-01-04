package jp.bglb.bonboru.behaviors.app

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

/**
 * Created by Tetsuya Masuda on 2016/12/23.
 */
class ImageAdapter(context: Context) : PagerAdapter() {

  private val layoutInflater = LayoutInflater.from(context)

  override fun isViewFromObject(view: View?, target: Any?): Boolean = view == target

  override fun getCount(): Int = 3

  override fun destroyItem(container: ViewGroup?, position: Int, view: Any?) {
    container?.removeView(view as View)
  }

  override fun instantiateItem(container: ViewGroup?, position: Int): Any {
    val view = layoutInflater.inflate(R.layout.list_item_image, container, false) as ImageView
    val resId = when (position) {
      0 -> R.drawable.frog
      1 -> R.drawable.kingfisher
      2 -> R.drawable.rabbit
      else -> R.drawable.frog
    }
    view.setImageResource(resId)
    container?.addView(view)
    return view
  }
}
