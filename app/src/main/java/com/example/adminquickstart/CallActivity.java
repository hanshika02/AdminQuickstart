package com.example.adminquickstart;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.example.adminquickstart.R;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.Button;
import android.widget.TextView;

public class CallActivity extends AppCompatActivity {

    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        button = (Button) findViewById(R.id.call);

        button.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                String number = ((TextView) findViewById(R.id.primaryPhone)).getText().toString();
                callIntent.setData(Uri.parse("tel:"+number));
                startActivity(callIntent);
            }
        });

    }

}

