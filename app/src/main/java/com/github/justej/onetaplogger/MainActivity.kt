package com.github.justej.onetaplogger

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fabSleep: FloatingActionButton = findViewById(R.id.fabSleep)
        val fabWakeUp: FloatingActionButton = findViewById(R.id.fabWakeUp)

        fabSleep.setOnClickListener { onClickListener(it) }
        fabWakeUp.setOnClickListener { onClickListener(it) }

        val db = SleepLogDatabase.getInstance(this)
        val latest = db?.sleepLogDao()?.get(1, 0)
        updateLastAction(if (latest!!.isEmpty()) null else latest[0])
    }

    private fun onClickListener(view: View) {
        storeActionTime(view)
    }

    private fun storeActionTime(view: View) {
        try {
            val db = SleepLogDatabase.getInstance(this)
            val timestamp = Date().time
            if (view !is FloatingActionButton) {
                return
            }
            val label = when (view.id) {
                R.id.fabSleep -> getString(R.string.Sleep)
                R.id.fabWakeUp -> getString(R.string.WakeUp)
                else -> return
            }
            db?.sleepLogDao()?.insert(SleepLogData(null, timestamp, label))
            updateLastAction(SleepLogData(null, timestamp, label))
        } catch (e: Exception) {
            Log.e(MainActivity::class.qualifiedName, "Generic error", e)
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun updateLastAction(latest: SleepLogData?) {
        val lastAction = findViewById<TextView>(R.id.lastActionLabel)
        if (latest == null) {
            lastAction.text = String.format(getString(R.string.LastAction), getString(R.string.NoData))
        } else {
            val (_, timestamp, label, comment) = latest
            val formattedTime = SimpleDateFormat("dd/MM/yyyy hh:mm:ss").format(timestamp)
            val text = String.format(getString(R.string.LastAction), "$label\n$formattedTime\n$comment")
            lastAction.text = text
        }
    }
}
