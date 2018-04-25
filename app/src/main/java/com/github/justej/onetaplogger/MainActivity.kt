package com.github.justej.onetaplogger

import android.arch.core.util.Function
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.EditText
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        val singeLineFormatter = Function<String, String> { it.replace("\r\n", " ").replace("\r", " ").replace("\n", " ") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initListeners()

        val latest = SleepLogDatabase.getInstance(this).sleepLogDao().get(1, 0)
        LogViewAdapter.updateActionView(this, if (latest.isEmpty()) null else latest[0],
                findViewById(lastActionTextView.id), MainActivity.singeLineFormatter,
                getString(R.string.LastActionFormatter))
    }

    private fun initListeners() {
        val fabSleep: FloatingActionButton = findViewById(R.id.sleepFAB)
        val fabWakeUp: FloatingActionButton = findViewById(R.id.wakeUpFAB)
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
            val timestamp = Date().time
            if (view !is FloatingActionButton) {
                return
            }
            val label = when (view.id) {
                R.id.sleepFAB -> getString(R.string.Sleep)
                R.id.wakeUpFAB -> getString(R.string.WakeUp)
                else -> return
            }
            SleepLogDatabase.getInstance(this).sleepLogDao().insert(SleepLogData(null, timestamp, label))
            LogViewAdapter.updateActionView(this, SleepLogData(null, timestamp, label),
                    findViewById(lastActionTextView.id), MainActivity.singeLineFormatter,
                    getString(R.string.LastActionFormatter))
        } catch (e: Exception) {
            Log.e(MainActivity::class.qualifiedName, "Generic error", e)
        }
    }
}
