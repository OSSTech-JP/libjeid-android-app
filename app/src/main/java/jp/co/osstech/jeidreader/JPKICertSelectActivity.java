package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class JPKICertSelectActivity
    extends BaseActivity
    implements View.OnClickListener
{

    public static final String TAG = "jeidreader";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, getClass().getSimpleName() +
              "#onCreate(" + savedInstanceState + ")");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_cert);

        findViewById(R.id.show_auth_cert_button).setOnClickListener(this);
        findViewById(R.id.show_sign_cert_button).setOnClickListener(this);
        findViewById(R.id.show_auth_ca_cert_button).setOnClickListener(this);
        findViewById(R.id.show_sign_ca_cert_button).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        Intent intent;
        int id = view.getId();
        if(id == R.id.show_auth_cert_button) {
            intent = new Intent(getApplication(), JPKICertReaderActivity.class);
            intent.putExtra("TYPE", "AUTH");
            startActivity(intent);
        } else if (id == R.id.show_sign_cert_button) {
            intent = new Intent(getApplication(), JPKICertReaderActivity.class);
            intent.putExtra("TYPE", "SIGN");
            startActivity(intent);
        } else if (id == R.id.show_auth_ca_cert_button) {
            intent = new Intent(getApplication(), JPKICertReaderActivity.class);
            intent.putExtra("TYPE", "AUTH_CA");
            startActivity(intent);
        } else if (id == R.id.show_sign_ca_cert_button) {
            intent = new Intent(getApplication(), JPKICertReaderActivity.class);
            intent.putExtra("TYPE", "SIGN_CA");
            startActivity(intent);
        }
    }
}
