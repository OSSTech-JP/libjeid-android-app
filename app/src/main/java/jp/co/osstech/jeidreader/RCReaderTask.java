package jp.co.osstech.jeidreader;

import android.graphics.Bitmap;
import android.nfc.Tag;
import android.util.Base64;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import jp.co.osstech.libjeid.CardType;
import jp.co.osstech.libjeid.InvalidACKeyException;
import jp.co.osstech.libjeid.JeidReader;
import jp.co.osstech.libjeid.RCKey;
import jp.co.osstech.libjeid.ResidenceCardAP;
import jp.co.osstech.libjeid.ValidationResult;
import jp.co.osstech.libjeid.rc.*;
import jp.co.osstech.libjeid.rc.RCAddress;
import jp.co.osstech.libjeid.util.BitmapARGB;
import org.json.JSONObject;

public class RCReaderTask
    implements Runnable
{
    private static final String TAG = MainActivity.TAG;
    private final WeakReference<RCReaderActivity> activityRef;
    private Tag nfcTag;
    private String rcNumber;

    public RCReaderTask(RCReaderActivity activity, Tag nfcTag, String rcNumber) {
        this.activityRef = new WeakReference<>(activity);
        this.nfcTag = nfcTag;
        this.rcNumber = rcNumber;
    }

    private void publishProgress(String msg) {
        RCReaderActivity activity = activityRef.get();
        if (activity != null) {
            activity.print(msg);
        }
    }

    public void run() {
        Log.d(TAG, getClass().getSimpleName() + "#run()");
        RCReaderActivity activity = activityRef.get();
        if (activity == null) {
            return;
        }
        activity.clear();
        publishProgress("# 読み取り開始、カードを離さないでください");

        if (rcNumber.isEmpty()) {
            publishProgress("在留カード番号または特別永住者証明書番号を設定してください");
            return;
        }

        ProgressDialogFragment progress = new ProgressDialogFragment();
        activity.runOnUiThread(() -> {
            progress.show(activity.getSupportFragmentManager(), "progress");
        });
        long start = System.currentTimeMillis();
        try {
            JeidReader reader = new JeidReader(nfcTag);
            publishProgress("## カード種別");
            CardType type = reader.detectCardType();
            publishProgress("CardType: " + type);
            if (type != CardType.RC) {
                publishProgress("在留カード/特別永住者証明書ではありません");
                return;
            }
            ResidenceCardAP ap = reader.selectResidenceCardAP();
            RCCommonData commonData = ap.readCommonData();
            publishProgress("commonData: " + commonData);
            RCCardType cardType = ap.readCardType();
            publishProgress("cardType: " + cardType);
            RCKey rckey = new RCKey(rcNumber);
            publishProgress("## セキュアメッセージング用の鍵交換&認証");
            try {
                ap.startAC(rckey);
            } catch (InvalidACKeyException e) {
                publishProgress("在留カード番号または特別永住者証明書番号が間違っています");
                return;
            }

            publishProgress("## カードから情報を取得します");
            RCFiles files = ap.readFiles();
            JSONObject obj = new JSONObject();
            obj.put("rc-card-type", cardType.getType());
            RCCardEntries cardEntries = files.getCardEntries();
            byte[] png = cardEntries.toPng();
            String src = "data:image/png;base64," + Base64.encodeToString(png, Base64.DEFAULT);
            obj.put("rc-front-image", src);

            publishProgress("## 写真のデコード");
            RCPhoto photo = files.getPhoto();
            BitmapARGB argb = photo.getPhotoBitmapARGB();
            if (argb != null) {
                Bitmap bitmap = Bitmap.createBitmap(argb.getData(),
                        argb.getWidth(),
                        argb.getHeight(),
                        Bitmap.Config.ARGB_8888);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                byte[] jpeg = os.toByteArray();
                src = "data:image/jpeg;base64," + Base64.encodeToString(jpeg, Base64.DEFAULT);
                obj.put("rc-photo", src);
            }
            publishProgress("完了");
            publishProgress("## 住居地（裏面追記）");
            RCAddress address = files.getAddress();
            publishProgress(address.toString());

            if ("1".equals(cardType.getType())) {
                publishProgress("## 裏面資格外活動包括許可欄");
                RCComprehensivePermission comprehensivePermission = files.getComprehensivePermission();
                publishProgress(comprehensivePermission.toString());

                publishProgress("## 裏面資格外活動個別許可欄");
                RCIndividualPermission individualPermission = files.getIndividualPermission();
                publishProgress(individualPermission.toString());

                publishProgress("## 裏面在留期間等更新申請欄");
                RCUpdateStatus updateStatus = files.getUpdateStatus();
                publishProgress(updateStatus.toString());
            }

            publishProgress("## 電子署名");
            RCSignature signature = files.getSignature();
            publishProgress(signature.toString());

            // 真正性検証
            publishProgress("## 真正性検証");
            try {
                ValidationResult result = files.validate();
                obj.put("rc-valid", result.isValid());
                obj.put("rc-validation-result", result.toString());
                publishProgress("真正性検証結果: " + result);
            } catch(UnsupportedOperationException e) {
                // free版の場合、真正性検証処理で
                // UnsupportedOperationException が返ります。
            }
            // ビューアは別Activityではなく読み取り画面内のFragmentとして表示する
            final String json = obj.toString();
            activity.runOnUiThread(() -> activity.showViewer(json));
        } catch (Exception e) {
            Log.e(TAG, "error", e);
            publishProgress("エラー: " + e);
        } finally {
            activity.runOnUiThread(() -> {
                progress.dismissAllowingStateLoss();
            });
        }
    }
}
