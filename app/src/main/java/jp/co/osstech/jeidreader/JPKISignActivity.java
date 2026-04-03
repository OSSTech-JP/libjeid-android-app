package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import jp.co.osstech.libjeid.JPKIAP;

public class JPKISignActivity
    extends BaseActivity
{
    private int type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        type = intent.getIntExtra("TYPE", 0);
        if (type == JPKIAP.TYPE_AUTH) {
            setContentView(R.layout.activity_sign_jpki_auth);
        } else {
            setContentView(R.layout.activity_sign_jpki_sign);
        }
        TextView textView = (TextView)findViewById(R.id.message);
        EditText editPin = (EditText)findViewById(R.id.edit_pin);
        this.enableNFC = true;
    }

    @Override
    public void onTagDiscovered(final Tag tag) {
        Log.d(TAG, getClass().getSimpleName() + "#onTagDiscovered()");
        if (!this.enableNFC) {
            Log.d(TAG, getClass().getSimpleName() + ": NFC disabled.");
            return;
        }
        runOnUiThread(() -> {
            String pin = getPin();
            byte[] input = getText().getBytes();
            String signAlgo = getSignAlgo();
            hideKeyboard();
            JPKISignTask task = new JPKISignTask(this, tag, pin, input, signAlgo);
            exec.submit(task);
        });
    }

    protected int getType() {
        return type;
    }

    protected String getPin() {
        EditText edit = (EditText)findViewById(R.id.edit_pin);
        return edit.getText().toString();
    }

    protected String getText() {
        TextView view = (TextView)findViewById(R.id.edit_text);
        return view.getText().toString();
    }

    protected String getSignAlgo() {
        Spinner spinner = (Spinner)findViewById(R.id.sign_algo_spinner);
        return spinner.getSelectedItem().toString();
    }
}
