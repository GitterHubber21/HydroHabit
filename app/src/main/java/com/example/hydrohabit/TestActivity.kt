package com.example.hydrohabit

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class TestActivity : AppCompatActivity() {
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        val url = "http://coolcoder.hackclub.dev/"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    findViewById<TextView>(R.id.testResponse).text = "Failed to connect"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let {
                    val message = JSONObject(it).getString("color")
                    runOnUiThread {
                        findViewById<TextView>(R.id.testResponse).text = message
                    }
                }
            }
        })
        val backArrow: ImageView=findViewById(R.id.backArrow)

        backArrow.setOnClickListener {
            startActivity(Intent(applicationContext, InsightsActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
    }
}
