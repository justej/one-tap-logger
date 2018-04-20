package com.github.justej.onetaplogger

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initListeners()

        val db = SleepLogDatabase.getInstance(this)
        val latest = db?.sleepLogDao()?.get(1, 0)
        updateLastAction(if (latest!!.isEmpty()) null else latest[0])
    }

    private fun initListeners() {
        val fabSleep: FloatingActionButton = findViewById(R.id.fabSleep)
        val fabWakeUp: FloatingActionButton = findViewById(R.id.fabWakeUp)
        fabSleep.setOnClickListener { storeActionTime(it) }
        fabWakeUp.setOnClickListener { storeActionTime(it) }

        val lastAction: EditText = findViewById(R.id.lastActionTextView)
        lastAction.setOnClickListener {
            val intent = Intent(this, LogViewActivity::class.java)
            startActivity(intent)
        }
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
        val lastAction = findViewById<TextView>(R.id.lastActionTextView)
        if (latest == null) {
            lastAction.text = String.format(getString(R.string.LastAction), getString(R.string.NoData))
        } else {
            val (_, timestamp, label, multilineComment) = latest
            val comment = multilineComment.replace("\r\n", " ")
                    .replace('\r', ' ')
                    .replace('\n', ' ')
            val labelColor = when (label) {
                getString(R.string.Sleep) -> getColor(R.color.colorSleep)
                getString(R.string.WakeUp) -> getColor(R.color.colorWakeUp)
                else -> getColor(R.color.colorBackground)
            }
            val formattedTime = SimpleDateFormat("dd/MM/yyyy hh:mm:ss").format(timestamp)
            val text = String.format(getString(R.string.LastAction), "$label\n$formattedTime\n$comment")
            lastAction.text = text
            lastAction.setBackgroundColor(labelColor)
        }
    }
}
