package com.tinnhanh.reader

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.view.View
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.tinnhanh.core.Discovery
import com.tinnhanh.core.DohDns
import com.tinnhanh.core.SignatureVerifier
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var chromeClient: AppChromeClient? = null
    private val bg = Executors.newSingleThreadExecutor()

    // <input type=file>
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var cameraImageUri: Uri? = null

    // getUserMedia (camera/mic trong trang)
    private var pendingWebPermission: PermissionRequest? = null

    // GPS
    private var pendingGeoOrigin: String? = null
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null

    // Video fullscreen
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var savedOrientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraPermLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var geoPermLauncher: ActivityResultLauncher<Array<String>>

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerLaunchers()

        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            javaScriptCanOpenWindowsAutomatically = true
            setGeolocationEnabled(true)
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = WebViewClient()
        chromeClient = AppChromeClient()
        webView.webChromeClient = chromeClient

        bg.execute { resolveAndLoad() }
    }

    /** Lớp 1: tìm gương sống → load thẳng. Lớp 2: không có gương → bật proxy rồi load. */
    private fun resolveAndLoad() {
        val bootstrap = OkHttpClient()
        val dohClient = OkHttpClient.Builder().dns(DohDns(bootstrap)).build()
        val result = try {
            Discovery(
                anchorClientHttp = dohClient,
                probeHttp = OkHttpClient(),
                verifier = SignatureVerifier(Config.PUBLIC_KEY_B64),
                anchors = Config.ANCHORS,
            ).discover()
        } catch (e: Exception) {
            null
        }

        // Kiểm tra bản mới (nếu có config).
        result?.config?.let { cfg ->
            runOnUiThread { Updater.check(this, cfg, OkHttpClient()) }
        }

        // FORCE_PROXY_TEST: ép bỏ qua Lớp 1 để test tunnel trên máy thật.
        val live = if (Config.FORCE_PROXY_TEST) null else result?.liveDomain
        if (live != null) {
            runOnUiThread { webView.loadUrl(live) }
            return
        }

        // Lớp 2: không gương nào vào thẳng được → thử tunnel qua proxy.
        val ssKey = result?.config?.outline?.firstOrNull()
        val firstDomain = result?.config?.domains?.firstOrNull() ?: Config.FALLBACK_DOMAIN
        if (ssKey != null && ssKey.startsWith("ss://")) {
            val addr = ProxyManager.start(ssKey)
            if (addr != null) {
                val applied = ProxyManager.applyToWebView(addr, { it.run() }) {
                    runOnUiThread { webView.loadUrl(firstDomain) }
                }
                if (applied) return
            }
        }

        runOnUiThread { webView.loadUrl(firstDomain) }
    }

    private fun registerLaunchers() {
        fileChooserLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val cb = filePathCallback
            filePathCallback = null
            if (cb == null) return@registerForActivityResult
            val uris: Array<Uri>? = when {
                result.resultCode != RESULT_OK -> null
                result.data?.data != null -> arrayOf(result.data!!.data!!)
                cameraImageUri != null -> arrayOf(cameraImageUri!!)
                else -> null
            }
            cb.onReceiveValue(uris)
        }

        cameraPermLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { grants ->
            val req = pendingWebPermission
            pendingWebPermission = null
            if (req == null) return@registerForActivityResult
            if (grants.values.all { it }) req.grant(req.resources) else req.deny()
        }

        geoPermLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { grants ->
            val cb = pendingGeoCallback
            val origin = pendingGeoOrigin
            pendingGeoCallback = null
            pendingGeoOrigin = null
            if (cb != null && origin != null) cb.invoke(origin, grants.values.any { it }, false)
        }
    }

    private fun hasPermission(p: String) =
        ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun showSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
    }

    private fun buildCameraIntent(): Intent? {
        if (!hasPermission(Manifest.permission.CAMERA)) return null
        return try {
            val imageFile = File.createTempFile("capture_", ".jpg", cacheDir)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", imageFile)
            cameraImageUri = uri
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, uri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
        } catch (e: Exception) {
            null
        }
    }

    private inner class AppChromeClient : WebChromeClient() {
        override fun onPermissionRequest(request: PermissionRequest) {
            runOnUiThread {
                val wantsCamera = request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                if (wantsCamera && !hasPermission(Manifest.permission.CAMERA)) {
                    pendingWebPermission = request
                    cameraPermLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                } else {
                    request.grant(request.resources)
                }
            }
        }

        override fun onGeolocationPermissionsShowPrompt(
            origin: String,
            callback: GeolocationPermissions.Callback,
        ) {
            if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                callback.invoke(origin, true, false)
                return
            }
            pendingGeoOrigin = origin
            pendingGeoCallback = callback
            geoPermLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }

        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
            if (customView != null) {
                callback.onCustomViewHidden()
                return
            }
            customView = view
            customViewCallback = callback
            savedOrientation = requestedOrientation
            (window.decorView as FrameLayout).addView(
                view,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                )
            )
            webView.visibility = View.GONE
            hideSystemBars()
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }

        override fun onHideCustomView() {
            val view = customView ?: return
            (window.decorView as FrameLayout).removeView(view)
            customView = null
            customViewCallback?.onCustomViewHidden()
            customViewCallback = null
            webView.visibility = View.VISIBLE
            showSystemBars()
            requestedOrientation = savedOrientation
        }

        override fun onShowFileChooser(
            webView: WebView?,
            callback: ValueCallback<Array<Uri>>?,
            params: FileChooserParams?,
        ): Boolean {
            filePathCallback?.onReceiveValue(null)
            filePathCallback = callback

            val contentIntent = params?.createIntent()
                ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
            val cameraIntent = buildCameraIntent()
            val chooser = Intent(Intent.ACTION_CHOOSER).apply {
                putExtra(Intent.EXTRA_INTENT, contentIntent)
                putExtra(Intent.EXTRA_TITLE, "Chọn hoặc chụp ảnh")
                if (cameraIntent != null) {
                    putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf<Parcelable>(cameraIntent))
                }
            }
            return try {
                fileChooserLauncher.launch(chooser)
                true
            } catch (e: Exception) {
                filePathCallback = null
                false
            }
        }
    }

    override fun onBackPressed() {
        when {
            customView != null -> chromeClient?.onHideCustomView()
            webView.canGoBack() -> webView.goBack()
            else -> super.onBackPressed()
        }
    }
}
