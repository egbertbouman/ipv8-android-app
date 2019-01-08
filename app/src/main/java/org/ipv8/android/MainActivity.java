package org.ipv8.android;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatDelegate;
import android.text.TextUtils;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebResourceError;
import android.os.Handler;
import android.util.Log;
import java.lang.Runnable;

import org.ipv8.android.service.IPV8Service;

public class MainActivity extends BaseActivity {

    private Class<?> mClss;
    private WebView mWebView;
    private JavaScriptInterface JSInterface;
    private static final int ZBAR_CAMERA_PERMISSION = 1;
    private static final int WRITE_STORAGE_PERMISSION_REQUEST_CODE = 110;
    private static final String url = "http://127.0.0.1:8085/gui";

    static {
        // Backwards compatibility for vector graphics
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private void shutdown() {
        killService();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

    protected void startService() {
        IPV8Service.start(this); // Run normally
    }

    protected void killService() {
        IPV8Service.stop(this);
    }

    @Override
    protected void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            return;
        }
        switch (action) {

            case Intent.ACTION_MAIN:
                // Handle intent only once
                intent.setAction(null);
                return;

            case Intent.ACTION_SHUTDOWN:
                // Handle intent only once
                intent.setAction(null);
                shutdown();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Write permissions on sdcard?
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_STORAGE_PERMISSION_REQUEST_CODE);
        } else {
            startService();
        }

        mWebView = (WebView) findViewById(R.id.activity_main_webview);
        // Enable Javascript
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        // Set JavascriptInterface
        JSInterface = new JavaScriptInterface(this);
        mWebView.addJavascriptInterface(JSInterface, "android");
        // Load the GUI
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                // Show custom error page
                if (view != null){
                    String htmlData ="<html><body><div align=\"center\" >Please wait while loading backend..</div></body>";
                    view.loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null);
                }

                // Reload after 1s. Should happen only when IPv8 is still loading.
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mWebView.loadUrl(url);
                    }
                }, 1000);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                Log.d("tlswitnessWebview", "requesting: " + url);
                return super.shouldInterceptRequest(view, url);
            }
        });
        if (savedInstanceState == null) {
            mWebView.loadUrl(url);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mWebView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mWebView.restoreState(savedInstanceState);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            String qrcode = data.getExtras().getString("qrcode");
            mWebView.loadUrl("javascript:onScannerResult('" + qrcode + "')");
        }
    }

    public void launchActivity(Class<?> clss) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            mClss = clss;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, ZBAR_CAMERA_PERMISSION);
        } else {
            Intent intent = new Intent(this, clss);
            startActivityForResult(intent,1);
        }
    }


    public class JavaScriptInterface {
        Context mContext;

        JavaScriptInterface(Context c) {
            mContext = c;
        }

        @android.webkit.JavascriptInterface
        public void launchScanner()
        {
            launchActivity(ScannerActivity.class);
        }
    }
}