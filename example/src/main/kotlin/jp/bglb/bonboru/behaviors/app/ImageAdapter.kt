package jp.bglb.bonboru.behaviors.app

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

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
