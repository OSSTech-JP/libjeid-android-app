package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.graphics.Bitmap;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import android.nfc.TagLostException;
import java.lang.ref.WeakReference;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONObject;

import jp.co.osstech.libjeid.*;
import jp.co.osstech.libjeid.ep.*;
import jp.co.osstech.libjeid.util.Hex;

public class EPReaderTask extends AsyncTask<Void, String, JSONObject>
{
    private static final String TAG = MainActivity.TAG;
    private WeakReference mRef;
    private Tag mNfcTag;
    private String passportNumber;
    private String birthDate;
    private String expireDate;
    private ProgressDialogFragment mProgress;

    public EPReaderTask(EPReaderActivity activity, Tag nfcTag) {
        mRef = new WeakReference<EPReaderActivity>(activity);
        mNfcTag = nfcTag;
    }

    @Override
    protected void onPreExecute() {
        EPReaderActivity activity = (EPReaderActivity)mRef.get();
        if (activity == null) {
            return;
        }
        passportNumber = activity.getPassportNumber();
        birthDate = activity.getBirthDate();
        expireDate = activity.getExpireDate();

        activity.hideKeyboard();
        activity.setMessage("# 読み取り開始、カードを離さないでください");
        mProgress = new ProgressDialogFragment();
        mProgress.show(activity.getSupportFragmentManager(), "progress");
    }

    @Override
    protected JSONObject doInBackground(Void... args) {
        Log.d(TAG, getClass().getSimpleName() + "#doInBackground()");

        if (passportNumber.isEmpty()) {
            publishProgress("旅券番号を設定してください");
            return null;
        }

        if (birthDate.isEmpty()) {
            publishProgress("生年月日を設定してください");
            return null;
        }
        if (birthDate.length() == 8) {
            birthDate = birthDate.substring(2);
        } else if (birthDate.length() != 6) {
            publishProgress("生年月日が8桁ではありません");
            return null;
        }

        if (expireDate.isEmpty()) {
            publishProgress("有効期限を設定してください");
            return null;
        }
        if (expireDate.length() == 8) {
            expireDate = expireDate.substring(2);
        } else if (expireDate.length() != 6) {
            publishProgress("有効期限が8桁ではありません");
            return null;
        }
        EPMRZ mrz;
        try {
            mrz = new EPMRZ(passportNumber, birthDate, expireDate);
        } catch (IllegalArgumentException e) {
            publishProgress("旅券番号、生年月日または有効期限が間違っています\nエラー: " + e);
            return null;
        }

        long start = System.currentTimeMillis();
        JeidReader reader;
        try {
            reader = new JeidReader(mNfcTag);
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            publishProgress("エラー: " + e);
            return null;
        }

        try {
            publishProgress("## カード種別");
            CardType type = reader.detectCardType();
            publishProgress("CardType: " + type);
            if (type != CardType.EP) {
                publishProgress("パスポートではありません");
                return null;
            }

            PassportAP ap = reader.selectPassportAP();
            publishProgress("## Basic Access Control");
            try {
                ap.startBAC(mrz);
            } catch (InvalidBACKeyException e) {
                publishProgress("失敗\n"
                        + "旅券番号、生年月日または有効期限が間違っています");
                return null;
            }
            publishProgress("完了");

            publishProgress("## パスポートから情報を読み取ります");
            EPFiles files = ap.readFiles();
            publishProgress(files.toString());

            publishProgress("## Common Data");
            EPCommonData commonData = files.getCommonData();
            publishProgress(commonData.toString());

            JSONObject obj = new JSONObject();
            publishProgress("## Data Group1");
            EPDataGroup1 dg1 = files.getDataGroup1();
            publishProgress(dg1.getMRZ());

            EPMRZ dg1Mrz = new EPMRZ(dg1.getMRZ());
            obj.put("ep-type", dg1Mrz.getDocumentCode());
            obj.put("ep-issuing-country", dg1Mrz.getIssuingCountry());
            obj.put("ep-passport-number", dg1Mrz.getPassportNumber());
            obj.put("ep-surname", dg1Mrz.getSurname());
            obj.put("ep-given-name", dg1Mrz.getGivenName());
            obj.put("ep-nationality", dg1Mrz.getNationality());
            obj.put("ep-date-of-birth", dg1Mrz.getBirthDate());
            obj.put("ep-sex", dg1Mrz.getSex());
            obj.put("ep-date-of-expiry", dg1Mrz.getExpirationDate());
            obj.put("ep-mrz", dg1.getMRZ());

            EPDataGroup2 dg2 = files.getDataGroup2();
            byte[] jpeg = dg2.getFaceJpeg();
            String src = "data:image/jpeg;base64," + Base64.encodeToString(jpeg, Base64.DEFAULT);
            obj.put("ep-photo", src);

            obj.put("ep-bac-result", true);
            publishProgress("## Passive Authentication");
            try {
                ValidationResult result = files.validate();
                obj.put("ep-pa-result", result.isValid());
                publishProgress("検証結果: " + result.isValid());
            } catch (UnsupportedOperationException e) {
                publishProgress("libjeid-freeでは検証をスキップします");
            }

            publishProgress("## Active Authentication");
            try {
                boolean aaResult = ap.activeAuthentication(files);
                obj.put("ep-aa-result", aaResult);
                publishProgress("検証結果: " + aaResult);
            } catch (UnsupportedOperationException e) {
                publishProgress("libjeid-freeでは検証をスキップします");
            } catch (FileNotFoundException e) {
                publishProgress("Active Authenticationに非対応なパスポートです");
            } catch (TagLostException e) {
                throw e;
            } catch (IOException e) {
                publishProgress("Active Authenticationで不明なエラーが発生しました");
            }

            if (!"JPN".equals(dg1Mrz.getIssuingCountry())) {
                publishProgress("日本発行のパスポートではありません");
                return null;
            }
            return obj;
        } catch (Exception e) {
            Log.e(TAG, "error", e);
            publishProgress("エラー: " + e);
            return null;
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        Log.d(TAG, getClass().getSimpleName() + "#onProgressUpdate()");
        EPReaderActivity activity = (EPReaderActivity)mRef.get();
        if (activity == null) {
            return;
        }
        activity.addMessage(values[0]);
    }

    @Override
    protected void onPostExecute(JSONObject obj) {
        Log.d(TAG, getClass().getSimpleName() + "#onPostExecute()");
        mProgress.dismissAllowingStateLoss();
        EPReaderActivity activity = (EPReaderActivity)mRef.get();
        if (activity == null ||
            activity.isFinishing()) {
            return;
        }
        if (obj == null) {
            return;
        }
        Intent intent = new Intent(activity, EPViewerActivity.class);
        intent.putExtra("json", obj.toString());
        activity.startActivity(intent);
    }
}
