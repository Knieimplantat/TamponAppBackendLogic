package hwr.stud.tamponapp;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import hwr.stud.mylibrary.HttpDigestAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText username;
    private EditText password;
    private Button signIn;
    private TextView loggedInStatus;

   /* private final String usernameString = null;
    private final String passwordString = null;
    private final String loginURLString = null;*/

    private Boolean loggedIn = false;

    private Intent intentStats;
    private Intent intentLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login2);

        intentStats = new Intent(this, StatsActivity.class);
        intentLogin = new Intent(this, LoginActivity.class);

        username = (EditText) findViewById(R.id.username);
        password = (EditText) findViewById(R.id.password);
        signIn = (Button) findViewById(R.id.signIn);
        loggedInStatus = (TextView) findViewById(R.id.loggedInStatus);

        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Read username and password from form
                final String usernameString = username.getText().toString();
                final String passwordString = password.getText().toString();

                // Create loginURLString with params
                final String loginURLString = "http://192.168.178.54:8080/login"; //?un=" + usernameString + "&pw=" + passwordString;

                // talk to REST Service, done in separate worker thread
                // to be changed to Https
                loginPOSTRequest(loginURLString, usernameString, passwordString);
            }

            private void loginPOSTRequest(
                    final String loginURLString,
                    final String usernameString,
                    final String passwordString) {

                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {


                        HttpURLConnection loginConnection = establishHttpConnection(
                                loginURLString,
                                usernameString,
                                passwordString
                        );

                        // set methode to POST
                        try {
                            loginConnection.setRequestMethod("POST");
                            loginConnection.setDoOutput(true);
                            loginConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                            loginConnection.setRequestProperty("Accept", "application/json");
                            loginConnection.setChunkedStreamingMode(0);
                        } catch (ProtocolException e) {
                            e.printStackTrace();
                        }

                        // construct request body
                        JSONObject loginJSON = new JSONObject();
                        try {
                            loginJSON.put("un", usernameString);
                            loginJSON.put("pw", passwordString);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        // write requestbody
                        try {
                            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
                                    loginConnection.getOutputStream());
                            try {
                                outputStreamWriter.write(loginJSON.toString());
                                outputStreamWriter.flush(); // Streams IMMER flushen!!
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            // test request
                            Log.i("[loginJSON]", loginJSON.toString());

                            if(isLoginSuccess(loginConnection)) {startActivity(intentStats);}
                            else {startActivity(intentLogin);}


                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        loginConnection.disconnect();
                    }
                });

            }

            @Nullable
            HttpURLConnection establishHttpConnection(
                    String loginURLString,
                    String usernameString,
                    String passwordString
            ) {
                URL loginURL = null;
                try {
                    loginURL = new URL(loginURLString);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    return null;
                }
                try {
                   /* HttpURLConnection loginConnection =
                            new HttpDigestAuth().tryAuth(
                                    (HttpURLConnection) loginURL.openConnection(),
                                    usernameString,
                                    passwordString);*/

                    HttpURLConnection loginConnection = (HttpURLConnection) loginURL.openConnection();

                    return loginConnection;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            Boolean isLoginSuccess(HttpURLConnection loginConnection) {

                Boolean isLoggedIn = false;

                JsonReader jsonReader = null;
                try {
                    if (loginConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        InputStream responseBody = loginConnection.getInputStream();
                        InputStreamReader responseBodyReader = new InputStreamReader(responseBody, "UTF-8");
                        jsonReader = new JsonReader(responseBodyReader);
                    }
                    jsonReader.beginObject();
                    while (jsonReader.hasNext()) {
                        String key = jsonReader.nextName();
                        if (key.equals("success")) {
                            if (jsonReader.nextString().equals("true")) {
                                isLoggedIn = true;
                                // startActivity(intentStats);
                                break;
                            } else {
                                isLoggedIn = false;
                            }
                        } else {
                            jsonReader.skipValue();
                        }
                    }
                    jsonReader.endObject();
                    Log.i("[jsonReader]", jsonReader.toString());
                    jsonReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    isLoggedIn = false;
                }
                return isLoggedIn;
            }
        });


    }
}
