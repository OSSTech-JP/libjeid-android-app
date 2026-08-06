package jp.co.osstech.jeidreader;

import android.app.AlertDialog;
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
import android.widget.Toast;
import java.util.ArrayList;

public class EPReaderActivity
    extends BaseActivity
    implements MrzScanFragment.Listener
{
    EditText passportNumber;
    EditText birthDate;
    EditText expireDate;
    private View viewerContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ep_reader);
        this.enableNFC = true;
        passportNumber = (EditText)findViewById(R.id.edit_ep_passport_number);
        birthDate = (EditText)findViewById(R.id.edit_ep_birth_date);
        expireDate = (EditText)findViewById(R.id.edit_ep_expire_date);
        viewerContainer = findViewById(R.id.viewer_container);
        // 復元時にスキャン画面またはビューアが表示されていた場合はNFCを止めたままにする
        if (getSupportFragmentManager()
                .findFragmentByTag(MrzScanFragment.TAG_FRAGMENT) != null) {
            this.enableNFC = false;
        }
        if (isViewerShown()) {
            this.enableNFC = false;
            viewerContainer.setVisibility(View.VISIBLE);
            setTitle(R.string.ep_viewer);
        }
        // ビューアが閉じられたら読み取り画面の状態へ戻す
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (isViewerShown()) {
                return;
            }
            viewerContainer.setVisibility(View.GONE);
            setTitle(R.string.ep_reader);
            this.enableNFC = true;
        });
        findViewById(R.id.button_ep_scan_mrz).setOnClickListener(v -> showMrzScanner());
        findViewById(R.id.button_ep_birth_date_help)
            .setOnClickListener(v -> showDateFormatHelp("生年月日"));
        findViewById(R.id.button_ep_expire_date_help)
            .setOnClickListener(v -> showDateFormatHelp("有効期限"));

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
            if (isViewerShown()) {
                runOnUiThread(() -> Toast.makeText(this, "ビューアを閉じてください",
                        Toast.LENGTH_LONG).show());
            }
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
     * 読み取り結果をビューアに表示します。UIスレッドから呼び出してください。
     *
     * <p>別Activityへ遷移せずこのActivity内のFragmentとして表示するため、
     * NFCリーダーモードの登録は解除されません。表示中の二重読み取りは
     * {@code enableNFC}で抑止します。
     *
     * @param json 表示する読み取り結果のJSON
     */
    public void showViewer(String json) {
        Log.d(TAG, getClass().getSimpleName() + ": showViewer(), json size=" + json.length());
        this.enableNFC = false;
        viewerContainer.setVisibility(View.VISIBLE);
        setTitle(R.string.ep_viewer);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.viewer_container,
                    ViewerFragment.newInstance(ViewerFragment.ASSET_EP, json),
                    ViewerFragment.TAG_FRAGMENT)
            .addToBackStack(ViewerFragment.TAG_FRAGMENT)
            .commit();
    }

    private boolean isViewerShown() {
        return getSupportFragmentManager()
                .findFragmentByTag(ViewerFragment.TAG_FRAGMENT) != null;
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
     * 日付欄の入力形式を説明するダイアログを表示します。
     *
     * <p>読み取りに使うのはMRZと同じ2桁年のためラベルはYYMMDDとしているが、
     * 西暦4桁で入力したいという要望にも応えるため8桁も受け付けている。
     * ラベルだけでは4桁も可であることが伝わらないため、ここで具体例を示す。
     *
     * @param label 対象の項目名
     */
    private void showDateFormatHelp(String label) {
        new AlertDialog.Builder(this)
            .setTitle(label + "の入力形式")
            .setMessage("西暦の下2桁から続けて、年月日を6桁で入力してください。\n\n"
                    + "例) 1990年11月8日 → 901108\n"
                    + "例) 2026年5月20日 → 260520\n\n"
                    + "西暦4桁のYYYYMMDD(8桁)でも入力できます。\n\n"
                    + "例) 1990年11月8日 → 19901108\n"
                    + "例) 2026年5月20日 → 20260520\n\n"
                    + "パスポート券面下部のMRZ(機械読取領域)には西暦の下2桁が"
                    + "記載されているため、カメラで読み取った場合は6桁が入ります。")
            .setPositiveButton("閉じる", null)
            .show();
    }

    /**
     * MRZ読み取り結果を入力欄へ反映します。日付はMRZと同じYYMMDDの6桁で渡されます。
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
