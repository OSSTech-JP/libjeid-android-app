package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import jp.co.osstech.libjeid.JPKIAP;

public class INMenuActivity
    extends BaseActivity
    implements View.OnClickListener
{
    private int clickCnt;
    private long lastClick;
    private boolean devBtnEnabled;
    private static final int REPEATED_TAP_MAX_INTERVAL = 300;
    private static final int REPEATED_TAP_TIMES = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_menu);

        findViewById(R.id.cardinfo_button).setOnClickListener(this);
        findViewById(R.id.selectcert_button).setOnClickListener(this);
        findViewById(R.id.sign_jpki_auth_button).setOnClickListener(this);
        findViewById(R.id.sign_jpki_sign_button).setOnClickListener(this);
        findViewById(R.id.in_test_button).setOnClickListener(this);
        findViewById(R.id.in_dev_button).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        Intent intent;
        int id = view.getId();
        if (id == R.id.cardinfo_button) {
            intent = new Intent(getApplication(), INReaderActivity.class);
            startActivity(intent);
        } else if (id == R.id.selectcert_button) {
            intent = new Intent(getApplication(), JPKICertSelectActivity.class);
            startActivity(intent);
        } else if (id == R.id.sign_jpki_auth_button) {
            intent = new Intent(getApplication(), JPKISignActivity.class);
            intent.putExtra("TYPE", JPKIAP.TYPE_AUTH);
            startActivity(intent);
        } else if (id == R.id.sign_jpki_sign_button) {
            intent = new Intent(getApplication(), JPKISignActivity.class);
            intent.putExtra("TYPE", JPKIAP.TYPE_SIGN);
            startActivity(intent);
        } else if (id == R.id.in_test_button) {
            intent = new Intent(getApplication(), INTestActivity.class);
            startActivity(intent);
        } else if (id == R.id.in_dev_button) {
            if (!devBtnEnabled) {
                long clickTime = System.currentTimeMillis();
                if (clickTime - lastClick < REPEATED_TAP_MAX_INTERVAL) {
                    clickCnt++;
                    if (clickCnt == REPEATED_TAP_TIMES) {
                        findViewById(R.id.in_dev_button).setAlpha((float) 1);
                        devBtnEnabled = true;
                        clickCnt = 0;
                    }
                } else {
                    clickCnt = 1;
                }
                lastClick = clickTime;
            }
            if (devBtnEnabled) {
                Button authBtn = findViewById(R.id.sign_jpki_auth_button);
                Button signBtn = findViewById(R.id.sign_jpki_sign_button);
                Button testBtn = findViewById(R.id.in_test_button);
                if (authBtn.getVisibility() == View.INVISIBLE
                        || authBtn.getVisibility() == View.GONE) {
                    authBtn.setVisibility(View.VISIBLE);
                    signBtn.setVisibility(View.VISIBLE);
                    testBtn.setVisibility(View.VISIBLE);
                } else {
                    authBtn.setVisibility(View.GONE);
                    signBtn.setVisibility(View.GONE);
                    testBtn.setVisibility(View.GONE);
                }
            }
        }
    }
}
