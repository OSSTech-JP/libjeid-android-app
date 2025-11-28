package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jp.co.osstech.libjeid.InvalidPinException;

public class INReaderActivity
    extends BaseActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_in_reader);
        super.onCreate(savedInstanceState);
        this.enableNFC = true;
        EditText editPin = findViewById(R.id.edit_pin);
    }

    @Override
    public void onTagDiscovered(final Tag tag) {
        Log.d(TAG, getClass().getSimpleName() + "#onTagDiscovered()");
        if (!this.enableNFC) {
            Log.d(TAG, getClass().getSimpleName() + ": NFC disabled.");
            return;
        }
        INReaderTask task = new INReaderTask(this, tag);
        ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.submit(task);
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
