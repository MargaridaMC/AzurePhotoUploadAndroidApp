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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class BlobListView extends Activity implements MyRecyclerViewAdapter.ItemClickListener, AsyncResponse {

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

    private SwipeRefreshLayout swipeContainer;
    final BlobListView thisClass = this;
    String TAG = "TAG";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blob_list_view);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setSelectedItemId(R.id.navigation_dashboard);
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        blobStorageConnection = new BlobStorageConnection(this);
        checkCredentials();

        swipeContainer = findViewById(R.id.swipecontainer);

        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(onLayoutRefresh);

        Log.d("TAG", "Listing container.");

        // Check if there is a list of blobs in shared preferences
        SharedPreferences prefs = this.getSharedPreferences(getString(R.string.app_name), 0);
        String inputBlobListSerialized = prefs.getString(getString(R.string.inputBlobList), "");
        String outputBlobListSerialized = prefs.getString(getString(R.string.outputBlobList), "");

        if(inputBlobListSerialized.isEmpty() || outputBlobListSerialized.isEmpty()){
            ListContainerBlobs asyncTask = new ListContainerBlobs(this, blobStorageConnection);
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
            ListContainerBlobs asyncTask = new ListContainerBlobs(thisClass, blobStorageConnection);
            asyncTask.delegate = thisClass;
            asyncTask.execute();
            fillBlobList(inputBlobList, outputBlobList);
            swipeContainer.setRefreshing(false);
        }};

    @Override
    public void getBlobList(ArrayList<String> inputBlobList, ArrayList<String> outputBlobList){
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

        MyRecyclerViewAdapter inputAdapter;
        MyRecyclerViewAdapter outputAdapter;

        RecyclerView inputRecyclerView = findViewById(R.id.inputContainerBlobs);
        RecyclerView outputRecyclerView = findViewById(R.id.outputContainerBlobs);

        // List input blobs
        inputRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        inputAdapter = new MyRecyclerViewAdapter(this, inputBlobList);
        inputAdapter.setClickListener(this);
        inputRecyclerView.setAdapter(inputAdapter);

        // List output blobs
        outputRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        outputAdapter = new MyRecyclerViewAdapter(this, outputBlobList);
        outputAdapter.setClickListener(this);
        outputRecyclerView.setAdapter(outputAdapter);

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

        final String selectedBlobName;
        final Context context = this.getApplicationContext();
        final String blobType;
        View parentView = (View) view.getParent();
        final String name = parentView.getTag().toString();
        // Toast.makeText(this, "You clicked " + name + " on row number " + position, Toast.LENGTH_SHORT).show();

        if(name.equals("inputContainerBlobs")){
            selectedBlobName = this.inputBlobList.get(position);
            blobType = "input";
        } else if(name.equals("outputContainerBlobs")){
            selectedBlobName = this.outputBlobList.get(position);
            blobType = "output";
        } else {
            Log.d("TAG", "Couldn't determine the origin of the click");
            return;
        }


        // Assign popup menu to the list element
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.blob_list_popup_menu,popupMenu.getMenu());
        popupMenu.setGravity(Gravity.RIGHT);

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
                        //Toast.makeText(context, "You clicked delete", Toast.LENGTH_SHORT).show();
                        DeleteBlob deleteBlob = new DeleteBlob(blobStorageConnection, context);
                        deleteBlob.execute(selectedBlobName, blobType);

                        ListContainerBlobs listBlobs = new ListContainerBlobs(context, blobStorageConnection);
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

        Log.d("TAG", "Opeing image for view");
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
        intent.setDataAndType(path, "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    public static void attachImageToEmail(Context context, ArrayList<String> filenames, String blobType){

        // blobType should be "input" or "output"

        // If the blob list view hasn't been open yet, this variable hasn't been set
        if (blobStorageConnection == null) blobStorageConnection = new BlobStorageConnection(context);

        SharedPreferences prefs = context.getSharedPreferences(context.getResources().getString(R.string.app_name), 0);
        ArrayList<Uri> uris = new ArrayList<>();

        if(filenames == null){
            // Use list from shared preferences
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<String>>() {}.getType();
            filenames = gson.fromJson(prefs.getString("processedImages",""), type);
        }

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

        String email = prefs.getString("email", "");

        Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);

        emailIntent.setType("vnd.android.cursor.dir/email");
        String[] to = {email};
        emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
      //  emailIntent.putExtra(Intent.EXTRA_STREAM, path);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Facturas " + filenames.toString());
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
}

