package net.pinae.caliperapp

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import java.util.GregorianCalendar

class SharedPreferencesWrapper(context: Context) {
    private val prefsFilename = "net.pinae.caliperapp.prefs"
    private val loginRejectedKey = "login_rejected"
    private val sexKey = "sex"
    private val birthdayKey = "birthday"
    private val prefs: SharedPreferences = context.getSharedPreferences(prefsFilename, 0)

    var loginRejected: Boolean
        get() = prefs.getBoolean(loginRejectedKey, false)
        set(value) = prefs.edit().putBoolean(loginRejectedKey, value).apply()

    var sex: Int
        get() = prefs.getInt(sexKey, -1)
        set(value) = prefs.edit().putInt(sexKey, value).apply()

    var birthday: GregorianCalendar
        get() {
            val tmpCal = GregorianCalendar()
            tmpCal.timeInMillis = prefs.getLong(birthdayKey,
                GregorianCalendar(1870, 0, 1).timeInMillis)
            return tmpCal
        }
        set(value) {
            prefs.edit().putLong(birthdayKey, value.timeInMillis).apply()
        }
}

val prefs: SharedPreferencesWrapper = PreferencedApplication.sharedPreferences!!

class PreferencedApplication: Application() {
    companion object {
        var sharedPreferences: SharedPreferencesWrapper? = null
    }

    override fun onCreate() {
        sharedPreferences = SharedPreferencesWrapper(applicationContext)
        super.onCreate()
    }
}