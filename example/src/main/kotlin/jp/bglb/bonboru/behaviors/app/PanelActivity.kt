package jp.bglb.bonboru.behaviors.app

import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import butterknife.bindView
import jp.bglb.bonboru.behaviors.PanelBehavior

/**
 * Created by tetsuya on 2017/01/20.
 */
class PanelActivity : AppCompatActivity(), PanelBehavior.OnPositionChangedListener {

  val root by bindView<CoordinatorLayout>(R.id.root)
  val panel1 by bindView<TextView>(R.id.panel1)
  val panel2 by bindView<TextView>(R.id.panel2)
  val panel3 by bindView<TextView>(R.id.panel3)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_panel)
    PanelBehavior.from(panel1)?.listener = this
    PanelBehavior.from(panel2)?.listener = this
    PanelBehavior.from(panel3)?.listener = this
  }

  override fun onPositionChanged(view: View, @PanelBehavior.Companion.State state: Long) {
    if (state == PanelBehavior.STATE_SLIDED_TO_RIGHT) {
      arrayOf(panel1, panel2, panel3).forEach {
        val behavior = PanelBehavior.from(it)
        behavior?.position = when (behavior?.position) {
          PanelBehavior.Position.Left -> PanelBehavior.Position.Center

          PanelBehavior.Position.Center -> PanelBehavior.Position.Right

          PanelBehavior.Position.Right -> PanelBehavior.Position.Left

          else -> PanelBehavior.Position.Center
        }
      }
    } else if (state == PanelBehavior.STATE_SLIDED_TO_LEFT) {
      arrayOf(panel1, panel2, panel3).forEach {
        val behavior = PanelBehavior.from(it)
        behavior?.position = when (behavior?.position) {
          PanelBehavior.Position.Left -> PanelBehavior.Position.Right

          PanelBehavior.Position.Center -> PanelBehavior.Position.Left

          PanelBehavior.Position.Right -> PanelBehavior.Position.Right

          else -> PanelBehavior.Position.Center
        }
      }
    }
  }
}
