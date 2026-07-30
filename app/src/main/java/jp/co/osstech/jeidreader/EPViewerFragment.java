package jp.co.osstech.jeidreader;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import org.json.JSONObject;

/**
 * パスポートの読み取り結果を表示する画面。
 *
 * <p>Activityではなく呼び出し元のActivity内に表示するFragmentとして実装している。
 * {@code NfcAdapter#enableReaderMode()}はActivityに紐づくAPIであり、Activity境界ごとに
 * リーダーモードの解除・再登録が発生する。同一Activity内に収めれば呼び出し元は
 * resumedのままなので、登録は一度も解除されない。
 *
 * <p>表示中の二重読み取りは、呼び出し元が{@code enableNFC}をfalseにすることで抑止する。
 *
 * <p>読み取り結果のJSONはFragmentの引数として渡す。引数は状態保存時に保存・復元されるため、
 * 画面回転やプロセス復元の後も再描画できる(共有ViewModelではプロセス復元で失われる)。
 */
public class EPViewerFragment
    extends Fragment
{
    /** FragmentManagerへ登録する際のタグ。 */
    public static final String TAG_FRAGMENT = "epviewer";

    private static final String TAG = BaseActivity.TAG;
    private static final String ARG_ASSET_URL = "assetUrl";
    private static final String ARG_JSON = "json";

    private static final String EP_ASSET_URL = "file:///android_asset/ep/ep.html";

    private WebView webView;

    /**
     * インスタンスを生成します。
     *
     * @param json 表示する読み取り結果のJSON
     * @return 生成したインスタンス
     */
    public static EPViewerFragment newInstance(String json) {
        Bundle args = new Bundle();
        args.putString(ARG_ASSET_URL, EP_ASSET_URL);
        args.putString(ARG_JSON, json);
        EPViewerFragment fragment = new EPViewerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ep_viewer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        webView = (WebView)view.findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setTextZoom(100);
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
        webView.loadUrl(requireArguments().getString(ARG_ASSET_URL));
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, getClass().getSimpleName() + "#onDestroyView()");
        // WebViewはContextを保持し続けるため、Viewの破棄に合わせて明示的に解放する。
        // Activityであれば画面ごと破棄されるが、Fragmentでは自前で行う必要がある。
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroyView();
    }

    private void render() {
        if (webView == null) {
            return;
        }
        String json = requireArguments().getString(ARG_JSON);
        webView.evaluateJavascript("render(" + JSONObject.quote(json) + ")", null);
    }
}
