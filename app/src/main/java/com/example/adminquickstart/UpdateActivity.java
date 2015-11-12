package com.example.adminquickstart;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class UpdateActivity extends AppCompatActivity {

    String name, address, email, phone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle userInfo = getIntent().getExtras();
        name = userInfo.getString("name");
        address = userInfo.getString("address");
        email = userInfo.getString("email");
        phone = userInfo.getString("phone");
        setContentView(R.layout.activity_update);

        ((EditText) findViewById(R.id.mandatory)).setText(name);
        ((EditText) findViewById(R.id.phone)).setText(phone);
        ((TextView) findViewById(R.id.email)).setText(email);
    }

    public void saveDetails(View view) {
        EditText nameText;
        nameText = (EditText)findViewById(R.id.mandatory);
        String firstName = nameText.getText().toString();
        if("".equals(firstName)) {
            nameText.setError("Name is mandatory!!!");
        }
        else {
            name = nameText.getText().toString();
            StringBuilder sb = new StringBuilder(((EditText) findViewById(R.id.addressLine1)).getText().toString());
            sb.append(" ");
            sb.append(((EditText) findViewById(R.id.addressLine2)).getText().toString());
            address = sb.toString();
            phone = ((EditText) findViewById(R.id.phone)).getText().toString();

            Intent intent = new Intent(UpdateActivity.this, DisplayContactsActivity.class);
            intent.putExtra("name", name);
            intent.putExtra("address", address);
            intent.putExtra("phone", phone);

            startActivity(intent);
            finish();

        }
    }

}
