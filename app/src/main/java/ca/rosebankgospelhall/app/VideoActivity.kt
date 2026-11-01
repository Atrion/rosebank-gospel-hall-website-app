package ca.rosebankgospelhall.app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import ca.rosebankgospelhall.app.data.HtmlParser
import ca.rosebankgospelhall.app.data.Site

/** Lets the page's player tell the app when a video can't be played in an embedded player. */
class VideoBridge(private val onFail: () -> Unit) {
    @JavascriptInterface
    fun onError(code: Int) {
        onFail()
    }
}

/** Full-screen in-app video player (YouTube, Vimeo, Rumble and direct video files). */
class VideoActivity : ComponentActivity() {
    private lateinit var root: FrameLayout
    private lateinit var content: FrameLayout
    private var web: WebView? = null
    private var exo: ExoPlayer? = null
    private var customView: View? = null
    private var customCb: WebChromeClient.CustomViewCallback? = null
    private var url = ""

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        url = intent.getStringExtra(EXTRA_URL).orEmpty()
        val youtube = HtmlParser.youtubeId(url)
        val vimeo = Regex("""vimeo\.com/(?:video/)?(\d+)""").find(url)?.groupValues?.get(1)
        val isFile = Regex("""\.(mp4|webm)(\?.*)?$""", RegexOption.IGNORE_CASE).containsMatchIn(url)
        if (youtube == null && vimeo == null && !isFile && !url.contains("rumble.com/embed/")) {
            openExternal()
            finish()
            return
        }

        root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(dp(4), 0, dp(4), 0)
        }
        bar.addView(barButton("Close") { finish() })
        bar.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        bar.addView(barButton(if (youtube != null) "Open in YouTube" else "Open in browser") { openExternal() })
        column.addView(bar, LinearLayout.LayoutParams(MATCH_PARENT, dp(52)))
        content = FrameLayout(this)
        column.addView(content, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        root.addView(column, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        setContentView(root)

        when {
            youtube != null -> startYouTube(youtube)
            isFile -> startFile()
            vimeo != null -> startWeb("https://player.vimeo.com/video/$vimeo?autoplay=1")
            else -> startWeb(url)
        }

        onBackPressedDispatcher.addCallback(this) {
            if (customView != null) hideCustom() else finish()
        }
    }

    private fun barButton(label: String, onClick: () -> Unit) = TextView(this).apply {
        text = label
        setTextColor(Color.WHITE)
        textSize = 16f
        setPadding(dp(14), dp(14), dp(14), dp(14))
        setOnClickListener { onClick() }
    }

    private fun openExternal() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun newWebView(): WebView {
        val w = WebView(this)
        w.settings.javaScriptEnabled = true
        w.settings.domStorageEnabled = true
        w.settings.mediaPlaybackRequiresUserGesture = false
        w.setBackgroundColor(Color.BLACK)
        w.webViewClient = WebViewClient()
        w.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (customView != null) {
                    callback.onCustomViewHidden()
                    return
                }
                customView = view
                customCb = callback
                root.addView(view, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
                systemBars(hidden = true)
            }

            override fun onHideCustomView() = hideCustom()

            override fun getDefaultVideoPoster(): Bitmap =
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
        web = w
        content.addView(w, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        return w
    }

    private fun startWeb(address: String) {
        newWebView().loadUrl(address)
    }

    private fun startYouTube(id: String) {
        val w = newWebView()
        // If the embedded player refuses (embedding disabled, error 153, no connection),
        // fall back to YouTube's own mobile page, which plays any public video.
        w.addJavascriptInterface(
            VideoBridge { runOnUiThread { w.loadUrl("https://m.youtube.com/watch?v=$id") } },
            "Android",
        )
        val html = """
            <!DOCTYPE html><html><head>
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <style>html,body{margin:0;height:100%;background:#000;overflow:hidden}
            #p{position:absolute;top:0;left:0;width:100%;height:100%}</style></head>
            <body><div id="p"></div>
            <script>
            var ready=false;
            var tag=document.createElement('script');
            tag.src='https://www.youtube.com/iframe_api';
            document.head.appendChild(tag);
            function onYouTubeIframeAPIReady(){
              new YT.Player('p',{videoId:'VIDEO_ID',width:'100%',height:'100%',
                playerVars:{autoplay:1,playsinline:1,rel:0,origin:'ORIGIN'},
                events:{onReady:function(e){ready=true;e.target.playVideo();},
                        onError:function(e){Android.onError(e.data);}}});
            }
            setTimeout(function(){if(!ready){Android.onError(-1);}},9000);
            </script></body></html>
        """.trimIndent().replace("VIDEO_ID", id).replace("ORIGIN", Site.BASE)
        w.loadDataWithBaseURL(Site.BASE + "/", html, "text/html", "utf-8", null)
    }

    private fun startFile() {
        val player = ExoPlayer.Builder(this).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
        exo = player
        content.addView(
            PlayerView(this).apply { this.player = player },
            FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT),
        )
    }

    private fun hideCustom() {
        customView?.let { root.removeView(it) }
        customView = null
        customCb?.onCustomViewHidden()
        customCb = null
        systemBars(hidden = false)
    }

    private fun systemBars(hidden: Boolean) {
        val c = WindowCompat.getInsetsController(window, root)
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (hidden) c.hide(WindowInsetsCompat.Type.systemBars()) else c.show(WindowInsetsCompat.Type.systemBars())
    }

    override fun onPause() {
        web?.onPause()
        exo?.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        web?.onResume()
    }

    override fun onDestroy() {
        web?.let {
            content.removeAllViews()
            it.destroy()
        }
        exo?.release()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_URL = "url"
    }
}
