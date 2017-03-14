package jp.bglb.bonboru.behaviors.app

import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import butterknife.bindView
import jp.bglb.bonboru.behaviors.VerticalDraggableBehavior
import jp.bglb.bonboru.behaviors.VerticalDraggableBehavior.OnBehaviorStateListener
import jp.bglb.bonboru.behaviors.YoutubeLikeBehavior

/**
 * Created by tetsuya on 2017/01/05.
 */
class VerticalDraggableBehaviorActivity : AppCompatActivity(), OnBehaviorStateListener {
  val root by bindView<CoordinatorLayout>(R.id.root)
  val show by bindView<Button>(R.id.show)
  val reset by bindView<Button>(R.id.reset)

  var media: ImageView? = null
  var description: View? = null
  var wipe: Button? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_vertical_draggable_behavior)

    show.setOnClickListener {
      media = layoutInflater.inflate(R.layout.include_vertical_draggable_content, root,
          false) as ImageView
      val behavior = VerticalDraggableBehavior.from(media)
      behavior?.listener = this
      root.addView(media)

//      description = layoutInflater.inflate(R.layout.include_description, root, false)
//      wipe = description?.findViewById(R.id.wipe) as Button
//      root.addView(description)

      wipe?.setOnClickListener {
        val behavior = VerticalDraggableBehavior.from(media)
        behavior?.updateState(VerticalDraggableBehavior.STATE_EXPANDED)
      }
    }

    reset.setOnClickListener {
      val behavior = VerticalDraggableBehavior.from(media)
      behavior?.updateState(VerticalDraggableBehavior.STATE_COLLAPSED)
    }
  }

  override fun onBehaviorStateChanged(newState: Long) {
//      root.removeView(media)
//      root.removeView(description)
  }
}
