package net.pinae.caliperapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.view.View
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.gms.common.api.ApiException
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), MainFragment.OnMainFragmentInteractionListener {
    private var client: GoogleSignInClient? = null
    private var account: GoogleSignInAccount? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!checkPermissions(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BODY_SENSORS))) {
            requestPermissions(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BODY_SENSORS))
        }
        val gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(Scopes.FITNESS_BODY_READ_WRITE))
            .build()
        client = GoogleSignIn.getClient(this, gso)
    }

    override fun onResume() {
        super.onResume()
        if (getGoogleAccount() == null) {
            Log.d("GoogleAccount", getGoogleAccount().toString())
            signIn()
        }
        updateUI(account)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SIGN_IN_REQUEST_CODE) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
            /*if (task.isSuccessful) {
                account = task.result
            } else {
                Log.i("Login failed", "try again")
            }*/
        } else if (requestCode == MEASURE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null && data.data != null &&
                data.hasExtra("MEASUREMENT_POSITION")) {
                Log.d("measure result", data.data!!.toString())
                Log.d("measure pos", data.getStringExtra("MEASUREMENT_POSITION"))
            }
        }
    }

    private fun updateUI(account: GoogleSignInAccount?) {
        val frameId = R.id.baseLayout
        if (account == null) {
            setFragment(NotLoggedInFragment.newInstance(), frameId)
        } else {
            setFragment(MainFragment.newInstance("a", "b"), frameId)
        }
    }

    private fun setFragment(fragment: Fragment, frameId: Int) {
        if (supportFragmentManager.fragments.isEmpty()) {
            addFragment(fragment, frameId)
        } else {
            replaceFragment(fragment, frameId)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)

            // Signed in successfully, show authenticated UI.
            updateUI(account)
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("MainActivity", "signInResult:failed code=" + e.statusCode)
            updateUI(null)
        }

    }

    private fun checkPermissions(permissions: Array<String>):Boolean {
        var permissionsAccepted = true
        for (permission in permissions) {
            val permissionState = ActivityCompat.checkSelfPermission(this, permission)
            Log.d(permission, permissionState.toString())
            permissionsAccepted = permissionsAccepted && permissionState == PackageManager.PERMISSION_GRANTED
        }
        return permissionsAccepted
    }

    private fun requestPermissions(permissions: Array<String>) {
        var needRationale = false
        for (permission in permissions) {
            needRationale = needRationale ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
        }
        if (needRationale) {
            Log.i("missing Permissions", "showing request permission rationale")
            val view: View? = findViewById(android.R.id.content)
            if (view != null) {
                Log.d("view", view.toString())
                Snackbar.make(
                    view, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE
                ).setAction(
                    R.string.ok
                ) {
                    ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE)
                }.show()
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE)
        }
    }

    private fun signIn() {
        startActivityForResult(client!!.signInIntent, SIGN_IN_REQUEST_CODE)
    }

    private fun getGoogleAccount():GoogleSignInAccount? {
        if (account == null) account = GoogleSignIn.getLastSignedInAccount(this)
        return account
    }

    override fun onFragmentInteraction(uri: Uri) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
