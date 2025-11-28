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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EPReaderActivity
    extends BaseActivity
{
    EditText passportNumber;
    EditText birthDate;
    EditText expireDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_ep_reader);
        super.onCreate(savedInstanceState);
        this.enableNFC = true;
        passportNumber = (EditText)findViewById(R.id.edit_ep_passport_number);
        birthDate = (EditText)findViewById(R.id.edit_ep_birth_date);
        expireDate = (EditText)findViewById(R.id.edit_ep_expire_date);

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
        EPReaderTask task = new EPReaderTask(this, tag);
        ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.submit(task);
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
