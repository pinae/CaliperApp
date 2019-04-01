package net.pinae.caliperapp

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.support.v4.app.Fragment
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.gms.common.api.ApiException
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


abstract class TopFragment: Fragment() {
    abstract fun setFatMeasurementNow(fatPercentage :Float)
}


class MainActivity : AppCompatActivity(),
    FatHistoryFragment.OnFatHistoryFragmentValueSelected,
    NotLoggedInFragment.OnLoginRequestByClick {
    private var client: GoogleSignInClient? = null
    private var account: GoogleSignInAccount? = null
    private val measurement: Measurement = Measurement()
    private var displayValue: FatReading? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        newMeasurementButton.setOnClickListener { button -> measureBodyPart(button) }
    }

    override fun onResume() {
        super.onResume()
        createClient()
        if ((getGoogleAccount() == null || getGoogleAccount()!!.id == null) && !prefs.loginRejected) signIn()
        updateUI(getGoogleAccount())
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.options_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            when (item.itemId) {
                R.id.action_set_sex -> startActivity(Intent(this, SelectSexActivity::class.java))
                R.id.action_set_birthday -> startActivity(Intent(this, SelectBirthdayActivity::class.java))
                R.id.action_logout -> {
                    if (client != null) {
                        client!!.signOut().addOnCompleteListener {
                            client = null
                            account = null
                            updateUI(null)
                        }
                        client!!.revokeAccess().addOnCompleteListener {
                            client = null
                            account = null
                            updateUI(null)
                        }
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            SIGN_IN_REQUEST_CODE -> {
                val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
                handleSignInResult(task)
            }
            MEASURE_REQUEST_CODE -> if (resultCode == Activity.RESULT_OK &&
                data != null && data.data != null && data.hasExtra(MEASUREMENT_BUNDLE)) {
                    measurement.setFromBundle(data.getBundleExtra(MEASUREMENT_BUNDLE))
                    val now = GregorianCalendar(TimeZone.getDefault())
                    val ageInMillis = now.timeInMillis - prefs.birthday.timeInMillis
                    val age = ageInMillis / (1000f * 60f * 60f * 24f * 365.2425f)
                    this.updateUI(getGoogleAccount())
                    if (!supportFragmentManager.fragments.isEmpty() &&
                        getTopFragment() is TopFragment) {
                        val topFragment = getTopFragment() as TopFragment
                        val fatPercentage = measurement.getFormula()(measurement.getSum(), age, prefs.sex) * 100
                        topFragment.setFatMeasurementNow(fatPercentage)
                        displayValue = FatReading(now.timeInMillis, fatPercentage)
                        updateUI(account)
                    } else throw Exception("topFragment is missing!")

            }
        }
    }

    private fun updateUI(account: GoogleSignInAccount?) {
        val frameId = R.id.topFragmentContainer
        if (account == null || account.id == null) {
            if (getTopFragment() != null && getTopFragment() is NotLoggedInFragment)
                setFragment(getTopFragment()!!, frameId)
            else setFragment(NotLoggedInFragment.newInstance(), frameId)
        } else {
            if (getTopFragment() != null && getTopFragment() is FatHistoryFragment)
                setFragment(getTopFragment()!!, frameId)
            else setFragment(FatHistoryFragment.newInstance(), frameId)
        }
        if (displayValue == null) {
            measurementValueCaption.visibility = View.GONE
            measurementValue.visibility = View.GONE
        } else {
            measurementValueCaption.text = getString(R.string.display_value_caption, displayValue!!.date)
            measurementValue.text = getString(R.string.display_value, displayValue!!.value)
            measurementValueCaption.visibility = View.VISIBLE
            measurementValue.visibility = View.VISIBLE
        }
        if (displayValue != null && getTopFragment() is FatHistoryFragment) {
            deleteMeasurementButton.visibility = View.VISIBLE
        } else {
            deleteMeasurementButton.visibility = View.GONE
        }
    }

    private fun getTopFragment() :Fragment? = if (supportFragmentManager.fragments.isEmpty()) null
    else supportFragmentManager.fragments[supportFragmentManager.fragments.count() - 1]

    private fun setFragment(fragment: Fragment, frameId: Int) {
        when {
            supportFragmentManager.fragments.isEmpty() -> addFragment(fragment, frameId)
            else -> replaceFragment(fragment, frameId)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun measureBodyPart(view: View? = null) {
        val measureIntent = Intent(this, MeasureActivity::class.java)
        measureIntent.putExtra(MEASUREMENT_POSITION, measurement.getNextMeasurePosition())
        measureIntent.putExtra(MEASUREMENT_BUNDLE, measurement.writeToBundle())
        startActivityForResult(measureIntent, MEASURE_REQUEST_CODE)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            account = completedTask.getResult(ApiException::class.java)
            prefs.loginRejected = false
            // Signed in successfully, show authenticated UI.
            updateUI(account)
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("MainActivity", "signInResult:failed code=" + e.statusCode)
            prefs.loginRejected = true
            updateUI(null)
        }

    }

    private fun createClient() {
        val gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(Scopes.FITNESS_BODY_READ_WRITE))
            .build()
        client = GoogleSignIn.getClient(this, gso)
    }

    private fun signIn() {
        if (client == null) createClient()
        startActivityForResult(client!!.signInIntent, SIGN_IN_REQUEST_CODE)
    }

    private fun getGoogleAccount():GoogleSignInAccount? {
        if (account == null) account = getAccount(this)
        return account
    }

    override fun onFatHistoryValueSelected(value: FatReading?) {
        displayValue = value
        updateUI(getGoogleAccount())
    }

    override fun onGoogleLoginRequested() {
        signIn()
    }

    @Suppress("UNUSED_PARAMETER")
    fun deleteMeasurement(view: View?) {
        if (displayValue != null && getTopFragment() != null && getTopFragment() is FatHistoryFragment)
            (getTopFragment() as FatHistoryFragment).deleteDataPoint(displayValue!!)
    }
}
