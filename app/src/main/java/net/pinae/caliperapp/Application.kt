package net.pinae.caliperapp

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import java.util.GregorianCalendar

class SharedPreferencesWrapper(context: Context) {
    private val PREFS_FILENAME = "net.pinae.caliperapp.prefs"
    private val SEX = "sex"
    private val BIRTHDAY = "birthday"
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0)

    var sex: Int
        get() = prefs.getInt(SEX, -1)
        set(value) = prefs.edit().putInt(SEX, value).apply()

    var birthday: GregorianCalendar
        get() {
            val tmpCal = GregorianCalendar()
            tmpCal.timeInMillis = prefs.getLong(BIRTHDAY,
                GregorianCalendar(1870, 0, 1).timeInMillis)
            return tmpCal
        }
        set(value) {
            prefs.edit().putLong(BIRTHDAY, value.timeInMillis).apply()
        }
}

val prefs: SharedPreferencesWrapper by lazy {
    PreferencedApplication.sharedPreferences!!
}

class PreferencedApplication: Application() {
    companion object {
        var sharedPreferences: SharedPreferencesWrapper? = null
    }

    override fun onCreate() {
        sharedPreferences = SharedPreferencesWrapper(applicationContext)
        super.onCreate()
    }
}