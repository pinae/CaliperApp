package net.pinae.caliperapp

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import kotlinx.android.synthetic.main.activity_select_birthday.calendarView
import kotlinx.android.synthetic.main.activity_select_birthday.yearEdit
import java.util.GregorianCalendar

class SelectBirthdayActivity : AppCompatActivity() {
    val selectedDate :GregorianCalendar = GregorianCalendar()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_birthday)
        if (prefs.birthday < GregorianCalendar(1870, 0, 2)) {
            val defaultBirthdayMillis = GregorianCalendar(1985, 5, 15).timeInMillis
            calendarView.date = defaultBirthdayMillis
            selectedDate.timeInMillis = defaultBirthdayMillis
        } else {
            calendarView.date = prefs.birthday.timeInMillis
            selectedDate.timeInMillis = prefs.birthday.timeInMillis
        }
        val calendarHelper = GregorianCalendar.getInstance()
        calendarView.maxDate = calendarHelper.timeInMillis
        calendarHelper.set(1870, 0, 2)
        calendarView.minDate = calendarHelper.timeInMillis
        calendarHelper.timeInMillis = calendarView.date
        yearEdit.setText(calendarHelper[GregorianCalendar.YEAR].toString())
        var changingDate = false
        calendarView.setOnDateChangeListener { _, year, month, day ->
            changingDate = true
            yearEdit.setText(year.toString())
            changingDate = false
            selectedDate.set(GregorianCalendar.YEAR, year)
            selectedDate.set(GregorianCalendar.MONTH, month)
            selectedDate.set(GregorianCalendar.DAY_OF_MONTH, day)
        }
        yearEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {

            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (!changingDate) {
                    try {
                        val enteredYear: Int = p0.toString().toInt()
                        if (enteredYear in 1870..GregorianCalendar.getInstance()[GregorianCalendar.YEAR]) {
                            selectedDate.set(GregorianCalendar.YEAR, enteredYear)
                            calendarView.setDate(selectedDate.timeInMillis, true, false)
                        }
                    } catch (ex :NumberFormatException) {}
                }
            }
        })
    }

    @Suppress("UNUSED_PARAMETER")
    fun setDate(view: View) {
        prefs.birthday = selectedDate
        finish()
    }
}
