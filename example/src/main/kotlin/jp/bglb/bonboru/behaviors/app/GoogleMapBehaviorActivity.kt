package jp.bglb.bonboru.behaviors.app

import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v4.view.ViewPager
import android.support.v4.widget.NestedScrollView
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.widget.Button
import butterknife.bindView
import jp.bglb.bonboru.behaviors.GoogleMapLikeBehavior


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
class GoogleMapBehaviorActivity() : AppCompatActivity() {

  val toolbar by bindView<Toolbar>(R.id.toolbar)
  val appbar by bindView<AppBarLayout>(R.id.appbar)
  val mergedToolbar by bindView<Toolbar>(R.id.merged_toolbar)
  val pager by bindView<ViewPager>(R.id.pager)
  val bottomContent by bindView<NestedScrollView>(R.id.bottom_content)
  val show by bindView<Button>(R.id.show)
  val hide by bindView<Button>(R.id.hide)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//      findViewById(
//          android.R.id.content).systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//    }
    setContentView(R.layout.activity_example)
    setSupportActionBar(toolbar)
    title = "GoogleMapLikeBehavior"
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    pager.adapter = ImageAdapter(this)
    show.setOnClickListener {
      val behavior = GoogleMapLikeBehavior.from(bottomContent)
      behavior?.updateState(GoogleMapLikeBehavior.STATE_COLLAPSED)
    }

    hide.setOnClickListener {
      val behavior = GoogleMapLikeBehavior.from(bottomContent)
      behavior?.updateState(GoogleMapLikeBehavior.STATE_HIDDEN)
    }

    mergedToolbar.setNavigationOnClickListener {
      bottomContent.smoothScrollTo(0, 0)
      val behavior = GoogleMapLikeBehavior.from(bottomContent)
      behavior?.updateState(GoogleMapLikeBehavior.STATE_COLLAPSED)
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> {
        val behavior = GoogleMapLikeBehavior.from(bottomContent)
        behavior?.updateState(GoogleMapLikeBehavior.STATE_COLLAPSED)
        true
      }

      else -> super.onOptionsItemSelected(item)
    }
  }
}