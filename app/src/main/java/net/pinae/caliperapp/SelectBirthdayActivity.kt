package net.pinae.caliperapp

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_select_birthday.calendarView
import java.util.Date
import java.util.GregorianCalendar

class SelectBirthdayActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_birthday)
        if (prefs.birthday < GregorianCalendar(1870, 0, 2).time) {
            calendarView.date = GregorianCalendar(1985, 5, 15).timeInMillis
        } else {
            calendarView.date = prefs.birthday.time
        }
    }

    fun setDate(view: View) {
        prefs.birthday = Date(calendarView.date)
        finish()
    }
}
