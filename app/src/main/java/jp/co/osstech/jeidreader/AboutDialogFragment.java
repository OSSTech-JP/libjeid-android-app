package jp.co.osstech.jeidreader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.DialogFragment;

public class AboutDialogFragment extends DialogFragment {
    // LibJeIDロゴをタップしてテストモードを切り替えるまでのタップ回数
    private static final int TEST_MODE_TAP_TIMES = 5;

    private int tapCount;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity()
            .getLayoutInflater()
            .inflate(R.layout.about_dialog, null);

        ((TextView)view.findViewById(R.id.app_version))
            .setText(BuildConfig.VERSION_NAME);

        ((TextView)view.findViewById(R.id.libjeid_version))
            .setText(jp.co.osstech.libjeid.BuildConfig.VERSION_NAME);

        view.findViewById(R.id.libjeid_icon)
            .setOnClickListener(v -> onIconClick());

        return new AlertDialog.Builder(getActivity())
            .setTitle("IDリーダー")
            .setView(view)
            .setPositiveButton("閉じる", null)
            .create();
    }

    // ロゴを規定回数タップするとテストモードの有効/無効が切り替わります
    private void onIconClick() {
        tapCount++;
        if (tapCount < TEST_MODE_TAP_TIMES) {
            return;
        }
        tapCount = 0;
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        boolean enabled = !TestMode.isEnabled();
        TestMode.setEnabled(enabled);
        Toast.makeText(activity,
                       enabled ? R.string.test_mode_enabled : R.string.test_mode_disabled,
                       Toast.LENGTH_SHORT).show();
        if (activity instanceof BaseActivity) {
            // 隠しメニューを表示中の画面に反映する
            ((BaseActivity)activity).onTestModeChanged(enabled);
        }
    }
}
