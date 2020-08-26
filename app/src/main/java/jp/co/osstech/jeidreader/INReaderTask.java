package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.graphics.Bitmap;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import jp.co.osstech.libjeid.CardType;
import jp.co.osstech.libjeid.INTextAP;
import jp.co.osstech.libjeid.INVisualAP;
import jp.co.osstech.libjeid.InvalidPinException;
import jp.co.osstech.libjeid.JPKIAP;
import jp.co.osstech.libjeid.JPKICertificate;
import jp.co.osstech.libjeid.JeidReader;
import jp.co.osstech.libjeid.ValidationResult;
import jp.co.osstech.libjeid.in.INTextAttributes;
import jp.co.osstech.libjeid.in.INTextFiles;
import jp.co.osstech.libjeid.in.INTextMyNumber;
import jp.co.osstech.libjeid.in.INVisualEntries;
import jp.co.osstech.libjeid.in.INVisualFiles;
import jp.co.osstech.libjeid.in.INVisualMyNumber;
import jp.co.osstech.libjeid.util.BitmapARGB;
import org.json.JSONObject;

public class INReaderTask extends AsyncTask<Void, String, JSONObject>
{
    private static final String TAG = MainActivity.TAG;
    private WeakReference mRef;
    private Tag mNfcTag;
    private String mPin;
    private ProgressDialogFragment mProgress;
    private InvalidPinException ipe;

    public INReaderTask(INReaderActivity activity, Tag nfcTag) {
        mRef = new WeakReference<INReaderActivity>(activity);
        mNfcTag = nfcTag;
    }

    @Override
    protected void onPreExecute() {
        INReaderActivity activity = (INReaderActivity)mRef.get();
        if (activity == null) {
            return;
        }
        activity.setMessage("# 読み取り開始、カードを離さないでください");
        activity.hideKeyboard();
        mPin = activity.getPin();
        mProgress = new ProgressDialogFragment();
        mProgress.show(activity.getSupportFragmentManager(), "progress");
    }

    @Override
    protected JSONObject doInBackground(Void... args) {
        if (mPin.isEmpty() || mPin.length() != 4) {
            publishProgress("4桁の暗証番号を入力してください。");
            return null;
        }
        try {
            JeidReader reader = new JeidReader(mNfcTag);
            CardType type = reader.detectCardType();
            publishProgress("## カード種別" + type);
            publishProgress("CardType: " + type);
            if (type != CardType.IN) {
                publishProgress("マイナンバーカードではありません");
                return null;
            }
            publishProgress("## 券面入力補助APから情報を取得します");
            INTextAP textAp = reader.selectINTextAP();
            try {
                textAp.verifyPin(mPin);
            } catch (InvalidPinException e) {
                ipe = e;
                return null;
            }
            INTextFiles textFiles = textAp.readFiles();
            publishProgress("完了");
            JSONObject obj = new JSONObject();
            try {
                INTextMyNumber textMyNumber = textFiles.getMyNumber();
                obj.put("cardinfo-mynumber", textMyNumber.getMyNumber());
            } catch (FileNotFoundException | UnsupportedOperationException ue) {
                // 無償版では個人番号を取得出来ません。
            }
            publishProgress("## 4情報");
            INTextAttributes textAttrs = textFiles.getAttributes();
            obj.put("cardinfo-name", textAttrs.getName());
            obj.put("cardinfo-birth", textAttrs.getBirth());
            obj.put("cardinfo-sex", textAttrs.getSexString());
            obj.put("cardinfo-addr", textAttrs.getAddr());
            publishProgress(textAttrs.toString());

            try {
                publishProgress("## 券面入力補助APの真正性検証");
                ValidationResult validationResult = textFiles.validate();
                publishProgress("検証結果: " + validationResult.toString());
                obj.put("textap-validation-result", validationResult.isValid());
            } catch (UnsupportedOperationException ue) {
                // 無償版では真正性検証をサポートしていません。
            }

            publishProgress("## 券面APから情報を取得します");
            INVisualAP visualAp = reader.selectINVisualAP();
            visualAp.verifyPin(mPin);
            INVisualFiles visualFiles = visualAp.readFiles();
            publishProgress("完了");

            INVisualEntries visualEntries = visualFiles.getEntries();
            String expire = visualEntries.getExpire();
            obj.put("cardinfo-expire", expire);
            obj.put("cardinfo-birth2", visualEntries.getBirth());
            obj.put("cardinfo-sex2", visualEntries.getSexString());
            obj.put("cardinfo-name-image", "data:image/png;base64,"
                    + Base64.encodeToString(visualEntries.getName(), Base64.DEFAULT));
            obj.put("cardinfo-address-image", "data:image/png;base64,"
                    + Base64.encodeToString(visualEntries.getAddr(), Base64.DEFAULT));

            publishProgress("## 写真のデコード");
            BitmapARGB argb = visualEntries.getPhotoBitmapARGB();
            Bitmap bitmap = Bitmap.createBitmap(argb.getData(),
                                                argb.getWidth(),
                                                argb.getHeight(),
                                                Bitmap.Config.ARGB_8888);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            byte[] jpeg = os.toByteArray();
            String src = "data:image/jpeg;base64," + Base64.encodeToString(jpeg, Base64.DEFAULT);
            obj.put("cardinfo-photo", src);
            publishProgress("完了");

            try {
                publishProgress("## 券面APの真正性検証");
                ValidationResult validationResult = visualFiles.validate();
                publishProgress("検証結果: " + validationResult.toString());
                obj.put("visualap-validation-result", validationResult.isValid());
            } catch (UnsupportedOperationException ue) {
                // 無償版では真正性検証をサポートしていません。

            }
            try {
                INVisualMyNumber visualMyNumber = visualFiles.getMyNumber();
                obj.put("cardinfo-mynumber-image", "data:image/png;base64,"
                        + Base64.encodeToString(visualMyNumber.getMyNumber(), Base64.DEFAULT));
            } catch (FileNotFoundException | UnsupportedOperationException ue) {
                // 無償版では個人番号(画像)を取得できません。
            }
            publishProgress("## ユーザー認証用証明書の有効期限を取得します");
            JPKIAP jpkiAP = reader.selectJPKIAP();
            JPKICertificate cert = jpkiAP.getAuthCert();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
            String certExpireDate = sdf.format(cert.getNotAfter());
            obj.put("cardinfo-cert-expire", certExpireDate);
            publishProgress("完了");

            return obj;
        } catch (Exception e) {
            Log.e(TAG, "error at CardInfoTask#doInBackground()", e);
            publishProgress("エラー: " + e);
            return null;
        }
    }

    protected void onProgressUpdate(String... values) {
        INReaderActivity activity = (INReaderActivity)mRef.get();
        if (activity == null) {
            return;
        }
        activity.addMessage(values[0]);
    }

    @Override
    protected void onPostExecute(JSONObject obj) {
        mProgress.dismiss();
        INReaderActivity activity = (INReaderActivity)mRef.get();
        if (activity == null ||
            activity.isFinishing()) {
            return;
        }
        if (ipe != null) {
            int counter = ipe.getCounter();
            String title;
            String msg;
            if (ipe.isBlocked()) {
                title = "PINがブロックされています";
                msg = "市区町村窓口でブロック解除の申請をしてください。";
            } else {
                title = "PINが間違っています";
                msg = "PINを正しく入力してください。";
                msg += "のこり" + counter + "回間違えるとブロックされます。";
            }
            activity.addMessage(title);
            activity.addMessage(msg);
            activity.showInvalidPinDialog(title, msg);
            return;
        }
        if (obj == null) {
            activity.addMessage("エラー: カードを読み取れませんでした。");
            return;
        }

        Intent intent = new Intent(activity, INViewerActivity.class);
        intent.putExtra("json", obj.toString());
        activity.startActivity(intent);
    }
}
