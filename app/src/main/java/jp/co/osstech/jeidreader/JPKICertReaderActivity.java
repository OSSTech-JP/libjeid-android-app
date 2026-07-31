package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import jp.co.osstech.libjeid.InvalidPinException;
import org.json.JSONObject;

public class JPKICertReaderActivity
    extends BaseActivity
{
    private String type;
    private View viewerContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_cert);

        TextView textView = (TextView)findViewById(R.id.message);
        TextView info = (TextView)findViewById(R.id.info);
        this.enableNFC = true;
        Intent intent = getIntent();
        type = intent.getStringExtra("TYPE");
        switch (type) {
        case "AUTH":
            info.setText(getString(R.string.show_auth_cert) + "を表示します。");
            break;
        case "SIGN":
            info.setText(getString(R.string.show_sign_cert) + "を表示します。");
            TextView textPin = (TextView)findViewById(R.id.text_pin);
            textPin.setVisibility(View.VISIBLE);
            EditText editPin = (EditText)findViewById(R.id.edit_pin);
            editPin.setVisibility(View.VISIBLE);
            break;
        case "AUTH_CA":
            info.setText(getString(R.string.show_auth_ca_cert) + "を表示します。");
            break;
        case "SIGN_CA":
            info.setText(getString(R.string.show_sign_ca_cert) + "を表示します。");
            break;
        default:
            Log.e(TAG, "Unknown type");
            finish();
        }
        viewerContainer = findViewById(R.id.viewer_container);
        // 復元時にビューアが表示されていた場合はNFCを止めたままにする
        if (isViewerShown()) {
            this.enableNFC = false;
            viewerContainer.setVisibility(View.VISIBLE);
            setTitle(R.string.show_cert_vewer);
        }
        // ビューアが閉じられたら読み取り画面の状態へ戻す
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (isViewerShown()) {
                return;
            }
            viewerContainer.setVisibility(View.GONE);
            setTitle(R.string.show_cert);
            this.enableNFC = true;
        });
    }

    /**
     * 読み取り結果をビューアに表示します。UIスレッドから呼び出してください。
     *
     * <p>別Activityへ遷移せずこのActivity内のFragmentとして表示するため、
     * NFCリーダーモードの登録は解除されません。表示中の二重読み取りは
     * {@code enableNFC}で抑止します。
     *
     * @param json 表示する読み取り結果のJSON
     */
    public void showViewer(String json) {
        Log.d(TAG, getClass().getSimpleName() + ": showViewer(), json size=" + json.length());
        this.enableNFC = false;
        viewerContainer.setVisibility(View.VISIBLE);
        setTitle(R.string.show_cert_vewer);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.viewer_container,
                    ViewerFragment.newInstance(ViewerFragment.ASSET_CERT, json),
                    ViewerFragment.TAG_FRAGMENT)
            .addToBackStack(ViewerFragment.TAG_FRAGMENT)
            .commit();
    }

    private boolean isViewerShown() {
        return getSupportFragmentManager()
                .findFragmentByTag(ViewerFragment.TAG_FRAGMENT) != null;
    }

    @Override
    public void onTagDiscovered(final Tag tag) {
        Log.d(TAG, getClass().getSimpleName() + "#onTagDiscovered()");

        if (!this.enableNFC) {
            Log.d(TAG, getClass().getSimpleName() + ": NFC disabled.");
            if (isViewerShown()) {
                runOnUiThread(() -> Toast.makeText(this, "ビューアを閉じてください",
                        Toast.LENGTH_LONG).show());
            }
            return;
        }

        runOnUiThread(() -> {
            String password = "SIGN".equals(type) ? getPassword() : "";
            hideKeyboard();
            JPKICertReaderTask task = new JPKICertReaderTask(this, tag, type, password);
            exec.submit(task);
        });
    }

    protected void showInvalidPasswordDialog(InvalidPinException e) {
        String title;
        String msg;
        if (e.isBlocked()) {
            title = "パスワードがブロックされています";
            msg = "市区町村窓口でブロック解除の申請をしてください。";
        } else {
            int counter = e.getCounter();
            title = "パスワードが間違っています";
            msg = "パスワードを正しく入力してください。";
            msg += "のこり" + counter + "回間違えるとブロックされます。";
        }
        this.print(title);
        this.print(msg);
        this.showDialog(title, msg);
    }

    protected String getPassword() {
        EditText edit = (EditText)findViewById(R.id.edit_pin);
        return edit.getText().toString();
    }
}

