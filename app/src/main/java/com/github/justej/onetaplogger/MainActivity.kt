package com.github.justej.onetaplogger

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val db = SleepLogDatabase.getInstance(this)
        val latest = db?.sleepLogDao()?.get(1, 0)
        updateLastAction(if (latest!!.isEmpty()) null else latest[0])
    }

    fun storeActionTime(view: View) {
        try {
            if (view is Button) {
                val db = SleepLogDatabase.getInstance(this)
                val timestamp = Date().time
                db?.sleepLogDao()?.insert(SleepLogData(null, timestamp, view.text.toString()))
                updateLastAction(SleepLogData(null, timestamp, view.text.toString()))
            }
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
