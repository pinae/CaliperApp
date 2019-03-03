package net.pinae.caliperapp

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType

fun getGoogleSignInOptionsExtension():GoogleSignInOptionsExtension {
    return FitnessOptions.builder()
        .addDataType(DataType.TYPE_BODY_FAT_PERCENTAGE, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_BODY_FAT_PERCENTAGE, FitnessOptions.ACCESS_WRITE)
        .build()
}

fun getAccount(context: Context):GoogleSignInAccount {
    return GoogleSignIn.getAccountForExtension(context, getGoogleSignInOptionsExtension())
}