package net.pinae.caliperapp

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun measureStomach(view: View) {
        val measureStomachIntent = Intent(this, MeasureActivity::class.java)
        startActivity(measureStomachIntent)
    }
}
