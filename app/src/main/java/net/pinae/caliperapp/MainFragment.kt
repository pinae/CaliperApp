package net.pinae.caliperapp

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.*
import com.google.android.gms.fitness.request.DataReadRequest
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.fragment_main.view.*
import java.text.DateFormat.getDateInstance
import java.util.concurrent.TimeUnit
import com.google.android.gms.fitness.result.DataReadResponse
import com.google.android.gms.tasks.Task
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter
import com.jjoe64.graphview.series.DataPoint as GVDataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.text.DateFormat.getTimeInstance
import java.util.*


data class FatReading(val date: Long, val value: Float)


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [MainFragment.OnMainFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [MainFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class MainFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnMainFragmentInteractionListener? = null
    val measurement: Measurement = Measurement()
    var fatHistory: List<FatReading> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        if (prefs.sex < 0) startActivity(Intent(activity, SelectSexActivity::class.java))
        if (prefs.birthday < GregorianCalendar(1870, 0, 2)) {
            startActivity(Intent(activity, SelectBirthdayActivity::class.java))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_main, container, false)
        view.stomachButton.setOnClickListener { button -> measureBodyPart(button) }
        view.fatGraph.viewport.isScalable = true
        view.fatGraph.viewport.isScrollable = true
        view.fatGraph.gridLabelRenderer.labelFormatter = DateAsXAxisLabelFormatter(activity)
        view.fatGraph.gridLabelRenderer.numHorizontalLabels = 3
        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("MainFragment onActRes", requestCode.toString() + ", " + resultCode.toString() + ", Data: " + data.toString())
        if (requestCode == MEASURE_REQUEST_CODE && resultCode == Activity.RESULT_OK &&
            data != null && data.data != null && data.hasExtra(MEASUREMENT_BUNDLE)) {
            Log.d("measure sum", data.data!!.toString())
            measurement.setFromBundle(data.getBundleExtra(MEASUREMENT_BUNDLE))
            Log.d("measurement from bundle", measurement.toString())
            val now = GregorianCalendar(TimeZone.getDefault())
            val ageInMillis = now.timeInMillis - prefs.birthday.timeInMillis
            val age = ageInMillis / (1000f * 60f * 60f * 24f * 365.2425f)
            saveBodyFat(measurement.getFormula()(measurement.getSum(), age, prefs.sex))
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnMainFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainFragment", "resuming...")
        loadBodyFatData()
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnMainFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MainFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MainFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    fun measureBodyPart(view: View? = null) {
        val measureIntent = Intent(activity, MeasureActivity::class.java)
        measureIntent.putExtra(MEASUREMENT_POSITION, measurement.getNextMeasurePosition())
        measureIntent.putExtra(MEASUREMENT_BUNDLE, measurement.writeToBundle())
        startActivityForResult(measureIntent, MEASURE_REQUEST_CODE)
    }

    private fun subscribeToBodyFat() {
        val account = getAccount(activity as Context)
        Log.d("GSignIn account", account.toString())
        val recordingClient = Fitness.getRecordingClient(activity as Activity, account)
        recordingClient.listSubscriptions(DataType.TYPE_BODY_FAT_PERCENTAGE).addOnSuccessListener {
                subscriptions -> if (subscriptions.isEmpty()) recordingClient
            .subscribe(DataType.TYPE_BODY_FAT_PERCENTAGE)
            .addOnSuccessListener { Log.i("Fitness API", "Subscribed to TYPE_BODY_FAT_PERCENTAGE") }
            .addOnFailureListener { exception -> Log.e("FitnessAPI error", exception.message) } }
    }

    private fun loadBodyFatData() {
        val account = getAccount(activity as Context)
        if (!GoogleSignIn.hasPermissions(account, getGoogleSignInOptionsExtension())) {
            GoogleSignIn.requestPermissions(activity as Activity, GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                account, getGoogleSignInOptionsExtension())
            return
        }
        subscribeToBodyFat()
        val cal = Calendar.getInstance()
        val now = Date()
        cal.time = now
        val endTime = cal.timeInMillis
        cal.add(Calendar.WEEK_OF_YEAR, -1)
        val startTime = cal.timeInMillis

        val dateFormat = getDateInstance()
        Log.i(TAG, "Range Start: " + dateFormat.format(startTime))
        Log.i(TAG, "Range End: " + dateFormat.format(endTime))

        val readRequest: DataReadRequest = DataReadRequest.Builder()
            .read(DataType.TYPE_BODY_FAT_PERCENTAGE)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        val response: Task<DataReadResponse> = Fitness.getHistoryClient(activity as Activity, account)
            .readData(readRequest)
            .addOnSuccessListener { response -> bodyFatDataLoaded(response) }
            .addOnFailureListener { exception -> Log.e(TAG, "Data load failed: " + exception.message ) }

        /*if (response. !is Task<DataReadResult>) return
        val dataReadResult: DataReadResult = Tasks.await(response as Task<DataReadResult>)

        //val response = Fitness.getHistoryClient(activity as Activity, account).readData(readRequest)
        if (response.result == null) {
            Log.i(TAG, "The Fitness API returned no dataSets for TYPE_BODY_FAT_PERCENTAGE")
        } else {
            val dataSets = response.result!!.dataSets
            for (dataSet in dataSets) {
                dumpDataSet(dataSet)
            }
        }*/
    }

    private fun bodyFatDataLoaded(response: DataReadResponse) {
        Log.d("loaded dataSets", response.dataSets.toString())
        if (response.dataSets == null || response.dataSets.isEmpty()) {
            Log.i(TAG, "The Fitness API returned no dataSets for TYPE_BODY_FAT_PERCENTAGE")
        } else {
            for (dataSet in response.dataSets) {
                dumpDataSet(dataSet)
                for (dp in dataSet.dataPoints) {
                    var foundValue: Float? = null
                    for (field in dp.dataType.fields) {
                        if (field.name == "percentage") foundValue = dp.getValue(field).asFloat()
                    }
                    if (foundValue != null) {
                        updateFatHistory(FatReading(dp.getEndTime(TimeUnit.MILLISECONDS), foundValue))
                    }
                }
            }
        }
    }

    private fun dumpDataSet(dataSet: DataSet) {
        Log.i(TAG, "Data returned for Data type: " + dataSet.dataType.name)
        val dateFormat = getTimeInstance()

        for (dp in dataSet.dataPoints) {
            Log.i(TAG, "Data point:")
            Log.i(TAG, "\tType: " + dp.dataType.name)
            Log.i(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)))
            Log.i(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)))
            for (field in dp.dataType.fields) {
                Log.i(TAG, "\tField: " + field.name + " Value: " + dp.getValue(field))
            }
        }
    }

    private fun updateFatHistory(dataPoint: FatReading) {
        val newHistory = arrayListOf<FatReading>()
        var pointAdded = false
        for (entry in fatHistory) {
            when {
                entry.date < dataPoint.date -> newHistory.add(entry)
                entry.date == dataPoint.date -> newHistory.add(dataPoint)
                else -> {
                    if (!pointAdded) {
                        newHistory.add(dataPoint)
                        pointAdded = true
                    }
                    newHistory.add(entry)
                }
            }
        }
        if (!pointAdded) newHistory.add(dataPoint)
        fatHistory = newHistory
        updateFatDiagram()
        Log.d("fatHistory", fatHistory.toString())
    }

    private fun updateFatDiagram() {
        val dataSeries = LineGraphSeries<GVDataPoint>()
        for (entry in fatHistory) {
            Log.d("fatHistory entry", Date(entry.date).toString() + ": " + entry.value.toString())
            dataSeries.appendData(GVDataPoint(Date(entry.date), entry.value.toDouble() * 100), true, 500)
        }
        dataSeries.isDrawDataPoints = true
        dataSeries.dataPointsRadius = 10.0f
        dataSeries.thickness = 7
        dataSeries.setAnimated(true)
        dataSeries.setOnDataPointTapListener { _, dataPoint -> Log.d("dp", dataPoint.x.toString() + ", " + dataPoint.y.toString()) }
        if (fatGraph != null) {
            fatGraph.removeAllSeries()
            fatGraph.addSeries(dataSeries)
            fatGraph.viewport.isXAxisBoundsManual = true
            fatGraph.viewport.setMinX(dataSeries.lowestValueX)
            fatGraph.viewport.setMaxX(dataSeries.highestValueX)
        }
    }

    private fun saveBodyFat(fatPercentage: Float) {
        val account = getAccount(activity as Context)
        val cal = Calendar.getInstance()
        val now = Date()
        cal.time = now
        val endTime = cal.timeInMillis
        cal.add(Calendar.SECOND, -1)
        val startTime = cal.timeInMillis
        val dataSource: DataSource = DataSource.Builder()
            .setAppPackageName(activity as Context)
            .setDataType(DataType.TYPE_BODY_FAT_PERCENTAGE)
            .setStreamName("$TAG - body fat")
            .setType(DataSource.TYPE_RAW)
            .build()
        val dataSet: DataSet = DataSet.create(dataSource)
        val dataPoint: DataPoint = dataSet.createDataPoint().setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
        dataPoint.getValue(Field.FIELD_PERCENTAGE).setFloat(fatPercentage)
        dataSet.add(dataPoint)
        Log.d(TAG, "DataSet for saving: $dataSet")
        Fitness.getHistoryClient(activity as Activity, account)
            .insertData(dataSet)
            .addOnSuccessListener { Log.i(TAG, "Data inserted: $dataSet") }
            .addOnFailureListener { exception -> Log.e(TAG, exception.message)
                Log.e(TAG, "Unable to insert data: $dataSet") }
            .addOnCanceledListener { Log.i(TAG, "saving data was cancelled.") }
    }
}
