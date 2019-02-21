package net.pinae.caliperapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText

class MeasureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_measure)
    }

    fun saveMeasurement(view: View) {
        val inputField = findViewById<EditText>(R.id.measurementInput)
        val dataIntent = Intent()
        if (this.intent.hasExtra("MEASUREMENT_POSITION")) {
            dataIntent.putExtra("MEASUREMENT_POSITION", this.intent.getStringExtra("MEASUREMENT_POSITION"))
        }
        dataIntent.data = Uri.parse(inputField.text.toString())
        setResult(Activity.RESULT_OK, dataIntent)
        finish()
    }
}
