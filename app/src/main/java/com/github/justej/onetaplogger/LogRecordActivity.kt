package com.github.justej.onetaplogger

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem

class LogRecordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_record)

        // TODO: on long click listeners

//        viewHolder.view.setOnLongClickListener {
//            val logRecordFields = (it as TextView).text.toString().split("\n", limit = 3)
//            AlertDialog.Builder(context)
//                    .setTitle(String.format(context.getString(R.string.LogRecordFormatter), logRecordFields[0], logRecordFields[1]))
//                    .setItems(R.array.ActionsOnLogRecord, { _, id ->
//                        AlertDialog.Builder(context)
//                                .setTitle("Info")
//                                .setMessage("$id : ${context.resources.getStringArray(R.array.ActionsOnLogRecord)[id]}")
//                                .show()
//                    })
//                    .show()
//            // return type should be boolean...
//            true
//        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (android.R.id.home == item?.itemId) {
            this.finish()
            return true
        }
        return false
    }
}
