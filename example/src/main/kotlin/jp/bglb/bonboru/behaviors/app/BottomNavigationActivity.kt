package jp.bglb.bonboru.behaviors.app

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import butterknife.bindView

/**
 * Created by tetsuya on 2017/01/12.
 */
class BottomNavigationActivity() : AppCompatActivity() {

  val toolbar by bindView<Toolbar>(R.id.toolbar)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_bottom_navigation)
    setSupportActionBar(toolbar)
    title = "BottomNavigationBehavior"
  }
}
