package com.github.justej.onetaplogger

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.format.DateFormat
import android.util.Log
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.lang.Math.max
import java.util.concurrent.TimeUnit

private const val LIMIT = 50
private val MINUTES_PER_HOUR = TimeUnit.HOURS.toMinutes(1)
private val HOURS_PER_DAY = TimeUnit.DAYS.toHours(1)

// TODO: restore list position on-resume (e.g. after record editing)
class LogViewActivity : AppCompatActivity() {

    private val dataset = LruCache<Int, SleepLogData>(LIMIT)
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private var datasetSize: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_view)
        viewManager = LinearLayoutManager(this)
    }

    override fun onResume() {
        super.onResume()
        initDataset()
        viewAdapter = LogViewAdapter(this, dataset, datasetSize)

        recyclerView = findViewById<RecyclerView>(R.id.log_recycler_view).apply {
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }

    private fun initDataset() {
        val data = SleepLogDatabase.getInstance(this).sleepLogDao().get(LIMIT)
        (0 until data.size).forEach { dataset.put(it, data[it]) }
        datasetSize = SleepLogDatabase.getInstance(this).sleepLogDao().count()
    }
}

class LogViewAdapter(
        private val context: Context,
        private val dataset: LruCache<Int, SleepLogData>,
        private val datasetSize: Int
) : RecyclerView.Adapter<LogViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val logViewRecord = LayoutInflater.from(parent.context)
                .inflate(R.layout.log_record_layout, parent, false) as ViewGroup

        return ViewHolder(logViewRecord)
    }

    override fun getItemCount() = datasetSize

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var data: SleepLogData? = dataset[position]
        var dataPrev: SleepLogData? = dataset[position + 1]

        if (data == null || dataPrev == null) {
            val offset = max(position - LIMIT / 2, 0)
            val dataChunk = SleepLogDatabase.getInstance(context).sleepLogDao().get(LIMIT, offset)
            (0 until dataChunk.size).forEach { dataset.put(it + offset, dataChunk[it]) }
            data = dataset[position]
            dataPrev = dataset[position + 1]
        }

        holder.viewGroup.setBackgroundColor(getLabelColor(context, data))
        holder.viewGroup.setOnClickListener(startLogRecordActivityOnClickListener(context, data))
        // TODO: offer to delete record on-long-click

        updateActionView(context, holder.timestamp, data)
        updateElapsedView(context, holder.elapsed, data, dataPrev)
    }

    private fun updateElapsedView(context: Context, view: TextView, data: SleepLogData?, dataPrev: SleepLogData?) {
        if (data == null || dataPrev == null) {
            view.text = context.getString(R.string.message_no_data)
        } else {
            // TODO: extract to utility class
            val diff = data.timestamp - dataPrev.timestamp
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff) % HOURS_PER_DAY
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % MINUTES_PER_HOUR
            var diffStr = (if (days != 0L) days.toString() + context.getString(R.string.label_abbrev_day) + " " else "") +
                    (if (hours != 0L) hours.toString() + context.getString(R.string.label_abbrev_hour) + " " else "") +
                    (if (minutes != 0L) minutes.toString() + context.getString(R.string.label_abbrev_minute) + " " else "")
            if (diffStr.isEmpty()) {
                diffStr = "0 " + context.getString(R.string.label_abbrev_minute)
            }
            // TODO: extract to utility class
            val labelId: Int = when (data.label) {
                context.getString(R.string.label_sleep) -> R.string.label_awaken
                context.getString(R.string.label_wake_up) -> R.string.label_asleep
                else -> R.string.label_empty
            }
            view.text = String.format(context.getString(R.string.formatter_space_separated_strings), diffStr, context.getString(labelId))
        }
    }

    companion object {
        private val startLogRecordActivityOnClickListener: (Context, SleepLogData?) -> View.OnClickListener =
                { context: Context, data: SleepLogData? ->
                    View.OnClickListener {
                        onClickHandler(data, context)
                    }
                }
        val startLogRecordActivityOnLongClickListener =
                { context: Context, dataProvider: () -> SleepLogData? ->
                    View.OnLongClickListener {
                        onClickHandler(dataProvider.invoke(), context)
                        true
                    }
                }

        private fun onClickHandler(data: SleepLogData?, context: Context): Boolean {
            try {
                if (data == null) {
                    AlertDialog.Builder(context)
                            .setTitle(R.string.title_no_data)
                            .setMessage(R.string.message_no_data)
                            .show()
                    return true
                }
                val intent = Intent(context, LogRecordActivity::class.java)
                with(intent) {
                    putExtra(context.getString(R.string.extras_label), data.label)
                    putExtra(context.getString(R.string.extras_timestamp), data.timestamp)
                    putExtra(context.getString(R.string.extras_comment), data.comment)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(this::class.qualifiedName, "Exception while calling intent ${LogRecordActivity::class.java.canonicalName}: $e")
                AlertDialog.Builder(context)
                        .setTitle(R.string.message_no_data)
                        .setMessage(R.string.message_error)
                        .show()
            }
            return false
        }

        // TODO: extract to utility class
        internal fun getLabelColor(context: Context, sleepLogData: SleepLogData?): Int {
            if (sleepLogData == null) {
                return context.getColor(R.color.background)
            }

            return when (sleepLogData.label) {
                context.getString(R.string.label_sleep) -> context.getColor(R.color.sleep)
                context.getString(R.string.label_wake_up) -> context.getColor(R.color.wake_up)
                else -> context.getColor(R.color.background)
            }
        }

        internal fun updateActionView(context: Context, view: TextView, sleepLogData: SleepLogData?) {
            if (sleepLogData == null) {
                view.text = context.getString(R.string.message_no_data)
            } else {
                val formattedTimestamp = "${DateFormat.getDateFormat(context).format(sleepLogData.timestamp)} ${DateFormat.getTimeFormat(context).format(sleepLogData.timestamp)}"
                view.text = String.format(context.getString(R.string.formatter_log_record), sleepLogData.label, formattedTimestamp, sleepLogData.comment)
            }
        }
    }

    class ViewHolder(internal val viewGroup: ViewGroup) : RecyclerView.ViewHolder(viewGroup) {
        internal val timestamp: TextView = viewGroup.findViewById(R.id.timestamp)
        internal val elapsed: TextView = viewGroup.findViewById(R.id.elapsed_time)
    }
}
