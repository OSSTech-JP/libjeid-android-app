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

public class INReaderActivity
    extends BaseActivity
{
    private View viewerContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_reader);
        this.enableNFC = true;
        EditText editPin = findViewById(R.id.edit_pin);
        viewerContainer = findViewById(R.id.viewer_container);
        // 復元時にビューアが表示されていた場合はNFCを止めたままにする
        if (isViewerShown()) {
            this.enableNFC = false;
            viewerContainer.setVisibility(View.VISIBLE);
            setTitle(R.string.in_viewer);
        }
        // ビューアが閉じられたら読み取り画面の状態へ戻す
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (isViewerShown()) {
                return;
            }
            viewerContainer.setVisibility(View.GONE);
            setTitle(R.string.in_reader);
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
        setTitle(R.string.in_viewer);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.viewer_container,
                    ViewerFragment.newInstance(ViewerFragment.ASSET_IN, json),
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
            String pin = getPin();
            hideKeyboard();
            if (!pin.matches("\\d{4}")) {
                showDialog("入力エラー", "暗証番号は4桁の数字を入力してください。");
                return;
            }
            INReaderTask task = new INReaderTask(this, tag, pin);
            exec.submit(task);
        });
    }

    protected void showInvalidPinDialog(InvalidPinException e) {
        Log.d(TAG, getClass().getSimpleName() + "#showInvalidPinDialog()");
        String title;
        String msg;
        if (e.isBlocked()) {
            title = "暗証番号(4桁)がブロックされています";
            msg = "市区町村窓口でブロック解除の申請をしてください。";
        } else {
            int counter = e.getCounter();
            title = "暗証番号(4桁)が間違っています";
            msg = "暗証番号(4桁)を正しく入力してください。";
            msg += "のこり" + counter + "回間違えるとブロックされます。";
        }
        this.print(title);
        this.print(msg);
        this.showDialog(title, msg);
    }

    protected String getPin() {
        EditText edit = findViewById(R.id.edit_pin);
        return edit.getText().toString();
    }
}
