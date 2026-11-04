package vn.khkt.dopaminedecay

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout

class SafeCocoonActivity : Activity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetUrl = intent.getStringExtra("target_url") ?: "https://m.youtube.com"
        val container = FrameLayout(this)

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return false
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    injectStrictWebDecayEngine()
                }
            }
            loadUrl(targetUrl)
        }
        container.addView(webView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        // Thanh chuyển nhanh 4 ứng dụng chỉ định ở trên cùng
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xEE0F172A.toInt())
            setPadding(12, 12, 12, 12)
        }
        val targets = listOf(
            "🔴 YouTube" to "https://m.youtube.com",
            "⚫ TikTok" to "https://www.tiktok.com",
            "🔵 Facebook" to "https://m.facebook.com",
            "🟣 Roblox/Game" to "https://poki.com"
        )
        for ((label, url) in targets) {
            val btn = Button(this).apply {
                text = label
                textSize = 11f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { webView.loadUrl(url) }
            }
            topBar.addView(btn)
        }
        container.addView(topBar, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP
        ))

        setContentView(container)
    }

    private fun injectStrictWebDecayEngine() {
        val prefs = getSharedPreferences("dopamine_decay_prefs", Context.MODE_PRIVATE)
        val sec = prefs.getFloat("saved_active_seconds", 0f).toInt()
        val js = "(function(){if(!document.getElementById('dd-strict-style')){const st=document.createElement('style');st.id='dd-strict-style';st.innerHTML='ytm-reel-shelf-renderer,ytd-reel-shelf-renderer,[data-e2e=like-count],[data-e2e=comment-count]{display:none !important;}html{transition:filter 0.35s ease !important;}';document.head.appendChild(st);}const s=" + sec + ";let f='none';if(s>=90)f='grayscale(100%) brightness(85%)';else if(s>=60)f='saturate(30%) grayscale(50%)';else if(s>=30)f='saturate(65%)';document.documentElement.style.filter=f;})();" 
        webView.evaluateJavascript(js, null)
    }
}
