package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.graphics.Bitmap;
import android.nfc.Tag;
import android.util.Base64;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import jp.co.osstech.libjeid.*;
import jp.co.osstech.libjeid.dl.*;
import jp.co.osstech.libjeid.util.BitmapARGB;
import jp.co.osstech.libjeid.util.Hex;
import org.json.JSONArray;
import org.json.JSONObject;

public class INDLReaderTask
    implements Runnable
{
    private static final String TAG = MainActivity.TAG;
    private static final String DPIN = "****";
    private Tag nfcTag;
    private String pin;
    private INDLReaderActivity activity;

    public INDLReaderTask(INDLReaderActivity activity, Tag nfcTag) {
        this.activity = activity;
        this.nfcTag = nfcTag;
    }

    private void publishProgress(String msg) {
        this.activity.print(msg);
    }

    public void run() {
        Log.d(TAG, getClass().getSimpleName() + "#run()");
        this.activity.clear();
        pin = activity.getPin();
        activity.hideKeyboard();
        publishProgress("# 読み取り開始、カードを離さないでください");
        // 読み取り中ダイアログを表示
        ProgressDialogFragment progress = new ProgressDialogFragment();
        progress.show(activity.getSupportFragmentManager(), "progress");

        try {
            JeidReader reader = new JeidReader(this.nfcTag);
            publishProgress("## マイナ運転免許証の読み取り開始");
            CardType type = reader.detectCardType();
            publishProgress("CardType: " + type);
            if (type != CardType.IN) {
                publishProgress("マイナンバーカードではありません");
                return;
            }
            INDriverLicenseAP ap = reader.selectINDriverLicenseAP();
            DLPinSetting pinSetting = ap.readPinSetting();
            publishProgress("## 暗証番号(PIN)設定");
            publishProgress(pinSetting.toString());

            if (pin.isEmpty()) {
                publishProgress("暗証番号を入力してください");
                return;
            }

            if (!pinSetting.isPinSet()) {
                publishProgress("暗証番号設定がfalseのため、デフォルトPINの「****」を暗証番号として使用します\n");
                pin = DPIN;
            }

            try {
                ap.verifyPin(pin);
            } catch (InvalidPinException e) {
                activity.showInvalidPinDialog(e);
                return;
            }

            // 読み出し可能なファイルをすべて読み出します。
            INDLFiles files = ap.readFiles();

            // 免許情報の取得
            INDLEntries entries = files.getEntries();
            publishProgress(entries.toString());
            JSONObject obj = new JSONObject();
            obj.put("color-class", entries.getColorClass());
            obj.put("expire-date", entries.getExpireDate());
            obj.put("conditions", new JSONArray(entries.getConditions()));
            obj.put("license-number", entries.getLicenseNumber());
            JSONArray categories = new JSONArray();
            for (INDLCategory category : entries.getCategories()) {
                JSONObject categoryObj = new JSONObject();
                categoryObj.put("tag", category.getTag());
                categoryObj.put("name", category.getName());
                categoryObj.put("licensed", category.isLicensed());
                // 「二・小・原」「他」「二種」のみ取得年月日が記録されており、
                // それ以外の免許種別に年月日は記録されていません。
                DLDate date = category.getDate();
                if (date != null) {
                    categoryObj.put("date", category.getDate().toString());
                }
                if (category.isLicensed()) {
                    categories.put(categoryObj);
                }
            }
            obj.put("categories", categories);

            INDLPhoto photo = entries.getPhoto();
            publishProgress("写真のデコード中...");
            BitmapARGB argb = photo.getPhotoBitmapARGB();
            Bitmap bitmap = Bitmap.createBitmap(argb.getData(),
                                                argb.getWidth(),
                                                argb.getHeight(),
                                                Bitmap.Config.ARGB_8888);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            byte[] jpeg = os.toByteArray();
            obj.put("photo", Base64.encodeToString(jpeg, Base64.DEFAULT));

            // 電子署名
            INDLSignature signature = files.getSignature();
            publishProgress("# 電子署名");
            publishProgress(signature.toString());
            obj.put("signature-issuer", signature.getIssuer());
            obj.put("signature-subject", signature.getSubject());
            obj.put("signature-ski", Hex.encode(signature.getSubjectKeyIdentifier()));

            // 真正性検証
            ValidationResult result = files.validate();
            obj.put("signature-valid", result.isValid());
            publishProgress("真正性検証結果: " + result);

            // ViewerにJSONをデータを引き渡して起動
            Intent intent = new Intent(activity, INDLViewerActivity.class);
            intent.putExtra("json", obj.toString());
            activity.startActivity(intent);
        } catch (java.io.FileNotFoundException e) {
            publishProgress("マイナ運転免許証ではありません。");
        } catch (Exception e) {
            Log.e(TAG, "error", e);
            publishProgress("エラー: " + e);
        } finally {
            progress.dismissAllowingStateLoss();
        }
    }
}
