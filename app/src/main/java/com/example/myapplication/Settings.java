package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.annotation.NonNull;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;

public class Settings extends AppCompatActivity {

    boolean credentialsAreShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setSelectedItemId(R.id.navigation_settings);

        // If credentials for blob storage have been set display them on the editText fields
        final SharedPreferences preferences = this.getSharedPreferences(getString(R.string.app_name),0);
        String inputContainerName = preferences.getString(getString(R.string.input_container),"");
        String outputContainerName = preferences.getString(getString(R.string.output_container),"");
        String connectionString = preferences.getString(getString(R.string.connection_string),"");
        String email = preferences.getString("email", "");

        EditText inputContainerNameEditText = findViewById(R.id.inputContainerNameEditText);
        EditText outputContainerNameEditText = findViewById(R.id.outputContainerNameEditText);
        EditText connectionStringText = findViewById(R.id.connectionStringEditText);
        final EditText emailEditText = findViewById(R.id.emailEditText);

        inputContainerNameEditText.setText(inputContainerName);
        outputContainerNameEditText.setText(outputContainerName);
        connectionStringText.setText(connectionString);
        emailEditText.setText(email);

        final InputMethodManager mgr = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        emailEditText.setImeOptions(IME_ACTION_DONE);
        emailEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event){
                // If the event is a key-down event on the "enter" button
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // Perform action on key press
                    setCredentials(view); // parse the coordinate
                    if (mgr != null) mgr.hideSoftInputFromWindow(emailEditText.getWindowToken(), 0);// make the keyboard disappear
                    return true;
                }
                return false;
            }
        });

        final Context context = this;
        BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener  = new BottomNavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Intent intent;
                switch (item.getItemId()) {
                    case R.id.navigation_home:
                        return true;
                    case R.id.navigation_blob_list:
                        // First check if credentials are set and work
                        try{
                            new BlobStorageConnection(preferences);
                        }catch (NullPointerException e){
                            Toast.makeText(context, "Please fill in your credentials in the settings page.", Toast.LENGTH_SHORT).show();
                            Log.e("TAG","Nothing there" );
                            return false;
                        }

                        // Then if they are go to BlobListViewActivity
                        intent = new Intent( Settings.this, BlobListView.class);
                        startActivity(intent);
                        return true;
                    case R.id.navigation_settings:
                        intent = new Intent(Settings.this, Settings.class);
                        startActivity(intent);
                        return true;
                }
                return false;
            }
        };
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    public void setCredentials(View view) {
        SharedPreferences prefs = Settings.this.getSharedPreferences(getString(R.string.app_name), 0);
        SharedPreferences.Editor editor = prefs.edit();

        EditText inputContainerNameEditText = findViewById(R.id.inputContainerNameEditText);
        String inputContainerNameString = inputContainerNameEditText.getText().toString();

        EditText outputContainerNameEditText = findViewById(R.id.outputContainerNameEditText);
        String outputContainerNameString = outputContainerNameEditText.getText().toString();

        EditText connectionString = findViewById(R.id.connectionStringEditText);
        String connectionStringString = connectionString.getText().toString();

        EditText destinationEmailEditText = findViewById(R.id.emailEditText);
        String email = destinationEmailEditText.getText().toString();

        editor.putString(getString(R.string.input_container), inputContainerNameString);
        editor.putString(getString(R.string.output_container), outputContainerNameString);
        editor.putString(getString(R.string.connection_string),connectionStringString);
        editor.putString("email",email);

        Log.d("TAG", "Input container name: " + inputContainerNameString);
        Log.d("TAG", "Output container name: " + outputContainerNameString);
        Log.d("TAG", "Connection String: " + connectionStringString);

        editor.apply();

        Toast.makeText(this, "Credentials saved.", Toast.LENGTH_SHORT).show();
    }

    public void clearCredentials(View view){
        final SharedPreferences prefs = Settings.this.getSharedPreferences(getString(R.string.app_name), 0);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Are you sure you want to clear the credentials?");
        builder.setPositiveButton("Clear", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(getString(R.string.input_container), "");
                editor.putString(getString(R.string.output_container), "");
                editor.putString(getString(R.string.connection_string), "");
                editor.apply();
                finish();
                startActivity(getIntent());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

            }
        });

        AlertDialog dialog = builder.create();

        dialog.show();

    }

    public void toggleDisplayCredentials(View view){

        Button button = findViewById(R.id.toggleCredentialDisplay);
        EditText inputContainerNameEditText = findViewById(R.id.inputContainerNameEditText);
        EditText outputContainerNameEditText = findViewById(R.id.outputContainerNameEditText);
        EditText connectionStringEditText = findViewById(R.id.connectionStringEditText);

        if (credentialsAreShowing){
            inputContainerNameEditText.setTransformationMethod(new PasswordTransformationMethod());
            outputContainerNameEditText.setTransformationMethod(new PasswordTransformationMethod());
            connectionStringEditText.setTransformationMethod(new PasswordTransformationMethod());
            button.setText("Display\nCredentials");
            credentialsAreShowing = false;
        }
        else{
            inputContainerNameEditText.setTransformationMethod(null);
            outputContainerNameEditText.setTransformationMethod(null);
            connectionStringEditText.setTransformationMethod(null);
            button.setText("Hide\nCredentials");
            credentialsAreShowing = true;
        }

    }
}
