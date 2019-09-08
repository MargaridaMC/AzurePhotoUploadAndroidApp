package com.example.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class BlobListView extends Activity implements MyRecyclerViewAdapter.ItemClickListener, AsyncResponse, BlobDeleted {

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Intent intent;
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    intent = new Intent(BlobListView.this, MainActivity.class);
                    startActivity(intent);
                    return true;
                case R.id.navigation_dashboard:
                    return true;
                case R.id.navigation_notifications:
                    intent = new Intent(BlobListView.this, Settings.class);
                    startActivity(intent);
                    return true;
            }
            return false;
        }
    };

    static  BlobStorageConnection blobStorageConnection;
    ArrayList<String> inputBlobList;
    ArrayList<String> outputBlobList;
    SharedPreferences preferences;

    private SwipeRefreshLayout swipeContainer;
    final BlobListView thisClass = this;
    int numberInputBlobs = 0;
    String TAG = "TAG";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blob_list_view);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setSelectedItemId(R.id.navigation_dashboard);
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        preferences = this.getSharedPreferences(getString(R.string.app_name), 0);
        blobStorageConnection = new BlobStorageConnection(preferences);
        checkCredentials();

        swipeContainer = findViewById(R.id.swipeContainer);

        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(onLayoutRefresh);

        Log.d("TAG", "Listing container.");

        // Check if there is a list of blobs in shared preferences
        SharedPreferences prefs = this.getSharedPreferences(getString(R.string.app_name), 0);
        String inputBlobListSerialized = prefs.getString(getString(R.string.inputBlobList), "");
        String outputBlobListSerialized = prefs.getString(getString(R.string.outputBlobList), "");
        boolean blobListsFilled = false;

        if(inputBlobListSerialized != null && outputBlobListSerialized!=null){
            blobListsFilled = inputBlobListSerialized.isEmpty() || outputBlobListSerialized.isEmpty();
        }

        if(blobListsFilled){
            ListContainerBlobs asyncTask = new ListContainerBlobs(blobStorageConnection);
            asyncTask.delegate = this;
            asyncTask.execute();
            //fillBlobList(inputBlobList, outputBlobList);
        } else {
            // Fill in view with the values from shared preferences
            Type type = new TypeToken<ArrayList<String>>() {}.getType();
            Gson gson = new Gson();

            inputBlobList = gson.fromJson(inputBlobListSerialized, type);
            outputBlobList = gson.fromJson(outputBlobListSerialized, type);

            fillBlobList(inputBlobList, outputBlobList);

        }


    }

    SwipeRefreshLayout.OnRefreshListener onLayoutRefresh = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            Log.d("TAG", "Refreshing");
            ListContainerBlobs asyncTask = new ListContainerBlobs(blobStorageConnection);
            asyncTask.delegate = thisClass;
            asyncTask.execute();
            fillBlobList(inputBlobList, outputBlobList);
            swipeContainer.setRefreshing(false);
        }};

    @Override
    public void getBlobList(ArrayList<String> inputBlobList, ArrayList<String> outputBlobList){

        if(inputBlobList == null || outputBlobList == null){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage("Are you sure the your inputted the names of your containers correctly? They weren't found. Please check your blob storage information in the settings page.");
            builder.setPositiveButton("Go to Settings", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Intent intent = new Intent(thisClass, Settings.class);
                    thisClass.startActivity(intent);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                }
            });

            AlertDialog dialog = builder.create();

            dialog.show();
            return;
        }

        this.inputBlobList = inputBlobList;
        this.outputBlobList = outputBlobList;

        // Save blob list in shared preferences
        SharedPreferences prefs = this.getSharedPreferences(getString(R.string.app_name), 0);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String inputBlobJson = gson.toJson(inputBlobList);
        String outputBlobJson = gson.toJson(outputBlobList);

        Log.d("TAG", "Got the blobs");

        editor.putString(getString(R.string.inputBlobList), inputBlobJson);
        editor.putString(getString(R.string.outputBlobList), outputBlobJson);
        editor.apply();

        fillBlobList(inputBlobList, outputBlobList);
    }

    public void fillBlobList(ArrayList<String> inputBlobList, ArrayList<String> outputBlobList){

        RecyclerView recyclerView = findViewById(R.id.blobList);

        numberInputBlobs = inputBlobList.size();

        ArrayList<String> allBlobs = new ArrayList<>();
        allBlobs.add("Input Container Blobs:");
        allBlobs.addAll(inputBlobList);
        allBlobs.add("Output Container Blobs:");
        allBlobs.addAll(outputBlobList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        MyRecyclerViewAdapter recyclerViewAdapter = new MyRecyclerViewAdapter(this, allBlobs, numberInputBlobs);
        recyclerViewAdapter.setClickListener(this);
        recyclerView.setAdapter(recyclerViewAdapter);

    }

    private void checkCredentials(){

        Log.d(TAG, "Checking credentials");

        if(blobStorageConnection.inputContainerName.isEmpty() || blobStorageConnection.outputContainerName.isEmpty() || blobStorageConnection.storageConnectionString.isEmpty()){

            Log.d(TAG, "They are empty");
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage("Please fill in your blob storage information in the settings page.");
            builder.setPositiveButton("Go to Settings", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Intent intent = new Intent(BlobListView.this, Settings.class);
                    startActivity(intent);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });

            AlertDialog dialog = builder.create();

            dialog.show();


        }
    }

    @Override
    public void onItemClick(View view, int position) {

        if(position == 0 || position == numberInputBlobs + 1) return;

        final String selectedBlobName;
        final Context context = this.getApplicationContext();
        final String blobType;

        if(position <= numberInputBlobs + 1){
            blobType = "input";
            selectedBlobName = this.inputBlobList.get(position - 1);
           // Toast.makeText(this, "You clicked " + blobType + " on row number " +  Integer.toString(position - 1), Toast.LENGTH_SHORT).show();
        } else {
            blobType = "output";
            selectedBlobName = this.outputBlobList.get(position - numberInputBlobs - 2);
           // Toast.makeText(this, "You clicked " + blobType + " on row number " + Integer.toString(position - numberInputBlobs - 2), Toast.LENGTH_SHORT).show();
        }


        // Assign popup menu to the list element
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.blob_list_popup_menu,popupMenu.getMenu());
        popupMenu.setGravity(Gravity.END);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch(menuItem.getItemId()){
                    // Handle the non group menu items here
                    case R.id.view:
                        viewImage(selectedBlobName, blobType);
                        return true;
                    case R.id.attach_to_email:
                        ArrayList<String> attachment = new ArrayList<>();
                        attachment.add(selectedBlobName);
                        attachImageToEmail(context, attachment, blobType);
                        return true;
                    case R.id.delete:
                        // Set the text color to blue
                        DeleteBlob deleteBlob = new DeleteBlob(blobStorageConnection);
                        deleteBlob.delegate = thisClass;
                        deleteBlob.execute(selectedBlobName, blobType);

                        ListContainerBlobs listBlobs = new ListContainerBlobs(blobStorageConnection);
                        listBlobs.delegate = thisClass;
                        listBlobs.execute();
                        return true;
                    default:
                        return false;
                }
            }
        });

        // Finally, show the popup menu
        popupMenu.show();

    }

    void viewImage(String filename, String blobType){

        Log.d("TAG", "Opening image for view");
        File imageLocation = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
        Uri path = FileProvider.getUriForFile(this,  getPackageName() + ".provider", imageLocation);
        boolean fileExists = imageLocation.exists();

        // Check if file exists and if not, download it
        if(!fileExists){
            Log.d("TAG", "File hasn't been downloaded. Trying to download " + filename);
            DownloadBlob downloadBlob = new DownloadBlob(blobStorageConnection, blobType);
            downloadBlob.execute(filename);
            viewImage(filename, blobType);
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(path, "image/jpeg");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    public static void attachImageToEmail(Context context, ArrayList<String> filenames, String blobType){

        // blobType should be "input" or "output"

        // If the blob list view hasn't been open yet, this variable hasn't been set
        SharedPreferences preferences = context.getSharedPreferences(context.getResources().getString(R.string.app_name), 0);
        if (blobStorageConnection == null) blobStorageConnection = new BlobStorageConnection(preferences);

        SharedPreferences prefs = context.getSharedPreferences(context.getResources().getString(R.string.app_name), 0);
        ArrayList<Uri> uris = new ArrayList<>();

        if(filenames == null){
            // Use list from shared preferences
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<String>>() {}.getType();
            filenames = gson.fromJson(prefs.getString("processedImages",""), type);
        }

        if(filenames != null){

        for(String filename: filenames) {

            File imageLocation = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
            boolean fileExists = imageLocation.exists();

            // Check if file exists and if not, download it
            if (!fileExists) {
                Log.d("TAG", "File hasn't been downloaded. Trying to download " + filename);
                DownloadBlob downloadBlob = new DownloadBlob(blobStorageConnection, blobType);
                downloadBlob.execute(filename);
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (Exception e){
                    e.printStackTrace();
                }
                attachImageToEmail(context, filenames, blobType);
            }

            Uri path = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", imageLocation);
            uris.add(path);
        }
        }
        else{
            Toast.makeText(context, "Couldn't find files.", Toast.LENGTH_SHORT).show();

        }

        String email = prefs.getString("email", "");

        Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);

        emailIntent.setType("vnd.android.cursor.dir/email");
        String[] to = {email};
        emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
      //  emailIntent.putExtra(Intent.EXTRA_STREAM, path);
        if(filenames != null) emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Facturas " + filenames.toString());
        emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
          //  context.startActivity(Intent.createChooser(emailIntent, "Send email..."));
            context.startActivity(emailIntent);

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("processedImages", "");
            editor.apply();



        } catch (Exception e){
            e.printStackTrace();
            Log.e("TAG",e.toString());
        }

    }

    @Override
    public void blobDeletedSuccess(boolean success, String blobToDeleteName, String blobType){
        if(success) Toast.makeText(this, "Successfully deleted " + blobToDeleteName + " from " + blobType + " folder.", Toast.LENGTH_LONG).show();
        else Toast.makeText(this, "Couldn't delete " + blobToDeleteName + " from " + blobType + " folder.", Toast.LENGTH_LONG).show();
    }
}

