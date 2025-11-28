package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.*;
import android.widget.Toast;

import org.json.JSONObject;

public class DLViewerActivity
    extends BaseActivity
{
    private static final String TAG = MainActivity.TAG;
    private WebView webView;
    private String json;

    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_dl_viewer);
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        json = intent.getStringExtra("json");

        webView = (WebView)findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setTextZoom(100);
        webView.loadUrl("file:///android_asset/dl/dl.html");
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

    public void render() {
        String js = "render(" + JSONObject.quote(json) + ")";
        webView.evaluateJavascript(js, null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, getClass().getSimpleName() + "#onNewIntent()");
        Toast.makeText(this, "ビューアを閉じてください", Toast.LENGTH_LONG).show();
    }

}
