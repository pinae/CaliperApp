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
import android.support.v4.app.Fragment
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.gms.common.api.ApiException


class MainActivity : AppCompatActivity(), MainFragment.OnMainFragmentInteractionListener {
    private var client: GoogleSignInClient? = null
    private var account: GoogleSignInAccount? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!checkAppPermissions(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BODY_SENSORS))) {
            requestAppPermissions(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BODY_SENSORS))
        }
        val gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(Scopes.FITNESS_BODY_READ_WRITE))
            .requestScopes(Scope(Scopes.PROFILE))
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
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SIGN_IN_REQUEST_CODE) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        } else if (requestCode == MEASURE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null && data.data != null &&
                data.hasExtra(MEASUREMENT_POSITION)) {
                Log.d("measure result", data.data!!.toString())
                Log.d("measure pos", data.getStringExtra(MEASUREMENT_POSITION))
            }
        } else if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            Log.d("TODO", "Need to implement this...")
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

    private fun checkAppPermissions(permissions: Array<String>):Boolean {
        var permissionsAccepted = true
        for (permission in permissions) {
            val permissionState = ActivityCompat.checkSelfPermission(this, permission)
            Log.d(permission, permissionState.toString())
            permissionsAccepted = permissionsAccepted && permissionState == PackageManager.PERMISSION_GRANTED
        }
        return permissionsAccepted
    }

    private fun requestAppPermissions(permissions: Array<String>) {
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
        if (account == null) account = getAccount(this)
        return account
    }

    override fun onFragmentInteraction(uri: Uri) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
