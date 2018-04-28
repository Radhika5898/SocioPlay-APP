package in.co.socioplay.socioplay;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

import static android.Manifest.permission.READ_CONTACTS;

public class SignUp extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {


    private AutoCompleteTextView Email;
    private EditText password;
    private EditText confirm;
    private View ProgressView;
    private AutoCompleteTextView Username;
    private View SignUpFormView;
    private static final int REQUEST_READ_CONTACTS = 0;
    private UserSignupTask mAuthTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        Email = findViewById(R.id.Email);
        password = findViewById(R.id.Password);
        confirm = findViewById(R.id.re_Password);
        Username=findViewById(R.id.username);
        Button sign_up = findViewById(R.id.SignUp);
        ProgressView = findViewById(R.id.sign_up_progress);
        SignUpFormView = findViewById(R.id.sign_form);

        confirm.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptSignup();
                    return true;
                }
                return false;
            }
        });
        sign_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptSignup();
            }
        });


    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), SignUp.ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        List<String> emails = new ArrayList<>();
        data.moveToFirst();
        while (!data.isAfterLast()) {
            emails.add(data.getString(SignUp.ProfileQuery.ADDRESS));
            data.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(SignUp.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        Email.setAdapter(adapter);
    }

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
    }

    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(Email, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }


    public void attemptSignup() {

        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        Email.setError(null);
        password.setError(null);

        // Store values at the time of the login attempt.
        String email = Email.getText().toString();
        String username = Username.getText().toString();
        String pass1 = password.getText().toString();
        String pass2 = confirm.getText().toString();
        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(pass1) && !isPasswordValid(pass1)) {
            password.setError(getString(R.string.error_invalid_password));
            focusView = password;
            cancel = true;
        }
        if (!TextUtils.isEmpty(pass2) && !isPasswordValid(pass2)) {
            confirm.setError(getString(R.string.error_invalid_password));
            focusView = confirm;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            Email.setError(getString(R.string.error_field_required));
            focusView = Email;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            if (pass1.equals(pass2)) {
                // Show a progress spinner, and kick off a background task to
                // perform the user login attempt.
                showProgress(true);
                mAuthTask = new UserSignupTask(email, pass1, username);
                mAuthTask.execute((Void) null);
            } else
                confirm.setError("Passwords Doesn't Match");
        }

    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        SignUpFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        SignUpFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                SignUpFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        SignUpFormView.setVisibility(show ? View.VISIBLE : View.GONE);
        SignUpFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                SignUpFormView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }


    @SuppressLint("StaticFieldLeak")
    public class UserSignupTask extends AsyncTask<Void, Void, Boolean> {


        final String mEmail;
        final String mUsername;
        final String mPassword;
        final JSONObject log= new JSONObject();


        UserSignupTask(String email, String pass1 , String username)  {
            mEmail=email;
            mUsername = username;
            mPassword = pass1;

            try {
                log.put("username",mUsername);
                log.put("password",mPassword);
                log.put("emailid",email);
                if (log.length() > 0) {
                    doInBackground();
                }
            }catch (JSONException e){ e.printStackTrace();}

        }
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                // Simulate network access.
                Thread.sleep(2000);
                getResponse(log);
            } catch (InterruptedException e) {
                Log.d("Exception from :","doInBackground");
                e.printStackTrace(); }
            return null;
        }
        private void getResponse(JSONObject log) {
            JSONObject networkResp=new JSONObject();

            try {
                try {
                    URL url = new URL("http://59f9d951.ngrok.io/users/signup");
                    MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                    OkHttpClient client = new OkHttpClient();
                    okhttp3.RequestBody body = RequestBody.create(JSON, log.toString());
                    okhttp3.Request request = new okhttp3.Request.Builder()
                            .url(url)
                            .post(body)
                            .build();

                    okhttp3.Response response = client.newCall(request).execute();
                    String networkResp1 = response.body().string();
                    networkResp= new JSONObject(networkResp1);

                } catch (Exception ex)
                {
                    String err = String.format("{\"result\":\"false\",\"error\":\"%s\"}", ex.getMessage());
                    Log.d("Exception ex:",err);
                }
                String result = (networkResp.getString("statusCode"));
                String token = (networkResp.getString("result"));
                Log.d("Token is :",token);
                Log.d("Status Code is :",result);
                if (result != null) {
                    if(result.equals("200"))
                    {
                        onPostExecute();
                    }
                    else if(result.equals("451"))
                    {
                        Username.setError("Already Exists!");
                    }
                    else if(result.equals("452"))
                    {
                        Email.setError("Already Exists!");
                    }
                    else if(result.equals("450"))
                    {
                        Email.setError("Already Exists!");
                        Username.setError("Already Exists!");
                    }
                }
            } catch (Exception e) {
                Log.d("InputStream", e.getLocalizedMessage());
            }
        }
        private void onPostExecute() {
            mAuthTask = null;
            Intent intent=new Intent(SignUp.this,Home.class);
                showProgress(true);
                startActivity(intent);

        }


    }
}