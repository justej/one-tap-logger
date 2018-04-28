package com.github.justej.onetaplogger

import android.arch.core.util.Function
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.format.DateFormat
import android.util.Log
import android.util.LruCache
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import java.lang.Math.max

class LogViewActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var colorMap: Map<String?, Int>
    private var datasetSize: Int = 0
    private val dataset = LruCache<Int, SleepLogData>(LIMIT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_view)

        initDataset()
        colorMap = mapOf(
                Pair(getString(R.string.Sleep), getColor(R.color.colorSleep)),
                Pair(getString(R.string.WakeUp), getColor(R.color.colorWakeUp)),
                Pair(null, getColor(R.color.colorBackground))
        )

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

    companion object {
        const val LIMIT = 100
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
            val offset = max(position - LogViewActivity.LIMIT / 2, 0)
            val data = SleepLogDatabase.getInstance(context).sleepLogDao().get(LogViewActivity.LIMIT, offset)
            (0 until data.size).forEach { dataset.put(it + offset, data[it]) }
        }

        holder.view.setOnLongClickListener {
            try {
                val intent = Intent(context, LogRecordActivity::class.java)
                with(intent) {
                    putExtra("label", dataset[position].label)
                    putExtra("timestamp", dataset[position].timestamp)
                    putExtra("comment", dataset[position].comment)

                }
                context.startActivity(intent)
                true

            } catch (e: Exception) {
                Log.e(this::class.qualifiedName, "Exception when calling intent ${LogRecordActivity::class.java.canonicalName}: $e")
                false
            }

        }

        updateActionView(context, dataset[position], holder.view)
    }

    companion object {
        fun updateActionView(
                context: Context,
                sleepLogData: SleepLogData?,
                view: TextView,
                commentFormatter: Function<String, String> = Function { it },
                formatter: String = "%s"
        ) {
            if (sleepLogData == null) {
                view.text = context.getString(R.string.NoData)
                view.setBackgroundColor(context.getColor(R.color.colorBackground))
            } else {
                val (_, timestamp, label, comment) = sleepLogData
                val labelColor = when (label) {
                    context.getString(R.string.Sleep) -> context.getColor(R.color.colorSleep)
                    context.getString(R.string.WakeUp) -> context.getColor(R.color.colorWakeUp)
                    else -> context.getColor(R.color.colorBackground)
                }
                val formattedTimestamp = "${DateFormat.getDateFormat(context).format(timestamp)} ${DateFormat.getTimeFormat(context).format(timestamp)}"
                val text = String.format(formatter, "$label\n$formattedTimestamp\n${commentFormatter.apply(comment)}")
                view.text = text
                view.setBackgroundColor(labelColor)
            }
        }
    }

    class ViewHolder(val view: TextView) : RecyclerView.ViewHolder(view)
}
