package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import jp.co.osstech.libjeid.InvalidPinException;

public class DLReaderActivity
    extends BaseActivity
{
    EditText editPin1;
    EditText editPin2;
    private View viewerContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dl_reader);
        this.enableNFC = true;
        editPin1 = (EditText)findViewById(R.id.edit_dl_pin1);
        editPin2 = (EditText)findViewById(R.id.edit_dl_pin2);
        viewerContainer = findViewById(R.id.viewer_container);
        // 復元時にビューアが表示されていた場合はNFCを止めたままにする
        if (isViewerShown()) {
            this.enableNFC = false;
            viewerContainer.setVisibility(View.VISIBLE);
            setTitle(R.string.dl_viewer);
        }
        // ビューアが閉じられたら読み取り画面の状態へ戻す
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (isViewerShown()) {
                return;
            }
            viewerContainer.setVisibility(View.GONE);
            setTitle(R.string.dl_reader);
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
        setTitle(R.string.dl_viewer);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.viewer_container,
                    ViewerFragment.newInstance(ViewerFragment.ASSET_DL, json),
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
            String pin1 = getPin1();
            String pin2 = getPin2();
            hideKeyboard();
            if (!pin1.matches("\\d{4}")) {
                showDialog("入力エラー", "暗証番号1は4桁の数字を入力してください。");
                return;
            }
            if (!pin2.isEmpty() && !pin2.matches("\\d{4}")) {
                showDialog("入力エラー", "暗証番号2は4桁の数字を入力してください。");
                return;
            }
            DLReaderTask task = new DLReaderTask(this, tag, pin1, pin2);
            exec.submit(task);
        });
    }

    protected String getPin1() {
        return editPin1.getText().toString();
    }

    protected String getPin2() {
        return editPin2.getText().toString();
    }

    protected void showInvalidPinDialog(String name,
                                        InvalidPinException e) {
        String title;
        String msg;
        if (e.isBlocked()) {
            title = name + "がブロックされています";
            msg = "警察署でブロック解除の申請をしてください。";
        } else {
            int counter = e.getCounter();
            title = name + "が間違っています";
            msg = name + "を正しく入力してください。";
            msg += "のこり" + counter + "回間違えるとブロックされます。";
        }
        this.print(title);
        this.print(msg);
        this.showDialog(title, msg);
    }
}
