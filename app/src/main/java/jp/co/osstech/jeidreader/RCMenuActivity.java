package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class RCMenuActivity
    extends BaseActivity
    implements View.OnClickListener
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rc_menu);

        findViewById(R.id.rc1_reader_button).setOnClickListener(this);
        findViewById(R.id.rc2_reader_button).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        Intent intent;
        int id = view.getId();
        if (id == R.id.rc1_reader_button) {
            intent = new Intent(getApplication(), RCReaderActivity.class);
            startActivity(intent);
        } else if (id == R.id.rc2_reader_button) {
            intent = new Intent(getApplication(), RC2ReaderActivity.class);
            startActivity(intent);
        }
    }
}
