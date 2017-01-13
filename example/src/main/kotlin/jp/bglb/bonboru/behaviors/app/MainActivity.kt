package jp.bglb.bonboru.behaviors.app

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import butterknife.bindView

/**
 * Created by tetsuya on 2017/01/09.
 */
class MainActivity() : AppCompatActivity() {

  val googleMap by bindView<Button>(R.id.google_map)
  val youtube by bindView<Button>(R.id.youtube)
  val navigation by bindView<Button>(R.id.botton_navigation)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    googleMap.setOnClickListener {
      startActivity(Intent(this, GoogleMapBehaviorActivity::class.java))
    }

    youtube.setOnClickListener {
      startActivity(Intent(this, YoutubeBehaviorActivity::class.java))
    }

    navigation.setOnClickListener {
      startActivity(Intent(this, BottomNavigationActivity::class.java))
    }
  }
}
