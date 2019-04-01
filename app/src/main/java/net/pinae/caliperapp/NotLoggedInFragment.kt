package net.pinae.caliperapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_not_logged_in.view.*


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
        val view = inflater.inflate(R.layout.fragment_not_logged_in, container, false)
        view.logInToGoogleButton.setOnClickListener { requestLogin() }
        return view
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

    private fun requestLogin() {
        if (this.listener != null) this.listener!!.onGoogleLoginRequested()
    }

    override fun setFatMeasurementNow(fatPercentage :Float) {

    }
}
