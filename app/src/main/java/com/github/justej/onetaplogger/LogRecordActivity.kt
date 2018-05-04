package com.github.justej.onetaplogger

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.MenuItem
import android.widget.DatePicker
import android.widget.EditText
import android.widget.TextView
import com.github.justej.onetaplogger.LogViewAdapter.Companion.getLabelColor
import java.util.*

private const val START_OF_EPOCH = 1900

// TODO: add "Undo" button (exit without saving changes)
class LogRecordActivity : AppCompatActivity() {
    private var commentChanged = false
    private lateinit var newDate: Date
    private lateinit var initialDate: Date
    private var secondsAndMilliseconds: Long = 0
    private lateinit var commentView: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_record)

        val dateView: TextView = findViewById(R.id.date_text_view)
        val timeView: TextView = findViewById(R.id.time_text_view)
        commentView = findViewById(R.id.comment_edit_text)

        val timestamp: Long = intent.getLongExtra(getString(R.string.extras_timestamp), 0)
        val comment: String = intent.getStringExtra(getString(R.string.extras_comment))
        newDate = Date(timestamp)
        initialDate = Date(timestamp)
        secondsAndMilliseconds = initialDate.time - Date(initialDate.year, initialDate.month, initialDate.date, initialDate.hours, initialDate.minutes).time

        title = intent.getStringExtra(getString(R.string.extras_label))
        val color = getLabelColor(this, title)
        val style = when (title) {
            getString(R.string.label_sleep) -> R.style.datepicker_sleep
            getString(R.string.label_wake_up) -> R.style.datepicker_wake_up
            else -> {
                AlertDialog.Builder(this)
                        .setTitle("Incorrect label '$title'")
                        .setPositiveButton(android.R.string.ok, { _, _ -> finish() })
                        .show()
                return
            }
        }

        with(dateView) {
            text = DateFormat.getDateFormat(this@LogRecordActivity).format(timestamp)
            setBackgroundColor(color)
            setOnClickListener {
                val datePickerDialog = DatePickerDialog(
                        this@LogRecordActivity,
                        style,
                        { datePicker: DatePicker, year: Int, month: Int, day: Int ->
                            newDate = Date(year - START_OF_EPOCH, month, day, newDate.hours, newDate.minutes)
                            text = DateFormat.getDateFormat(this@LogRecordActivity).format(newDate.time)
                        },
                        newDate.year + START_OF_EPOCH,
                        newDate.month,
                        newDate.date
                )
                datePickerDialog.show()
            }
        }
        with(timeView) {
            text = DateFormat.getTimeFormat(this@LogRecordActivity).format(timestamp)
            setBackgroundColor(color)
            setOnClickListener {
                val timePickerDialog = TimePickerDialog(
                        this@LogRecordActivity,
                        style,
                        { timePicker, hours, minutes ->
                            newDate = Date(newDate.year, newDate.month, newDate.date, hours, minutes)
                            text = DateFormat.getTimeFormat(this@LogRecordActivity).format(newDate.time)
                        },
                        newDate.hours,
                        newDate.minutes,
                        DateFormat.is24HourFormat(context)
                )
                timePickerDialog.show()
            }
        }
        with(commentView) {
            setText(comment, TextView.BufferType.EDITABLE)
            addTextChangedListener(
                    object : TextWatcher {
                        override fun afterTextChanged(p0: Editable?) {
                            commentChanged = true
                        }

                        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                            // do nothing
                        }

                        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                            // do nothing
                        }
                    }
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (android.R.id.home == item?.itemId) {
            if (newDate != initialDate || commentChanged) {
                updateRecordInDb()
                finish()
            } else {
                finish()
            }
            return true
        }
        return false
    }

    private fun updateRecordInDb() {
        val dao = SleepLogDatabase.getInstance(this).sleepLogDao()
        val newRecord = SleepLogData(newDate.time, title.toString(), commentView.text.toString())
        if (newDate.time == initialDate.time) {
            dao.update(newRecord)
        } else {
            dao.insert(newRecord)
            dao.delete(initialDate.time)
        }
    }
}
