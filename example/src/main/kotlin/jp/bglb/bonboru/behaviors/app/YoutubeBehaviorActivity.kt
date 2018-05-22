package jp.bglb.bonboru.behaviors.app

import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import jp.bglb.bonboru.behaviors.YoutubeLikeBehavior
import kotterknife.bindView

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
class YoutubeBehaviorActivity : AppCompatActivity(), YoutubeLikeBehavior.OnBehaviorStateListener {
  val root by bindView<CoordinatorLayout>(R.id.root)
  val show by bindView<Button>(R.id.show)
  val ignore by bindView<Button>(R.id.ignore)
  val reset by bindView<Button>(R.id.reset)

  var media: ImageView? = null
  var description: View? = null
  var wipe: Button? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_youtube)

    show.setOnClickListener {
      media = layoutInflater.inflate(R.layout.include_media, root, false) as ImageView
      description = layoutInflater.inflate(R.layout.include_description, root, false)
      wipe = description?.findViewById(R.id.wipe) as Button
      val behavior = YoutubeLikeBehavior.from(media)
      behavior?.listener = this

      root.addView(media)
      root.addView(description)

      wipe?.setOnClickListener {
        YoutubeLikeBehavior.from(media)?.let {
          it.updateState(YoutubeLikeBehavior.STATE_SHRINK)
        }
      }
    }

    ignore.setOnClickListener {
      YoutubeLikeBehavior.from(media)?.let {
        it.draggable = !it.draggable
      }
    }

    reset.setOnClickListener {
      val behavior = YoutubeLikeBehavior.from(media)
      behavior?.updateState(YoutubeLikeBehavior.STATE_EXPANDED)
    }
  }

  override fun onBehaviorStateChanged(newState: Int) {
    if (newState == YoutubeLikeBehavior.STATE_TO_RIGHT || newState == YoutubeLikeBehavior.STATE_TO_LEFT) {
      root.removeView(media)
      root.removeView(description)
    }
  }
}
