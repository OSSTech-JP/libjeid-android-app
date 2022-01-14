package jp.co.osstech.jeidreader;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

public class INReaderActivity
    extends BaseActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_in_reader);
        super.onCreate(savedInstanceState);
        EditText editPin = findViewById(R.id.edit_pin);
        this.enableNFC = true;
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, getClass().getSimpleName() + "#onNewIntent()");
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        this.onTagDiscovered(tag);
    }

    @Override
    public void onTagDiscovered(final Tag tag) {
        Log.d(TAG, getClass().getSimpleName() + "#onTagDiscovered()");
        if (!this.enableNFC) {
            Log.d(TAG, getClass().getSimpleName() + ": NFC disabled.");
            return;
        }
        INReaderTask task = new INReaderTask(this, tag);
        task.execute();
    }

    protected void showInvalidPinDialog(String title, String msg) {
        Log.d(TAG, getClass().getSimpleName() + "#showInvalidPinDialog()");
        this.enableNFC = false;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setNeutralButton(
                "戻る",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        enableNFC = true;
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    protected String getPin() {
        EditText edit = findViewById(R.id.edit_pin);
        return edit.getText().toString();
    }

    protected void setMessage(String message) {
        TextView view = findViewById(R.id.message);
        view.setText(message);
    }

    protected void addMessage(String message) {
        TextView text = (TextView)findViewById(R.id.message);
        text.setText(text.getText().toString() + "\n" + message);
        // 一番下にスクロール
        final ScrollView scroll = (ScrollView)findViewById(R.id.scroll);
        scroll.post(new Runnable() {
                public void run() {
                    scroll.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
    }
}
