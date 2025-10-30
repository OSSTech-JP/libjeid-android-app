package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jp.co.osstech.libjeid.InvalidPinException;

public class INDLReaderActivity
    extends BaseActivity
{
    EditText editPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_indl_reader);
        super.onCreate(savedInstanceState);
        this.enableNFC = true;
        editPin = (EditText)findViewById(R.id.edit_indl_pin);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, getClass().getSimpleName() + "#onNewIntent()");
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag.class);
        this.onTagDiscovered(tag);
    }

    @Override
    public void onTagDiscovered(final Tag tag) {
        Log.d(TAG, getClass().getSimpleName() + "#onTagDiscovered()");
        if (!this.enableNFC) {
            Log.d(TAG, getClass().getSimpleName() + ": NFC disabled.");
            return;
        }
        INDLReaderTask task = new INDLReaderTask(this, tag);
        ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.submit(task);
    }

    protected String getPin() {
        return editPin.getText().toString();
    }

    protected void showInvalidPinDialog(InvalidPinException e) {
        String title;
        String msg;
        if (e.isBlocked()) {
            title = "暗証番号(4桁)がブロックされています";
            msg = "警察署でブロック解除の申請をしてください。";
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
}
