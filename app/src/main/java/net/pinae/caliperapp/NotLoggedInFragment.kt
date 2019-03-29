package net.pinae.caliperapp

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


class NotLoggedInFragment : TopFragment() {
    private var listener: OnLoginRequestByClick? = null

    interface OnLoginRequestByClick {
        fun onGoogleLoginRequested()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_not_logged_in, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnLoginRequestByClick) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFatHistoryFragmentValueSelected")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment NotLoggedInFragment.
         */
        @JvmStatic
        fun newInstance() =
            NotLoggedInFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }

    fun logInToGoogle(view: View) {
        if (listener != null) listener!!.onGoogleLoginRequested()
    }

    override fun setFatMeasurementNow(fat :Float) {

    }
}
