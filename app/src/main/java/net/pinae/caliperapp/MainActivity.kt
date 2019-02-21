package net.pinae.caliperapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.support.design.widget.Snackbar
import android.view.View
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task

class MainActivity : AppCompatActivity() {
    val REQUEST_PERMISSIONS_REQUEST_CODE = 34
    val REQUEST_SIGN_IN_REQUEST_CODE = 9001
    val MEASURE_REQUEST_CODE = 213
    var client: GoogleSignInClient? = null

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
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account == null) {
            signIn()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SIGN_IN_REQUEST_CODE) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            if (task.isSuccessful()) {
                val account: GoogleSignInAccount? = task.getResult()
            } else {
                Log.i("Login failed", "try again")
            }
        } else if (requestCode == MEASURE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null && data.data != null &&
                data.hasExtra("MEASUREMENT_POSITION")) {
                Log.d("measure result", data.data!!.toString())
                Log.d("measure pos", data.getStringExtra("MEASUREMENT_POSITION"))
            }
        }
    }

    fun measure(view: View) {
        val measureStomachIntent = Intent(this, MeasureActivity::class.java)
        when (view) {
            findViewById<View>(R.id.stomachButton) -> {
                measureStomachIntent.putExtra("MEASUREMENT_POSITION", "STOMACH")
            }
        }
        startActivityForResult(measureStomachIntent, MEASURE_REQUEST_CODE)
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
                    ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS_REQUEST_CODE)
                }.show()
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS_REQUEST_CODE)
        }
    }

    private fun signIn() {
        startActivityForResult(client!!.signInIntent, REQUEST_SIGN_IN_REQUEST_CODE)
    }
}
