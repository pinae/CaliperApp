package net.pinae.caliperapp

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.*
import com.google.android.gms.fitness.request.DataDeleteRequest
import com.google.android.gms.fitness.request.DataReadRequest
import kotlinx.android.synthetic.main.fragment_fat_history.*
import kotlinx.android.synthetic.main.fragment_fat_history.view.*
import java.util.concurrent.TimeUnit
import com.google.android.gms.fitness.result.DataReadResponse
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter
import com.jjoe64.graphview.series.DataPoint as GVDataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.text.DateFormat.getTimeInstance
import java.util.*


data class FatReading(val date: Long, val value: Float)

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [FatHistoryFragment.OnFatHistoryFragmentValueSelected] interface
 * to handle interaction events.
 * Use the [FatHistoryFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class FatHistoryFragment : TopFragment() {
    private var listener: OnFatHistoryFragmentValueSelected? = null
    private var fatHistory: List<FatReading> = arrayListOf()

    interface OnFatHistoryFragmentValueSelected {
        fun onFatHistoryValueSelected(value: FatReading?)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        val view: View = inflater.inflate(R.layout.fragment_fat_history, container, false)
        view.fatGraph.viewport.isScalable = true
        view.fatGraph.viewport.isScrollable = true
        view.fatGraph.gridLabelRenderer.labelFormatter = DateAsXAxisLabelFormatter(activity)
        view.fatGraph.gridLabelRenderer.numHorizontalLabels = 3
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFatHistoryFragmentValueSelected) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFatHistoryFragmentValueSelected")
        }
    }

    override fun onResume() {
        super.onResume()
        loadBodyFatData()
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
         * @return A new instance of fragment FatHistoryFragment.
         */
        @JvmStatic
        fun newInstance() =
            FatHistoryFragment().apply {
                arguments = Bundle().apply {}
            }
    }

    private fun subscribeToBodyFat() {
        val account = getAccount(activity as Context)
        Log.d("GSignIn account", account.toString())
        val recordingClient = Fitness.getRecordingClient(activity as Activity, account)
        recordingClient.listSubscriptions(DataType.TYPE_BODY_FAT_PERCENTAGE).addOnSuccessListener {
                subscriptions -> if (subscriptions.isEmpty())
                                   recordingClient.subscribe(DataType.TYPE_BODY_FAT_PERCENTAGE)
                                       .addOnSuccessListener {
                                               Log.i("Fitness API", "Subscribed to TYPE_BODY_FAT_PERCENTAGE")
                                       }
                                       .addOnFailureListener {
                                               exception -> Log.e("FitnessAPI error", exception.message)
                                       }
        }
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
        val endTime = cal.timeInMillis
        cal.add(Calendar.WEEK_OF_YEAR, -12)
        val startTime = cal.timeInMillis
        val readRequest: DataReadRequest = DataReadRequest.Builder()
            .read(DataType.TYPE_BODY_FAT_PERCENTAGE)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()
        Fitness.getHistoryClient(activity as Activity, account)
            .readData(readRequest)
            .addOnSuccessListener { response -> bodyFatDataLoaded(response) }
            .addOnFailureListener { exception -> Log.e(TAG, "Data load failed: " + exception.message ) }
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
            dataSeries.appendData(GVDataPoint(Date(entry.date), entry.value.toDouble()), true, 500)
        }
        dataSeries.isDrawDataPoints = true
        dataSeries.dataPointsRadius = 10.0f
        dataSeries.thickness = 7
        dataSeries.setAnimated(true)
        dataSeries.setOnDataPointTapListener { _, dataPoint -> if (listener != null) {
                var foundReading: FatReading? = null
                for (entry in fatHistory) {
                    val tmpGvDp = GVDataPoint(Date(entry.date), entry.value.toDouble())
                    if (tmpGvDp.x - 1e-5 <= dataPoint.x && dataPoint.x <= tmpGvDp.x + 1e-5 &&
                        tmpGvDp.y - 1e-5 <= dataPoint.y && dataPoint.y <= tmpGvDp.y + 1e-5)
                        foundReading = entry
                }
                if (foundReading != null) (listener as OnFatHistoryFragmentValueSelected).onFatHistoryValueSelected(
                    foundReading
                )
            }
            Log.d("dp", dataPoint.x.toString() + ", " + dataPoint.y.toString()) }
        if (fatGraph != null) {
            fatGraph.removeAllSeries()
            fatGraph.addSeries(dataSeries)
            fatGraph.viewport.isXAxisBoundsManual = true
            fatGraph.viewport.setMinX(dataSeries.lowestValueX)
            fatGraph.viewport.setMaxX(dataSeries.highestValueX)
        }
    }

    override fun setFatMeasurementNow(fatPercentage: Float) {
        saveBodyFat(fatPercentage)
    }

    private fun saveBodyFat(fatPercentage: Float) {
        val account = getAccount(activity as Context)
        val cal = Calendar.getInstance()
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

    fun deleteDataPoint(dataPoint: FatReading) {
        val account = getAccount(activity as Context)
        val cal = Calendar.getInstance()
        cal.timeInMillis = dataPoint.date
        cal.add(Calendar.MILLISECOND, 10)
        val endTime = cal.timeInMillis
        cal.add(Calendar.MILLISECOND, -20)
        val startTime = cal.timeInMillis
        val readRequest: DataReadRequest = DataReadRequest.Builder()
            .read(DataType.TYPE_BODY_FAT_PERCENTAGE)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()
        Fitness.getHistoryClient(activity as Activity, account)
            .readData(readRequest)
            .addOnSuccessListener { response -> if (response.dataSets == null || response.dataSets.isEmpty()) {
                    Log.e(TAG, "No entry found for this dataPoint")
                } else {
                    for (dataSet in response.dataSets) {
                        dumpDataSet(dataSet)
                        for (dp in dataSet.dataPoints) {
                            var foundValue: Float? = null
                            for (field in dp.dataType.fields) {
                                if (field.name == "percentage") foundValue = dp.getValue(field).asFloat()
                            }
                            if (foundValue != null) {
                                deleteVerifiedDataPoint(dataSet, dp, dataPoint)
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { exception -> Log.e(TAG, "Data load failed: " + exception.message ) }
    }

    private fun deleteVerifiedDataPoint(dataSet: DataSet, dataPoint: DataPoint, fatReading: FatReading) {
        val account = getAccount(activity as Context)
        val deleteRequest :DataDeleteRequest = DataDeleteRequest.Builder()
            .addDataType(dataPoint.dataType)
            .addDataSource(dataSet.dataSource)
            .setTimeInterval(
                dataPoint.getStartTime(TimeUnit.MILLISECONDS),
                dataPoint.getEndTime(TimeUnit.MILLISECONDS),
                TimeUnit.MILLISECONDS)
            .build()
        Fitness.getHistoryClient(activity as Activity, account)
            .deleteData(deleteRequest)
            .addOnSuccessListener { Log.i(TAG, "Data deleted: $dataPoint")
                for (entry in fatHistory) {
                    if (entry.date == fatReading.date && entry.value == fatReading.value)
                        fatHistory = fatHistory.subList(0, fatHistory.indexOf(entry)) +
                                fatHistory.subList(fatHistory.indexOf(entry) + 1, fatHistory.count())
                }
                if (listener != null) listener!!.onFatHistoryValueSelected(null)
                updateFatDiagram()
                loadBodyFatData() }
            .addOnFailureListener { exception -> Log.e(TAG, exception.message)
                Log.e(TAG, "Unable to delete data: $dataPoint") }
            .addOnCanceledListener { Log.i(TAG, "saving data was cancelled.") }
    }
}
