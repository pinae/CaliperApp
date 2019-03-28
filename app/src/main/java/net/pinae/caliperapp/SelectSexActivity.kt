package net.pinae.caliperapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity;
import android.view.View
import kotlinx.android.synthetic.main.activity_select_sex.select_female_button
import kotlinx.android.synthetic.main.activity_select_sex.select_male_button

class SelectSexActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_sex)
    }

    fun selectSex(view: View) {
        when (view) {
            select_female_button -> {
                prefs.sex = FEMALE
            }
            select_male_button -> {
                prefs.sex = MALE
            }
        }
        finish()
    }
}
