package com.github.justej.onetaplogger

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private var sleepLogData: SleepLogData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<FloatingActionButton>(R.id.sleep_fab).setOnClickListener { storeActionTime(it) }
        findViewById<FloatingActionButton>(R.id.wake_up_fab).setOnClickListener { storeActionTime(it) }
        findViewById<TextView>(R.id.last_action).setOnClickListener {
            val intent = Intent(this, LogViewActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        try {
            super.onResume()

            val latest = SleepLogDatabase.getInstance(this).sleepLogDao().get(1, 0)
            sleepLogData = if (latest.isEmpty()) null else latest[0]
            findViewById<TextView>(R.id.last_action).setOnLongClickListener(
                    LogViewAdapter.startLogRecordActivityOnLongClickListener(this, { sleepLogData }))

            LogViewAdapter.updateActionView(this, sleepLogData, findViewById(last_action.id))
        } catch (e: Exception) {
            Log.e(this::class.qualifiedName, "Oops!", e)
            showFatalErrorDialog(e)
        }
    }

    private fun showFatalErrorDialog(e: Exception) {
        AlertDialog.Builder(this)
                .setTitle("Fatal error")
                .setMessage(String.format(getString(R.string.message_main_activity_fatal_error), e))
                .setPositiveButton(android.R.string.ok, { _: DialogInterface, _: Int -> finish() })
                .show()
    }

    private fun storeActionTime(view: View) {
        try {
            val timestamp = Date().time
            if (view !is FloatingActionButton) {
                return
            }
            val label = when (view.id) {
                R.id.sleep_fab -> getString(R.string.label_sleep)
                R.id.wake_up_fab -> getString(R.string.label_wake_up)
                else -> return
            }
            sleepLogData = SleepLogData(timestamp, label)
            SleepLogDatabase.getInstance(this).sleepLogDao().insert(sleepLogData!!)
            LogViewAdapter.updateActionView(this, sleepLogData, findViewById(last_action.id))
        } catch (e: Exception) {
            Log.e(this::class.qualifiedName, "Generic error", e)
        }
    }
}
