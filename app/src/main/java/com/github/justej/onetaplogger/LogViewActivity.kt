package com.github.justej.onetaplogger

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import java.text.SimpleDateFormat

class LogViewActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var dataset: List<SleepLogData>
    private lateinit var colorMap: Map<String?, Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_view)

        dataset = initDataset()
        colorMap = mapOf(
                Pair(getString(R.string.Sleep), getColor(R.color.colorSleep)),
                Pair(getString(R.string.WakeUp), getColor(R.color.colorWakeUp)),
                Pair(null, getColor(R.color.colorBackground))
        )

        viewManager = LinearLayoutManager(this)
        viewAdapter = LogViewAdapter(colorMap, dataset)

        recyclerView = findViewById<RecyclerView>(R.id.log_recycler_view).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }

    private fun initDataset(): List<SleepLogData> =
            SleepLogDatabase.getInstance(this)?.sleepLogDao()?.get() ?: emptyList()
}

class LogViewAdapter(private val colorMap: Map<String?, Int>, private val myDataset: List<SleepLogData>) :
        RecyclerView.Adapter<LogViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val logViewRecord = LayoutInflater.from(parent.context)
                .inflate(R.layout.multiline_text_view, parent, false) as EditText

        return ViewHolder(logViewRecord)
    }

    override fun getItemCount() = myDataset.size

    @SuppressLint("SimpleDateFormat")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (_, timestamp, label, multilineComment) = myDataset[position]
        val comment = multilineComment.replace("\r\n", " ")
                .replace('\r', ' ')
                .replace('\n', ' ')
        val formattedTime = SimpleDateFormat("dd/MM/yyyy hh:mm:ss").format(timestamp)
        val text = "$label\n$formattedTime\n$comment"
        holder.editText.text = text
        colorMap[label]?.let { holder.editText.setBackgroundColor(it) }
    }

    class ViewHolder(val editText: TextView) : RecyclerView.ViewHolder(editText)
}
