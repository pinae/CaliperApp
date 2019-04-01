package net.pinae.caliperapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_measure.*
import java.lang.NumberFormatException
import java.util.*

class MeasureActivity : AppCompatActivity() {
    private val measurement :Measurement = Measurement()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_measure)
        if (this.intent.hasExtra(MEASUREMENT_BUNDLE)) {
            measurement.setFromBundle(this.intent.getBundleExtra(MEASUREMENT_BUNDLE))
        }
        if (this.intent.hasExtra(MEASUREMENT_POSITION)) {
            Log.d("Measurement position", this.intent.getStringExtra(MEASUREMENT_POSITION))
            when (this.intent.getStringExtra(MEASUREMENT_POSITION)) {
                BELLY -> measurementPositionImageView.setImageResource(R.drawable.ic_belly)
                HIPS -> measurementPositionImageView.setImageResource(R.drawable.ic_hips)
                TRICEPS -> measurementPositionImageView.setImageResource(R.drawable.ic_triceps)
                CHEST -> measurementPositionImageView.setImageResource(R.drawable.ic_chest)
                TIGH -> measurementPositionImageView.setImageResource(R.drawable.ic_tigh)
                ARMPIT -> measurementPositionImageView.setImageResource(R.drawable.ic_armpit)
                SCAPULA -> measurementPositionImageView.setImageResource(R.drawable.ic_scapula)
            }
        }
        if (measurement.missingOnlyTheLastMeasurement()) {
            continueMeasurementButton.visibility = View.GONE
        } else {
            continueMeasurementButton.visibility = View.VISIBLE
        }
        measurementInput.addTextChangedListener(object :TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                try {
                    val enteredValue = p0.toString().toFloat()
                    if (enteredValue in 2.0..60.0) {
                        continueMeasurementButton.isEnabled = true
                        abortMeasurementButton.isEnabled = true
                    } else {
                        continueMeasurementButton.isEnabled = false
                        abortMeasurementButton.isEnabled = false
                    }
                } catch (e :NumberFormatException) {
                    continueMeasurementButton.isEnabled = false
                    abortMeasurementButton.isEnabled = false
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("MeasureAct onActRes", requestCode.toString() + ", " + resultCode.toString() + ", Data: " + data.toString())
        if (requestCode == MEASURE_REQUEST_CODE &&
            resultCode == Activity.RESULT_OK &&
            data != null && data.data != null && data.hasExtra(MEASUREMENT_BUNDLE)) {
            measurement.setFromBundle(data.getBundleExtra(MEASUREMENT_BUNDLE))
            Log.d("measurement from bundle", measurement.toString())
            val dataIntent = Intent()
            dataIntent.putExtra(MEASUREMENT_BUNDLE, measurement.writeToBundle())
            dataIntent.data = Uri.parse(measurement.getSum().toString())
            setResult(Activity.RESULT_OK, dataIntent)
            finish()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun toggleExplanation(view: View) {
        when (view) {
            expandExplanationButton, explanationHeading -> {
                if (explanationText.visibility == View.GONE) {
                    explanationText.visibility = View.VISIBLE
                    expandExplanationButton.setText(R.string.collapse_symbol)
                } else {
                    explanationText.visibility = View.GONE
                    expandExplanationButton.setText(R.string.expand_symbol)
                }
            }
        }
    }

    fun saveMeasurement(view: View) {
        val inputField = findViewById<EditText>(R.id.measurementInput)
        if (this.intent.hasExtra(MEASUREMENT_POSITION)) {
            val pos = this.intent.getStringExtra(MEASUREMENT_POSITION)
            try {
                val measuredValue = inputField.text.toString().toFloat()
                when (pos) {
                    BELLY -> measurement.dBelly = measuredValue
                    HIPS -> measurement.dHips = measuredValue
                    TRICEPS -> measurement.dTriceps = measuredValue
                    CHEST -> measurement.dChest = measuredValue
                    TIGH -> measurement.dTigh = measuredValue
                    ARMPIT -> measurement.dArmpit = measuredValue
                    SCAPULA -> measurement.dScapula = measuredValue
                }
                Log.d("measurement in mesAct", measurement.toString())
                when (view) {
                    continueMeasurementButton -> {
                        if (measurement.allValuesMeasured()) {
                            setResultAndFinish()
                        } else {
                            startNextMeasurement()
                        }
                    }
                    abortMeasurementButton -> setResultAndFinish()
                }
            } catch (e :NumberFormatException) {
                Toast.makeText(this, R.string.value_is_not_a_number, Toast.LENGTH_SHORT).show()
            }
        } else throw MissingResourceException("There needs to be a MEASUREMENT_POSITION in the Intent.",
            this.localClassName, MEASUREMENT_POSITION)
    }

    private fun setResultAndFinish() {
        val dataIntent = Intent()
        dataIntent.putExtra(MEASUREMENT_BUNDLE, measurement.writeToBundle())
        dataIntent.data = Uri.parse(measurement.getSum().toString())
        setResult(Activity.RESULT_OK, dataIntent)
        finish()
    }

    private fun startNextMeasurement() {
        val measureIntent = Intent(this, MeasureActivity::class.java)
        measureIntent.putExtra(MEASUREMENT_POSITION, measurement.getNextMeasurePosition())
        measureIntent.putExtra(MEASUREMENT_BUNDLE, measurement.writeToBundle())
        startActivityForResult(measureIntent, MEASURE_REQUEST_CODE)
    }
}
