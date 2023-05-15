package com.example.speedspy

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
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
import com.google.gson.Gson
import okhttp3.*
import java.io.File


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
//                println("------------------------------WebView"+consoleMessage.message())
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
                const informationContainer = document.querySelector('[data-marker="modification-characteristics"]');
                const childrenArray = [];
                for (let i = 0; i < informationContainer.children.length; i++) {
                  const child = divElement.children[i];
                  childrenArray.push(child.innerHTML);
                }
               
                Bridge.sendDataToServer(JSON.stringify(childrenArray));
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
        private fun showToast(message: String) {
            Handler(Looper.getMainLooper()).post {
                val toast = Toast.makeText(mContext, message, Toast.LENGTH_SHORT)
                toast.show()
            }
        }
        private fun writeToFile(data: String) {

                val gson = Gson()
                val json = gson.toJson(data)
                val file = File("CarSpecs.json")
                file.writeText(json)
                showToast("Informatino Downloaded Successfully!")
        }
        @JavascriptInterface
        fun sendDataToServer(data: String) {
            writeToFile(data)
            sentDataToServer = true
        }
    }
}


