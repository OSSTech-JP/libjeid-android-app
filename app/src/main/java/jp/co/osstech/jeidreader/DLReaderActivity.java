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

public class DLReaderActivity
    extends BaseActivity
{
    EditText editPin1;
    EditText editPin2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_dl_reader);
        super.onCreate(savedInstanceState);
        this.enableNFC = true;
        editPin1 = (EditText)findViewById(R.id.edit_dl_pin1);
        editPin2 = (EditText)findViewById(R.id.edit_dl_pin2);
    }

    @Override
    public void onTagDiscovered(final Tag tag) {
        Log.d(TAG, getClass().getSimpleName() + "#onTagDiscovered()");
        if (!this.enableNFC) {
            Log.d(TAG, getClass().getSimpleName() + ": NFC disabled.");
            return;
        }
        DLReaderTask task = new DLReaderTask(this, tag);
        ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.submit(task);
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
