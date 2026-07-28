package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends BaseActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setClickListener(R.id.in_menu_button, INMenuActivity.class);
        setClickListener(R.id.dl_reader_button, DLReaderActivity.class);
        setClickListener(R.id.indl_reader_button, INDLReaderActivity.class);
        setClickListener(R.id.ep_reader_button, EPReaderActivity.class);
        setClickListener(R.id.rc_menu_button, RCMenuActivity.class);
        setClickListener(R.id.pinstatus_button, PinStatusActivity.class);
    }

    private void setClickListener(int buttonId, final Class<?> activity) {
        findViewById(buttonId).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, activity));
            }
        });
    }
}
