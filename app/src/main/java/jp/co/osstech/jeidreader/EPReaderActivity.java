package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.ArrayList;

public class EPReaderActivity
    extends BaseActivity
    implements MrzScanFragment.Listener
{
    EditText passportNumber;
    EditText birthDate;
    EditText expireDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ep_reader);
        this.enableNFC = true;
        passportNumber = (EditText)findViewById(R.id.edit_ep_passport_number);
        birthDate = (EditText)findViewById(R.id.edit_ep_birth_date);
        expireDate = (EditText)findViewById(R.id.edit_ep_expire_date);
        // 復元時にスキャン画面が表示されていた場合はNFCを止めたままにする
        if (getSupportFragmentManager()
                .findFragmentByTag(MrzScanFragment.TAG_FRAGMENT) != null) {
            this.enableNFC = false;
        }
        findViewById(R.id.button_ep_scan_mrz).setOnClickListener(v -> showMrzScanner());

        String[] items = getResources().getStringArray(R.array.inputs_ep_reader);
        if (items.length == 0) {
            return;
        }
        ArrayList<String> nameList = new ArrayList<>();
        ArrayList<String> passportNumList = new ArrayList<>();
        ArrayList<String> birthDateList = new ArrayList<>();
        ArrayList<String> expireDateList = new ArrayList<>();
        for (String str : items) {
            String[] splitted = str.split(",", -1);
            nameList.add(splitted[0]);
            passportNumList.add(splitted[1]);
            birthDateList.add(splitted[2]);
            expireDateList.add(splitted[3]);
        }
        String[] names = nameList.toArray(new String[nameList.size()]);
        ArrayAdapter<String> adapter
                = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner spinner  = (Spinner) this.findViewById(R.id.spinner_ep_inputs);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView parent, View view, int position, long id) {
                Spinner spinner = (Spinner) parent;
                int selectedId = (int) spinner.getSelectedItemId();
                passportNumber.setText(passportNumList.get(selectedId), TextView.BufferType.NORMAL);
                birthDate.setText(birthDateList.get(selectedId), TextView.BufferType.NORMAL);
                expireDate.setText(expireDateList.get(selectedId), TextView.BufferType.NORMAL);
            }

            public void onNothingSelected(AdapterView parent) {
            } });
        spinner.setVisibility(View.VISIBLE);
    }

    @Override
    public void onTagDiscovered(final Tag tag) {
        Log.d(TAG, getClass().getSimpleName() + "#onTagDiscovered()");
        if (!this.enableNFC) {
            Log.d(TAG, getClass().getSimpleName() + ": NFC disabled.");
            return;
        }
        runOnUiThread(() -> {
            String passportNum = getPassportNumber();
            String birth = getBirthDate();
            String expire = getExpireDate();
            hideKeyboard();
            EPReaderTask task = new EPReaderTask(this, tag, passportNum, birth, expire);
            exec.submit(task);
        });
    }

    /**
     * MRZ読み取り画面を表示します。
     *
     * <p>スキャン中はカードを検出しても読み取りを開始しないよう、NFCを止めておきます。
     * リーダーモードの登録自体は解除しません(別Activityへ遷移しないため解除されません)。
     */
    private void showMrzScanner() {
        hideKeyboard();
        this.enableNFC = false;
        new MrzScanFragment().show(getSupportFragmentManager(), MrzScanFragment.TAG_FRAGMENT);
    }

    /**
     * MRZ読み取り結果を入力欄へ反映します。日付はYYYYMMDDの8桁で渡されます。
     */
    @Override
    public void onMrzScanned(String documentNumber, String birth, String expire) {
        Log.d(TAG, getClass().getSimpleName() + ": MRZ scanned, number=" + documentNumber);
        passportNumber.setText(documentNumber, TextView.BufferType.NORMAL);
        birthDate.setText(birth, TextView.BufferType.NORMAL);
        expireDate.setText(expire, TextView.BufferType.NORMAL);
        print("# MRZを読み取りました。パスポートを読み取り位置にタッチしてください");
    }

    @Override
    public void onMrzScanFinished() {
        this.enableNFC = true;
    }

    protected String getPassportNumber() {
        return passportNumber.getText().toString();
    }

    protected String getBirthDate() {
        return birthDate.getText().toString();
    }

    protected String getExpireDate() {
        return expireDate.getText().toString();
    }
}
