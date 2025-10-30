package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONObject;

public class INDLViewerActivity
    extends BaseActivity
{
    private static final String TAG = MainActivity.TAG;
    private WebView webView;
    private String json;

    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_in_viewer);
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        json = intent.getStringExtra("json");

        webView = (WebView)findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setTextZoom(100);
        webView.loadUrl("file:///android_asset/indl/indl.html");
        webView.setWebViewClient(new WebViewClient() {
                public void onPageFinished(WebView view, String url) {
                    render();
                }
                public void onReceivedError(WebView view,
                                            WebResourceRequest request,
                                            WebResourceError error) {
                    Log.d(TAG, "webview error: " + error);
                }
            });
    }

    public void addMessage(String msg) {
        String js = "addMessage(" + JSONObject.quote(msg) + ")";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(js, null);
        } else {
            webView.loadUrl("javascript:" + js );
        }
        webView.scrollTo(0, webView.getContentHeight());
    }

    public void clearMessage(String msg) {
        String js = "clearMessage()";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(js, null);
        } else {
            webView.loadUrl("javascript:" + js );
        }
    }

    public void render() {
        //Log.d(TAG, "json: " + json);
        String js = "render(" + JSONObject.quote(json) + ")";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(js, null);
        } else {
            webView.loadUrl("javascript:" + js );
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, getClass().getSimpleName() + "#onNewIntent()");
        Toast.makeText(this, "ビューアを閉じてください", Toast.LENGTH_LONG).show();
    }

}
