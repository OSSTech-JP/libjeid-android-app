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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import jp.co.osstech.libjeid.InvalidPinException;
import org.json.JSONObject;

public class JPKICertReaderActivity
    extends BaseActivity
{
    private String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_cert);

        TextView textView = (TextView)findViewById(R.id.message);
        TextView info = (TextView)findViewById(R.id.info);
        this.enableNFC = true;
        Intent intent = getIntent();
        type = intent.getStringExtra("TYPE");
        switch (type) {
        case "AUTH":
            info.setText(getString(R.string.show_auth_cert) + "を表示します。");
            break;
        case "SIGN":
            info.setText(getString(R.string.show_sign_cert) + "を表示します。");
            TextView textPin = (TextView)findViewById(R.id.text_pin);
            textPin.setVisibility(View.VISIBLE);
            EditText editPin = (EditText)findViewById(R.id.edit_pin);
            editPin.setVisibility(View.VISIBLE);
            break;
        case "AUTH_CA":
            info.setText(getString(R.string.show_auth_ca_cert) + "を表示します。");
            break;
        case "SIGN_CA":
            info.setText(getString(R.string.show_sign_ca_cert) + "を表示します。");
            break;
        default:
            Log.e(TAG, "Unknown type");
            finish();
        }
    }

    @Override
    public void onTagDiscovered(final Tag tag) {
        Log.d(TAG, getClass().getSimpleName() + "#onTagDiscovered()");

        if (!this.enableNFC) {
            Log.d(TAG, getClass().getSimpleName() + ": NFC disabled.");
            return;
        }

        JPKICertReaderTask task = new JPKICertReaderTask(this, tag, type);
        ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.submit(task);
    }

    protected void showInvalidPasswordDialog(InvalidPinException e) {
        String title;
        String msg;
        if (e.isBlocked()) {
            title = "パスワードがブロックされています";
            msg = "市区町村窓口でブロック解除の申請をしてください。";
        } else {
            int counter = e.getCounter();
            title = "パスワードが間違っています";
            msg = "パスワードを正しく入力してください。";
            msg += "のこり" + counter + "回間違えるとブロックされます。";
        }
        this.print(title);
        this.print(msg);
        this.showDialog(title, msg);
    }

    protected String getPassword() {
        EditText edit = (EditText)findViewById(R.id.edit_pin);
        return edit.getText().toString();
    }
}

