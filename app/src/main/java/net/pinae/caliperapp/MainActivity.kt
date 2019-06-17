package net.pinae.caliperapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.*
import android.support.v7.app.AppCompatActivity
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
    private var account: GoogleSignInAccount? = null
    private val measurement: Measurement = Measurement()
    private var displayValue: FatReading? = null
    var pinytoServiceMessenger: Messenger? = null
    var pinytoServiceIsBound: Boolean = false
    var pinytoServiceReady: Boolean = false

    private val pinytoServiceConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            pinytoServiceMessenger = Messenger(service)
            pinytoServiceIsBound = true
            checkPinyto()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            pinytoServiceMessenger = null
            pinytoServiceIsBound = false
        }
    }

    @SuppressLint("HandlerLeak")
    inner class MessagesFromPinytoServiceHandler: Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            if (msg == null) return
            val data = msg.data
            if (!data.containsKey("tag")) {
                Log.e(this@MainActivity.localClassName, "Answering messages to the need to have a tag!")
                return
            }
            when (data.getString("tag")) {
                "checkPinyto" -> {
                    Log.i(this@MainActivity.localClassName, "checkPinyto received.")
                    if (!data.containsKey("pinytoReady")) {
                        Log.e(this@MainActivity.localClassName, "The answer contains no key \"pinytoReady\".")
                        return
                    }
                    Log.i("checkPinytoAnswer", data.getBoolean("pinytoReady").toString())
                    pinytoServiceReady = data.getBoolean("pinytoReady")
                }
            }
        }
    }

    private val pinytoServiceReturnMessenger = Messenger(MessagesFromPinytoServiceHandler())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        newMeasurementButton.setOnClickListener { button -> measureBodyPart(button) }
    }

    override fun onResume() {
        super.onResume()
        createClient()
        bindToPinytoConnect()
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
                    val client :GoogleSignInClient = createClient()
                    client.signOut().addOnCompleteListener {
                        account = null
                        updateUI(account)
                    }
                    client.revokeAccess().addOnCompleteListener {
                        account = null
                        updateUI(account)
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

    private fun createClient() :GoogleSignInClient {
        val gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(Scopes.FITNESS_BODY_READ_WRITE))
            .build()
        return GoogleSignIn.getClient(this, gso)
    }

    private fun signIn() {
        val client :GoogleSignInClient = createClient()
        startActivityForResult(client.signInIntent, SIGN_IN_REQUEST_CODE)
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

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun bindToPinytoConnect() {
        val appName = "de.pinyto.pinyto_connect"
        if (isAppInstalled(appName)) {
            val bindPinytoServiceIntent = Intent("$appName.BIND")
            bindPinytoServiceIntent.setPackage(appName)
            bindPinytoServiceIntent.component = ComponentName(
                appName,
                "$appName.PinytoService"
            )
            bindService(bindPinytoServiceIntent, pinytoServiceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun createAnswerBundle(tag: String): Bundle {
        val bundle = Bundle()
        bundle.putString("tag", tag)
        bundle.putBinder("answerBinder", pinytoServiceReturnMessenger.binder)
        return bundle
    }

    private fun checkPinyto() {
        if (!pinytoServiceIsBound) return
        val msg = Message.obtain()
        val bundle = createAnswerBundle("checkPinyto")
        bundle.putString("path", "#check_pinyto")
        msg.data = bundle
        try {
            pinytoServiceMessenger?.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }
}
