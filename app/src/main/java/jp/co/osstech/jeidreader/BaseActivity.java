package jp.co.osstech.jeidreader;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcB;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public abstract class BaseActivity
    extends AppCompatActivity
    implements TagDiscoveredListener
{
    public static final String TAG = "JeidReader";
    protected NfcAdapter nfcAdapter;

    // ビューアーやメニュー画面ではNFC読み取りを無効化する
    // また、PIN間違いが発生してダイヤログを表示している間に
    // 連続読み取りが発生することを防ぐためのフラグ
    protected boolean enableNFC = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, getClass().getSimpleName() +
              "#onCreate(" + savedInstanceState + ")");
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, getClass().getSimpleName() + "#onResume()");
        super.onResume();

        invalidateOptionsMenu();

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            return;
        }

        Log.d(TAG, "NFC mode: ReaderMode");
        if(!this.enableNFC) {
            // メニュー画面やビューアーでNFC読み取りを無効化します
            // これを行わないと通常モード(OS標準)の読み取りが有効になるからです
            nfcAdapter.enableReaderMode(this, null, NfcAdapter.STATE_OFF, null);
            return;
        }
        Bundle options = new Bundle();
        //options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 500);
        nfcAdapter.enableReaderMode(this,
                                    new NfcAdapter.ReaderCallback() {
                                         @Override
                                         public void onTagDiscovered(Tag tag) {
                                             BaseActivity.this.onTagDiscovered(tag);
                                         }
                                     },
                                     NfcAdapter.FLAG_READER_NFC_B | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                                     options);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, getClass().getSimpleName() + "#onPause()");
        super.onPause();
        if (nfcAdapter == null) {
            return;
        }
        nfcAdapter.disableReaderMode(this);
    }

    // ビューアーやメニューのActivityでこれが呼ばれます
    // サブクラスの**ReaderActivityでは適時overrideします
    public void onTagDiscovered(final Tag tag) {
        Log.d(TAG, getClass().getSimpleName() + "#onTagDiscovered()");
        Toast.makeText(this, "ビューアを閉じてください", Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (this.enableNFC) {
            // NFCステータスアイコンを表示
            menu.getItem(0).setVisible(true);
            if (nfcAdapter != null) {
                if (nfcAdapter.isEnabled()) {
                    menu.getItem(0).setIcon(ContextCompat.getDrawable(this, R.drawable.ic_nfc_on));
                } else {
                    menu.getItem(0).setIcon(ContextCompat.getDrawable(this, R.drawable.ic_nfc_off));
                }
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        int id = item.getItemId();
        if (id == R.id.menu_nfc_settings) {
            // NFC設定画面を開きます
            intent = new Intent(Settings.ACTION_NFC_SETTINGS);
            startActivity(intent);
        } else if (id == R.id.menu_about) {
            AboutDialogFragment dialog = new AboutDialogFragment();
            dialog.show(getSupportFragmentManager(), "about");
        }
        return true;
    }

    protected void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) {
            return;
        }
        View view = getCurrentFocus();
        if (view == null) {
            return;
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    protected void setMessage(String message) {
        TextView view = (TextView)findViewById(R.id.message);
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

    protected void clear() {
        TextView view = (TextView)findViewById(R.id.message);
        view.post(new Runnable() {
                @Override
                public void run() {
                    view.setText("");
                }
            });
    }

    protected void print(String msg) {
        Handler handler = new Handler(Looper.getMainLooper());
        TextView text = (TextView)findViewById(R.id.message);
        ScrollView scroll = (ScrollView)findViewById(R.id.scroll);
        handler.post(new Runnable() {
                @Override
                public void run() {
                    text.setText(text.getText().toString() + msg + "\n");
                    // 一番下にスクロール
                    scroll.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
    }

    protected void showDialog(String title, String msg) {
        Log.d(TAG, getClass().getSimpleName() + "#showDialog()");
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

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
                @Override
                public void run() {
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });
    }
}
