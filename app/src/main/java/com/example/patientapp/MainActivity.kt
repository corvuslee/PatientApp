package com.example.patientapp

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity(),
    AdapterView.OnItemSelectedListener {
    var sunSign = "100001"
    var resultView: TextView? = null
    private lateinit var mQrResultLauncher : ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var buttonView: Button = findViewById(R.id.openCamera)
        resultView = findViewById(R.id.resultView)

        // QR Code
        mQrResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if(it.resultCode == Activity.RESULT_OK) {
                val result = IntentIntegrator.parseActivityResult(it.resultCode, it.data)

                if(result.contents != null) {
                    // Do something with the contents (this is usually a URL)
                    sunSign = result.contents
                    GlobalScope.async {
                        getPredictions(buttonView)
                    }
                }
            }
        }

        // Fire up the camera to scan QR code
        val button: Button = findViewById(R.id.openCamera)
        button.setOnClickListener {
            startScanner()
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        sunSign = "000000"
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (parent != null) {
            sunSign = parent.getItemAtPosition(position).toString()
        }
    }

    public suspend fun getPredictions(view: android.view.View) {
        try {
            val result = GlobalScope.async {
                callAztroAPI("https://asdfasdf.execute-api.eu-west-1.amazonaws.com/get_patient_details?patient_id=" + sunSign)
            }.await()

            onResponse(result)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun callAztroAPI(apiUrl: String): String? {
        var result: String? = ""
        val url: URL;
        var connection: HttpURLConnection? = null
        try {
            url = URL(apiUrl)
            connection = url.openConnection() as HttpURLConnection
            // set headers for the request
            // set host name
            // connection.setRequestProperty("x-rapidapi-host", "sameer-kumar-aztro-v1.p.rapidapi.com")

            // set the rapid-api key
            // connection.setRequestProperty("x-rapidapi-key", "<YOUR_RAPIDAPI_KEY>")
            // connection.setRequestProperty("content-type", "application/x-www-form-urlencoded")
            // set the request method - POST
            connection.requestMethod = "GET"
            val `in` = connection.inputStream
            val reader = InputStreamReader(`in`)

            // read the response data
            var data = reader.read()
            while (data != -1) {
                val current = data.toChar()
                result += current
                data = reader.read()
            }
            return result
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // if not able to retrieve data return null
        return null
    }

    private fun onResponse(result: String?) {
        try {

            // convert the string to JSON object for better reading
            val resultJson = JSONObject(result)
            // patient name field
            val patient_name = resultJson.getString("last_name") + ", " + resultJson.getString("first_name")
            setText(this.patientName, patient_name)
            // re-admission risk field
            val readmissionLevel = resultJson.getString("class")
            setText(this.readmissionLevel, readmissionLevel)
            // cluster field
            val patientCluster = resultJson.getString("cluster")
            when (patientCluster) {
                "0" -> setText(this.recommendation, "Low touch support")
                "1" -> setText(this.recommendation, "Skilled nursing facility or higher acuity sight of care recommended")
                "2" -> setText(this.recommendation, "Develop care plan, check in and monitor")
                "3" -> setText(this.recommendation, "Discuss support for social determinants of health, build out program to address any gaps")
                else -> {
                    setText(this.recommendation, "Error. Unknown patient cluster")
                }
            }


            // Initialize prediction text
            var prediction = ""
            // Update text with various fields from response
            prediction += "Patient ID: " + resultJson.getString("patient_id") + "\n"
            prediction += "Gender: " + resultJson.getString("gender") + "\n"
            prediction += "Age: " + resultJson.getString("age") + "\n"
            prediction += "A1C result: " + resultJson.getString("A1Cresult") + "\n"
            //Update the prediction to the view
            setText(this.resultView, prediction)

        } catch (e: Exception) {
            e.printStackTrace()
            this.resultView!!.text = "Oops!! something went wrong, please try again"
            // Remove previous on screen info
            this.patientName!!.text = ""
            this.readmissionLevel!!.text = ""
            this.recommendation!!.text = ""
        }
    }

    private fun setText(text: TextView?, value: String) {
        runOnUiThread { text!!.text = value }
    }

    // Start the QR Scanner
    private fun startScanner() {
        val scanner = IntentIntegrator(this)
        // QR Code Format
        scanner.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        // Set Text Prompt at Bottom of QR code Scanner Activity
        scanner.setPrompt("Scan the patient QR code")
        // Start Scanner (don't use initiateScan() unless if you want to use OnActivityResult)
        mQrResultLauncher.launch(scanner.createScanIntent())
    }
}