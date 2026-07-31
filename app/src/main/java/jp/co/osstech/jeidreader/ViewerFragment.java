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
 * 読み取り結果をWebViewで表示する画面。カード種別によらず共通で使用します。
 *
 * <p>表示内容はassets配下のHTML/JSに委ねており、カード種別ごとの差分は
 * 読み込むHTMLのURLだけです。
 *
 * <p>Activityではなく呼び出し元のActivity内に表示するFragmentとして実装しています。
 * {@code NfcAdapter#enableReaderMode()}はActivityに紐づくAPIであり、Activity境界ごとに
 * リーダーモードの解除・再登録が発生します。同一Activity内に収めれば呼び出し元は
 * resumedのままなので、登録は一度も解除されません。
 *
 * <p>表示中の二重読み取りは、呼び出し元が{@code enableNFC}をfalseにすることで抑止します。
 *
 * <p>読み取り結果のJSONはFragmentの引数として渡します。引数は状態保存時に保存・復元される
 * ため、画面回転やプロセス復元の後も再描画できます(共有ViewModelでは復元で失われます)。
 */
public class ViewerFragment
    extends Fragment
{
    /** FragmentManagerへ登録する際のタグ。1つのActivityにビューアは1つだけ表示します。 */
    public static final String TAG_FRAGMENT = "viewer";

    /** パスポートのビューア。 */
    public static final String ASSET_EP = "file:///android_asset/ep/ep.html";
    /** 運転免許証のビューア。 */
    public static final String ASSET_DL = "file:///android_asset/dl/dl.html";
    /** 在留カード(第1世代)のビューア。 */
    public static final String ASSET_RC = "file:///android_asset/rc/rc.html";
    /** 在留カード(第2世代)のビューア。 */
    public static final String ASSET_RC2 = "file:///android_asset/rc2/rc2.html";
    /** マイナンバーカード券面のビューア。 */
    public static final String ASSET_IN = "file:///android_asset/in/in.html";
    /** マイナ運転免許証のビューア。 */
    public static final String ASSET_INDL = "file:///android_asset/indl/indl.html";
    /** 電子証明書のビューア。 */
    public static final String ASSET_CERT = "file:///android_asset/show_cert/show_cert.html";

    private static final String TAG = BaseActivity.TAG;
    private static final String ARG_ASSET_URL = "assetUrl";
    private static final String ARG_JSON = "json";

    private WebView webView;

    /**
     * インスタンスを生成します。
     *
     * @param assetUrl 表示するHTMLのURL({@link #ASSET_EP}など)
     * @param json 表示する読み取り結果のJSON
     * @return 生成したインスタンス
     */
    public static ViewerFragment newInstance(String assetUrl, String json) {
        Bundle args = new Bundle();
        args.putString(ARG_ASSET_URL, assetUrl);
        args.putString(ARG_JSON, json);
        ViewerFragment fragment = new ViewerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_viewer, container, false);
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
