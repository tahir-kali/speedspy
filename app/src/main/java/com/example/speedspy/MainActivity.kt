package com.example.speedspy

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.*
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.speedspy.databinding.ActivityMainBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter

import org.json.JSONObject
import javax.net.ssl.HttpsURLConnection


class MainActivity : AppCompatActivity() {




    private lateinit var binding: ActivityMainBinding
    private lateinit var webView: WebView

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)



        var speedBtn = findViewById<Button>(R.id.startBtn)
        speedBtn.setOnClickListener{
            loadWebView()
            speedBtn.visibility = View.GONE
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadWebView (){
        webView = findViewById<WebView>(R.id.webView)
        webView.loadUrl("https://fast.com")
        // Enable JavaScript in the WebView
        webView.settings.javaScriptEnabled = true
        // Get the WebSettings for the WebView
        webView.settings.safeBrowsingEnabled = false
        webView.settings.domStorageEnabled = true
        //Add our a bridge between iframe's js and our code
        webView.addJavascriptInterface(WebInterface(this), "Bridge")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String) {
                super.onPageFinished(view, url)
                if (view != null) injectJavaScript(view)
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                println("------------------------------WebView"+consoleMessage.message())
                return true
            }

        }
    }
    private fun injectJavaScript(view: WebView) {
        view.loadUrl(
            """
            javascript:(function(){
            var valuesSentToServer = start();
             function start() {
                if(valuesSentToServer===true) return true;
                const showMoreDetails = document.getElementById("show-more-details-link");
                if(showMoreDetails) showMoreDetails.click();
                const speedvalue = document.getElementById("speed-value");
                if(!speedvalue.classList.contains('succeeded')) return false;
                const speedunits = document.getElementById("speed-units").innerHTML;
                const unloadedL = document.getElementById("latency-value");
                if(!unloadedL.classList.contains('succeeded')) return false;
                const unloadedLUnit = document.getElementById("latency-units").innerHTML;
                const loadedL = document.getElementById("bufferbloat-value");
                 if(!loadedL.classList.contains('succeeded')) return false;
                const loadedLUnit = document.getElementById("bufferbloat-units").innerHTML;
                const uploadSpeed = document.getElementById("upload-value");
                if(!uploadSpeed.classList.contains('succeeded')) return false;
                const uploadSpeedUnit = document.getElementById("upload-units").innerHTML;
                const dataToSend = {
                "downloadSpeed": speedvalue.innerHTML+speedunits,
                "unloadedLatency": unloadedL.innerHTML+unloadedLUnit,
                "loadedLatency": loadedL.innerHTML+loadedLUnit,
                "uploadSpeed": uploadSpeed.innerHTML+uploadSpeedUnit
                };
                Bridge.sendDataToServer(JSON.stringify(dataToSend));
                return true;
             }
             
             function initiateStart(){
                if(!valuesSentToServer){
                   setTimeout(()=>{
                   valuesSentToServer = start();
                   initiateStart();
                   },1000);
                }
             }
             initiateStart();
            })()
        """.trimIndent()
        )
    }

    class WebInterface(private val mContext: Context){
        var sentDataToServer = false;
        fun showToast(message: String) {
            Handler(Looper.getMainLooper()).post {
                val toast = Toast.makeText(mContext, message, Toast.LENGTH_SHORT)
                toast.show()
            }
        }
        fun initiatePostRequest(data: String) {
        //The sentDataToServer variable prevents the need for try catch for they are very expensive
            if(sentDataToServer) return;
            val urlObj = URL("https://api-life3.megafon.tj/tahir")
            println("Sending $data to $urlObj")
            val conn = urlObj.openConnection() as HttpsURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            val wr = OutputStreamWriter(conn.outputStream)
            wr.write(data)
            wr.flush()
            // Handle the response
            val responseCode = conn.responseCode
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                val inputStream = conn.inputStream
                // Read the response
                showToast(inputStream.toString())
            } else {
                // Handle the error
                showToast("Some Error occured! Response Code $responseCode")
            }
            conn.disconnect()
        }
        @OptIn(DelicateCoroutinesApi::class)
        @JavascriptInterface
        fun sendDataToServer(data: String) {
            initiatePostRequest(data)
            sentDataToServer = true
        }
    }
}


