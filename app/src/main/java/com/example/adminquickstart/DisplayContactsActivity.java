package com.example.adminquickstart;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;

import java.io.InputStream;
import java.lang.Object;

import com.google.api.services.admin.directory.DirectoryScopes;

import com.google.api.services.admin.directory.model.*;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DisplayContactsActivity extends Activity {
    GoogleAccountCredential mCredential;
    private TextView mOutputText;
    ProgressDialog mProgress;

    boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }else{
            setContentView(R.layout.content_main2);
            new MakeRequestTask(mCredential).execute();
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK twice to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }


    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static String email;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY };
    private List<User> users;
    private ListView mOutputList;

    /**
     * Create the main activity.
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        LinearLayout activityLayout = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        activityLayout.setLayoutParams(lp);
        activityLayout.setOrientation(LinearLayout.VERTICAL);
        activityLayout.setPadding(16, 16, 16, 16);

        ViewGroup.LayoutParams tlp = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        mOutputText = new TextView(this);
        mOutputText.setLayoutParams(tlp);
        mOutputText.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        mOutputText.setPadding(16, 16, 16, 16);
        mOutputText.setVerticalScrollBarEnabled(true);
        mOutputText.setMovementMethod(new ScrollingMovementMethod());
        activityLayout.addView(mOutputText);

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling Directory API ...");

        setContentView(activityLayout);

        // Initialize credentials and service object.
        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        mCredential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, null));
    }


    /**
     * Called whenever this activity is pushed to the foreground, such as after
     * a call to onCreate().
     */
    @Override
    protected void onResume() {
        super.onResume();
        Bundle userInfo = getIntent().getExtras();
        if(userInfo != null) {
            // TODO: 12/11/15 userInfo.getString("key") would give us all the required values to be updated.


        }

        if (isGooglePlayServicesAvailable()) {
            refreshResults();
        } else {
            mOutputText.setText("Google Play Services required: " +
                    "after installing, close and relaunch this app.");
        }
    }


    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    isGooglePlayServicesAvailable();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        mCredential.setSelectedAccountName(accountName);
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    mOutputText.setText("Account unspecified.");
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode != RESULT_OK) {
                    chooseAccount();
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Attempt to get a set of data from the Directory API to display. If the
     * email address isn't known yet, then call chooseAccount() method so the
     * user can pick an account.
     */
    private void refreshResults() {
        if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else {
            if (isDeviceOnline()) {
                new MakeRequestTask(mCredential).execute();
            } else {
                mOutputText.setText("No network connection available.");
            }
        }
    }

    /**
     * Starts an activity in Google Play Services so the user can pick an
     * account.
     */
    private void chooseAccount() {
        startActivityForResult(
                mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date. Will
     * launch an error dialog for the user to update Google Play Services if
     * possible.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        final int connectionStatusCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        } else if (connectionStatusCode != ConnectionResult.SUCCESS ) {
            return false;
        }
        return true;
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                connectionStatusCode,
                DisplayContactsActivity.this,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * An asynchronous task that handles the Directory API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */

    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.admin.directory.Directory mService = null;
        private Exception mLastError = null;

        public MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.admin.directory.Directory.Builder(transport, jsonFactory, credential)
                    .setApplicationName("Directory API Android Quickstart")
                    .build();
        }

        /**
         * Background task to call Directory API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of the names of the first 10 users in the domain.
         * @return List of Strings user names.
         * @throws IOException
         */
        private List<String> getDataFromApi() throws IOException {
            // Get the first 10 users in the domain.
            Users result = mService.users().list()
                    .setCustomer("my_customer")
                    .setOrderBy("email")
                    .setViewType("domain_public")
                    .execute();
            users = result.getUsers();
            List<String> names = new ArrayList<>();
            if (users != null) {
                for (User user : users) {
                    names.add(user.getName().getFullName());
                }
            }
            return names;
        }


        @Override
        protected void onPreExecute() {
            mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgress.hide();
            if (output == null || output.size() == 0) {
                mOutputText.setText("No results returned.");
            } else {
                //output.add(0, "Data retrieved using the Directory API:");
                postExecute(output);
            }
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            DisplayContactsActivity.REQUEST_AUTHORIZATION);
                } else {
                    mOutputText.setText("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                mOutputText.setText("Request cancelled.");
            }
            setContentView(R.layout.content_main2);
        }
    }

    private class PhoneCallListener extends PhoneStateListener {
        private boolean isPhoneCalling = false;

        String LOG_TAG = "Logging:";

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if(TelephonyManager.CALL_STATE_RINGING == state){
                Log.i(LOG_TAG,"Ringing number:"+incomingNumber);
            }

            if(TelephonyManager.CALL_STATE_OFFHOOK == state){
                Log.i(LOG_TAG,"OFFHOOK");
                isPhoneCalling = true;
            }

            if(TelephonyManager.CALL_STATE_IDLE == state){
                Log.i(LOG_TAG, "IDLE");
                if(isPhoneCalling){
                    Log.i(LOG_TAG, "restart app");

                    Intent i = getBaseContext().getPackageManager()
                            .getLaunchIntentForPackage(getBaseContext().getPackageName());
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);

                    isPhoneCalling = false;
                }
            }
        }
    }

    private void postExecute(List<String> output){
        mOutputText.setText("");
        ArrayAdapter<String> arrayAdapter;
        setContentView(R.layout.content_main2);
        mOutputList = (ListView) findViewById(R.id.list);
        //arrayAdapter = new ArrayAdapter<>(ContactsActivity.this,android.R.layout.simple_expandable_list_item_1,new String[] {"Names"});
        arrayAdapter = new ArrayAdapter<>(DisplayContactsActivity.this,android.R.layout.simple_expandable_list_item_1,output);
        mOutputList.setAdapter(arrayAdapter);
        mOutputList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            class DownloadImageTask extends AsyncTask<String,Void,Bitmap>{
                ImageView bmImage;
                public DownloadImageTask(ImageView bmImage){
                    this.bmImage=bmImage;
                }

                @Override
                protected Bitmap doInBackground(String... urls) {
                    String urlDisplay = urls[0];
                    Bitmap mIcon11 = null;
                    try{
                        InputStream in = new java.net.URL(urlDisplay).openStream();
                        mIcon11 = BitmapFactory.decodeStream(in);
                    } catch (Exception e) {
                        Log.e("Error", e.getMessage());
                        e.printStackTrace();
                    }
                    return mIcon11;
                }

                protected void onPostExecute(Bitmap result) {
                    bmImage.setImageBitmap(result);
                }
            }

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setContentView(R.layout.displaydetails);

                TextView name = (TextView)findViewById(R.id.nameInDetails);
                name.setGravity(Gravity.CENTER);

                final User selectedUser = users.get(position);
                final Button button = (Button) findViewById(R.id.call);
                if(selectedUser.getName().getFullName()!=null) {
                    ((TextView) findViewById(R.id.title)).setText(selectedUser.getName().getFullName());
                }
                if(selectedUser.getPrimaryEmail()!=null) {
                    ((TextView) findViewById(R.id.email)).setText(selectedUser.getPrimaryEmail());
                }
                final Object userPhones = selectedUser.getPhones();
                if (userPhones!=null) {
                    String value = userPhones.toString();
                    ((TextView) findViewById(R.id.primaryPhone)).setText(value.substring(8,22));
                    //((TextView) findViewById(R.id.primaryPhone)).setText(value);
                }else{
                    button.setEnabled(false);
                    button.setBackgroundColor(Color.parseColor("grey"));
                }
                Object userAddresses = selectedUser.getAddresses();
                if(userAddresses!=null) {
                    String value = userAddresses.toString();
                    ((TextView) findViewById(R.id.address)).setText(value.substring(23, value.length() - 2));
//                            ((TextView) findViewById(R.id.address)).setText(value);
                }
                if(selectedUser.getThumbnailPhotoUrl()!=null){
                    new DownloadImageTask((ImageView) findViewById(R.id.image))
                            .execute(selectedUser.getThumbnailPhotoUrl());
                }

                PhoneCallListener phoneCallListener = new PhoneCallListener();
                TelephonyManager telephonyManager = (TelephonyManager)
                        DisplayContactsActivity.this.getSystemService(Context.TELEPHONY_SERVICE);
                telephonyManager.listen(phoneCallListener, PhoneStateListener.LISTEN_CALL_STATE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent callIntent = new Intent(Intent.ACTION_CALL);
                        String phoneNumber = userPhones.toString().substring(8, 22);
                        callIntent.setData(Uri.parse("tel:" + phoneNumber));
                        startActivity(callIntent);
                    }
                });

            }
        });
    }

    public void startUpdateActivity(View view) {
        String email = mCredential.getSelectedAccountName();
        Intent intent = new Intent(DisplayContactsActivity.this, UpdateActivity.class);
        for (User user : users) {
            if(email.equals(user.getPrimaryEmail().toString())) {
                // TODO: 12/11/15 get the address and phone number from the directory
                intent.putExtra("name", user.getName().getFullName());
                intent.putExtra("address", "adddddddddddrrrrrrrrrrrresssssssssssss");
                intent.putExtra("phone", "675467654546765432");
                intent.putExtra("email", email);
            }
        }
        startActivity(intent);
    }
}
