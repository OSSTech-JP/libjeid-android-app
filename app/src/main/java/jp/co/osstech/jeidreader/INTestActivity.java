package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class INTestActivity
    extends BaseActivity
{
    EditText editAuthPin;
    EditText editSignPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_test);
        super.onCreate(savedInstanceState);
        this.enableNFC = true;

        editAuthPin = (EditText)findViewById(R.id.edit_jpki_auth_pin);
        editSignPin = (EditText)findViewById(R.id.edit_jpki_sign_pin);
    }

    @Override
    public void onTagDiscovered(final Tag tag) {
        Log.d(TAG, getClass().getSimpleName() + "#onTagDiscovered()");
        if (!this.enableNFC) {
            Log.d(TAG, getClass().getSimpleName() + ": NFC disabled.");
            return;
        }
        INTestTask task = new INTestTask(this, tag);
        ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.submit(task);
    }

    protected String getAuthPin() {
        return editAuthPin.getText().toString();
    }
    protected String getSignPin() {
        return editSignPin.getText().toString();
    }

}
