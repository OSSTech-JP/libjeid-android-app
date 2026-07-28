package jp.co.osstech.jeidreader;

import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;

public class RC2ReaderActivity
    extends BaseActivity
{
    EditText rcNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rc2_reader);
        this.enableNFC = true;
        rcNumber = (EditText)findViewById(R.id.edit_rc_number);
    }

    @Override
    public void onTagDiscovered(final Tag tag) {
        Log.d(TAG, getClass().getSimpleName() + "#onTagDiscovered()");
        if (!this.enableNFC) {
            Log.d(TAG, getClass().getSimpleName() + ": NFC disabled.");
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
