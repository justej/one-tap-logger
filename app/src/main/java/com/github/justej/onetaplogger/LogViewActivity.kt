package com.github.justej.onetaplogger

import android.arch.core.util.Function
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

private const val LIMIT = 100

// TODO: show time elapsed between consequent actions
class LogViewActivity : AppCompatActivity() {

    private val dataset = LruCache<Int, SleepLogData>(LIMIT)
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private var datasetSize: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_view)

        initDataset()
        viewManager = LinearLayoutManager(this)
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
                .inflate(R.layout.multiline_text_view, parent, false) as TextView

        return ViewHolder(logViewRecord)
    }

    override fun getItemCount() = datasetSize

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (dataset[position] == null) {
            val offset = max(position - LIMIT / 2, 0)
            val data = SleepLogDatabase.getInstance(context).sleepLogDao().get(LIMIT, offset)
            (0 until data.size).forEach { dataset.put(it + offset, data[it]) }
        }

        holder.view.setOnClickListener(startLogRecordActivityOnClickListener(context, dataset[position]))
        // TODO: offer to delete record on-long-click

        updateActionView(context, dataset[position], holder.view)
    }

    companion object {
        val startLogRecordActivityOnClickListener =
                { context: Context, data: SleepLogData? ->
                    View.OnClickListener {
                        onClickHandler(data, context)
                    }
                }
        val startLogRecordActivityOnLongClickListener =
                { context: Context, dataProvider: () -> SleepLogData? ->
                    View.OnLongClickListener {
                        if (onClickHandler(dataProvider.invoke(), context)) return@OnLongClickListener true
                        true
                    }
                }

        private fun onClickHandler(data: SleepLogData?, context: Context): Boolean {
            try {
                if (data == null) {
                    AlertDialog.Builder(context)
                            .setTitle(R.string.NoData)
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
                Log.e(this::class.qualifiedName, "Exception when calling intent ${LogRecordActivity::class.java.canonicalName}: $e")
                AlertDialog.Builder(context)
                        .setTitle(R.string.NoData)
                        .setMessage(R.string.Error)
                        .show()
            }
            return false
        }

        fun getLabelColor(context: Context, label: CharSequence): Int {
            return when (label) {
                context.getString(R.string.Sleep) -> context.getColor(R.color.sleep)
                context.getString(R.string.WakeUp) -> context.getColor(R.color.wake_up)
                else -> context.getColor(R.color.background)
            }
        }

        fun updateActionView(
                context: Context,
                sleepLogData: SleepLogData?,
                view: TextView,
                commentFormatter: Function<String, String> = Function { it },
                formatter: String = "%s"
        ) {
            if (sleepLogData == null) {
                view.text = context.getString(R.string.NoData)
                view.setBackgroundColor(context.getColor(R.color.background))
            } else {
                val (_, timestamp, label, comment) = sleepLogData
                val formattedTimestamp = "${DateFormat.getDateFormat(context).format(timestamp)} ${DateFormat.getTimeFormat(context).format(timestamp)}"
                view.text = String.format(formatter, "$label\n$formattedTimestamp\n${commentFormatter.apply(comment)}")
                view.setBackgroundColor(getLabelColor(context, label))
            }
        }

    }

    class ViewHolder(val view: TextView) : RecyclerView.ViewHolder(view)
}
