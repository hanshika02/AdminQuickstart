package com.example.adminquickstart;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.UserName;
import com.google.gdata.util.ServiceException;

import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;

public class UpdateActivity extends AppCompatActivity {

    /*
        Intetionally didn't create a class to store these values.
        Because passing an object with intent was adding A LOT of code making this UpdataActivity.java double its size.
        Unless we have high security issues (like checkmarx ka chu**yapa), we can ignore making objects :P
     */

    String name, address, email, phone;
    private GoogleCredential mCredential;
    private String my_file_name = "/home/zemoso05/AndroidStudioProjects/AdminQuickstart/app/src/main/assets/"
            + "assets/myprojectkey.p12";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
//        mCredential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Arrays.asList(SCOPES))
//                .setBackOff(new ExponentialBackOff())
//                .setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, null));
        JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
//        try {
//            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
//        } catch (GeneralSecurityException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        ArrayList<String> scopeList = new ArrayList<>();
        scopeList.add(DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY);
        scopeList.add(DirectoryScopes.ADMIN_DIRECTORY_USER);
        scopeList.add(DirectoryScopes.ADMIN_DIRECTORY_USER_SECURITY);
        try {
            //AssetManager am = getAssets();
            //InputStream inputStream = am.open(my_file_name);
            InputStream inputStream = getResources().openRawResource(R.raw.myprojectkey);
            java.io.File file = createFileFromInputStream(inputStream);
            mCredential = new GoogleCredential.Builder()
                    .setTransport(httpTransport)
                    .setJsonFactory(JSON_FACTORY)
                    .setServiceAccountId("account-1@deft-observer-111811.iam.gserviceaccount.com")
                    .setServiceAccountScopes(scopeList)
                    .setServiceAccountUser(email)
                    .setServiceAccountPrivateKeyFromP12File(file) //password: notasecret
                    .build();

        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

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

    private File createFileFromInputStream(InputStream inputStream) {

        String path = "";

        File file = new File(Environment.getExternalStorageDirectory(),
                "KeyHolder/KeyFile/");
        if (!file.exists()) {
            if (!file.mkdirs())
                Log.d("KeyHolder", "Folder not created");
            else
                Log.d("KeyHolder", "Folder created");
        } else
            Log.d("KeyHolder", "Folder present");

        path = file.getAbsolutePath();

        try {
            File f = new File(path+"/MyKey");
            OutputStream outputStream = new FileOutputStream(f);
            byte buffer[] = new byte[1024];
            int length = 0;

            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            return f;
        } catch (IOException e) {
            // Logging exception
            e.printStackTrace();
        }

        return null;
    }

    //name is a mandatory field, email we won't want to change because I guess that would be the only unique thing.
    // phone and address are the only things you can update.

    public void saveDetails(View view) throws IOException, ServiceException {
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
            new MakePostRequestTask(mCredential).execute();
        }

    }

    private class MakePostRequestTask extends AsyncTask<Void,String,String>{

        private com.google.api.services.admin.directory.Directory mService = null;

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        public MakePostRequestTask(GoogleCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

            mService = new com.google.api.services.admin.directory.Directory.Builder(transport, jsonFactory, credential)
                    .setApplicationName("Directory API Android Quickstart")
                    .setHttpRequestInitializer(credential).build();

        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        protected String doInBackground(Void... params) {
            try {
                putDataToGoogleServer();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }

            return null;
        }

        private void putDataToGoogleServer() throws Throwable{

            User updatedUser = new User();
            Log.i("Updated name", name);
            UserName updatedName = new UserName();
            updatedName.setFullName(name);
            updatedUser.setName(updatedName);
            Log.i("Updated address", address);
            updatedUser.setAddresses(address);
            Log.i("Updated phone", phone);
            updatedUser.setPhones(phone);
            User response = new User();
            boolean errorOccured = false;
            try {
                response = mService.users().patch(email, updatedUser).execute();
            }catch(final GoogleJsonResponseException e){
                UpdateActivity.this.runOnUiThread(new Runnable() {
                    @SuppressLint("LongLogTag")
                    public void run() {
                        Toast.makeText(UpdateActivity.this, "Update Failed, Authentication Error", Toast.LENGTH_LONG).show();
                        System.out.println(e);
                    }
                });
                errorOccured = true;
                UpdateActivity.this.finish();
            }

            if(response!=null && !errorOccured){
                UpdateActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(UpdateActivity.this, "Successfully updated", Toast.LENGTH_SHORT).show();
                    }
                });
                Log.i("POST REQUEST RESPONSE", response.toPrettyString());
            }else if(!errorOccured){
                UpdateActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(UpdateActivity.this, "Update Failed", Toast.LENGTH_SHORT).show();
                    }
                });
                Log.i("POST REQUEST RESPONSE", "EMPTY..............");
            }
            UpdateActivity.this.finish();

        }
    }


}
