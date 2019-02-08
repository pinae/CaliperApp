package net.pinae.caliperapp

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class MeasureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_measure)
    }

    fun saveMeasurement(view: View) {
        finish()
    }
}
