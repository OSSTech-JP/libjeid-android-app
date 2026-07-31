package jp.co.osstech.jeidreader;

import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class RC2ReaderActivity
    extends BaseActivity
{
    EditText rcNumber;
    private View viewerContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rc2_reader);
        this.enableNFC = true;
        rcNumber = (EditText)findViewById(R.id.edit_rc_number);
        viewerContainer = findViewById(R.id.viewer_container);
        // 復元時にビューアが表示されていた場合はNFCを止めたままにする
        if (isViewerShown()) {
            this.enableNFC = false;
            viewerContainer.setVisibility(View.VISIBLE);
            setTitle(R.string.rc2_viewer);
        }
        // ビューアが閉じられたら読み取り画面の状態へ戻す
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (isViewerShown()) {
                return;
            }
            viewerContainer.setVisibility(View.GONE);
            setTitle(R.string.rc2_reader);
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
        setTitle(R.string.rc2_viewer);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.viewer_container,
                    ViewerFragment.newInstance(ViewerFragment.ASSET_RC2, json),
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
            String rcNum = getRcNumber();
            hideKeyboard();
            RC2ReaderTask task = new RC2ReaderTask(this, tag, rcNum);
            exec.submit(task);
        });
    }

    protected String getRcNumber() {
        return rcNumber.getText().toString();
    }
}
