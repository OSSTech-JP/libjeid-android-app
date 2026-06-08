package jp.co.osstech.jeidreader;

import android.nfc.Tag;
import android.util.Log;
import java.lang.ref.WeakReference;
import jp.co.osstech.libjeid.CardType;
import jp.co.osstech.libjeid.DriverLicenseAP;
import jp.co.osstech.libjeid.INTextAP;
import jp.co.osstech.libjeid.INVisualAP;
import jp.co.osstech.libjeid.INDriverLicenseAP;
import jp.co.osstech.libjeid.JPKIAP;
import jp.co.osstech.libjeid.JeidReader;
import jp.co.osstech.libjeid.ResidenceCardAP;
import jp.co.osstech.libjeid.rc.RCCardType;
import jp.co.osstech.libjeid.ResidenceCard2AP;
import jp.co.osstech.libjeid.rc2.RC2CardType;

public class PinStatusTask
    implements Runnable
{
    private static final String TAG = MainActivity.TAG;
    private final WeakReference<PinStatusActivity> activityRef;
    private Tag nfcTag;

    public PinStatusTask(PinStatusActivity activity,
                         Tag nfcTag) {
        this.activityRef = new WeakReference<>(activity);
        this.nfcTag = nfcTag;
    }

    private void publishProgress(String msg) {
        PinStatusActivity activity = activityRef.get();
        if (activity != null) {
            activity.print(msg);
        }
    }

    public void run() {
        Log.d(TAG, getClass().getSimpleName() + "#run()");
        PinStatusActivity activity = activityRef.get();
        if (activity == null) {
            return;
        }
        activity.clear();
        ProgressDialogFragment progress = new ProgressDialogFragment();
        activity.runOnUiThread(() -> {
            progress.show(activity.getSupportFragmentManager(), "progress");
        });
        try {
            JeidReader reader = new JeidReader(this.nfcTag);
            int counter;

            CardType type = reader.detectCardType();
            switch (type) {
            case IN:
                publishProgress("カード種別: マイナンバーカード");
                INTextAP textAP = reader.selectINTextAP();
                counter = textAP.getPin();
                publishProgress("券面入力補助AP 暗証番号: " + counter);
                counter = textAP.getPinA();
                publishProgress("券面入力補助AP 照合番号A: " + counter);
                counter = textAP.getPinB();
                publishProgress("券面入力補助AP 照合番号B: " + counter);

                INVisualAP visualAP = reader.selectINVisualAP();
                counter = visualAP.getPinA();
                publishProgress("券面AP 照合番号A: " + counter);
                counter = visualAP.getPinB();
                publishProgress("券面AP 照合番号B: " + counter);

                JPKIAP jpkiAP = reader.selectJPKIAP();
                counter = jpkiAP.getAuthPin();
                publishProgress("JPKI-AP ユーザー認証PIN: " + counter);
                counter = jpkiAP.getSignPin();
                publishProgress("JPKI-AP デジタル署名PIN: " + counter);
                try {
                    INDriverLicenseAP indAP = reader.selectINDriverLicenseAP();
                    counter = indAP.getPin();
                    publishProgress("マイナ運転免許証AP 暗証番号: " + counter);
                } catch (java.io.FileNotFoundException e) {
                    // マイナ運転免許証APが無い
                }
                break;
            case DL:
                publishProgress("カード種別: 運転免許証");
                DriverLicenseAP dlAP = reader.selectDriverLicenseAP();
                counter = dlAP.getPin1();
                publishProgress("暗証番号1: " + counter);
                counter = dlAP.getPin2();
                publishProgress("暗証番号2: " + counter);
                break;
            case JUKI:
                publishProgress("カード種別: 住基カード");
                break;
            case EP:
                publishProgress("カード種別: パスポート");
                break;
            case RC:
                publishProgress("カード種別: 在留カード");
                ResidenceCardAP rcAP = reader.selectResidenceCardAP();
                RCCardType rcType = rcAP.readCardType();
                publishProgress("在留カード種別: " + rcType.toString());
                break;
            case RC2:
                publishProgress("カード種別: 第2世代在留カード");
                ResidenceCard2AP rc2AP = reader.selectResidenceCard2AP();
                RC2CardType rc2Type = rc2AP.readCardType();
                publishProgress("在留カード種別: " + rc2Type.toString());
                break;
            case RCS:
                publishProgress("カード種別: 特定在留カード");
                break;

            default:
                publishProgress("カード種別: 不明");
                break;
            }
            publishProgress("読み取り完了");
        } catch (Exception e) {
            Log.e(TAG, "error at " + getClass().getSimpleName(), e);
            publishProgress(e.toString());
        }
        activity.runOnUiThread(() -> {
            progress.dismissAllowingStateLoss();
        });
    }
}
