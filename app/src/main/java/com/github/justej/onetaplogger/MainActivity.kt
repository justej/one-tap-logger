package com.github.justej.onetaplogger

import android.arch.core.util.Function
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private var sleepLogData: SleepLogData? = null

    companion object {
        val singeLineFormatter = Function<String, String> { it.replace("\r\n", " ").replace("\r", " ").replace("\n", " ") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<FloatingActionButton>(R.id.sleep_fab).setOnClickListener { storeActionTime(it) }
        findViewById<FloatingActionButton>(R.id.wake_up_fab).setOnClickListener { storeActionTime(it) }
        findViewById<TextView>(R.id.last_action_text_view).setOnClickListener {
            val intent = Intent(this, LogViewActivity::class.java)
            startActivity(intent)
        }
        val latest = SleepLogDatabase.getInstance(this).sleepLogDao().get(1, 0)
        sleepLogData = if (latest.isEmpty()) null else latest[0]
        findViewById<TextView>(R.id.last_action_text_view).setOnLongClickListener(
                LogViewAdapter.startLogRecordActivityOnLongClickListener(this, { sleepLogData }))

        LogViewAdapter.updateActionView(this, sleepLogData,
                findViewById(last_action_text_view.id), MainActivity.singeLineFormatter,
                getString(R.string.LastActionFormatter))
    }

    // TODO: extract "Last Action:" to a separate TextView
    private fun storeActionTime(view: View) {
        try {
            val timestamp = Date().time
            if (view !is FloatingActionButton) {
                return
            }
            val label = when (view.id) {
                R.id.sleep_fab -> getString(R.string.Sleep)
                R.id.wake_up_fab -> getString(R.string.WakeUp)
                else -> return
            }
            sleepLogData = SleepLogData(null, timestamp, label)
            SleepLogDatabase.getInstance(this).sleepLogDao().insert(sleepLogData!!)
            LogViewAdapter.updateActionView(this, sleepLogData,
                    findViewById(last_action_text_view.id), MainActivity.singeLineFormatter,
                    getString(R.string.LastActionFormatter))
        } catch (e: Exception) {
            Log.e(this::class.qualifiedName, "Generic error", e)
        }
    }
}
