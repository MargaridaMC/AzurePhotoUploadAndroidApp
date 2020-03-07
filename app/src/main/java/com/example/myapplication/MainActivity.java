package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.FileProvider;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;


import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements OutboxCheck, UploadedToStorage, StorageConnectionSet {

    static final int GALLERY_REQUEST_CODE = 1;
    static final int CAMERA_REQUEST_CODE = 2;
    static final int CAMERA_METHOD_PERMISSIONS_CODE = 3;
    String[] CAMERA_METHOD_PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private String cameraFilePath;
    BlobStorageConnection blobStorageConnection;
    ArrayList<String> imagesToProcess = new ArrayList<>();
    String filename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setSelectedItemId(R.id.navigation_home);

        createNotificationChannel();

        final SharedPreferences preferences = this.getSharedPreferences(getString(R.string.app_name), 0);

        try{
            blobStorageConnection = new BlobStorageConnection(preferences);
            blobStorageConnection.delegate = this;
        }catch (NullPointerException e){
            Toast.makeText(this, "Please fill in your credentials in the settings page.", Toast.LENGTH_SHORT).show();
            Log.e("TAG","Nothing there" );
        }

        final Context context = this;
        BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener  = item -> {
            Intent intent;
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    return true;
                case R.id.navigation_blob_list:
                    // First check if credentials are set
                    try{
                        blobStorageConnection = new BlobStorageConnection(preferences);
                    }catch (NullPointerException e){
                        Toast.makeText(context, "Please fill in your credentials in the settings page.", Toast.LENGTH_SHORT).show();
                        Log.e("TAG","Nothing there" );
                        return false;
                    }

                    // Then if they are go to BlobListViewActivity
                    intent = new Intent(MainActivity.this, BlobListView.class);
                    startActivity(intent);
                    return true;
                case R.id.navigation_settings:
                    intent = new Intent(MainActivity.this, Settings.class);
                    startActivity(intent);
                    return true;
            }
            return false;
        };


        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        Log.d("TAG", "Done with onCreate");
    }


    static final String TAG = "TAG";
    public static boolean permissionsRevoked(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        switch(requestCode){
            case 1:
                Log.d(TAG,"External storage2");
                if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    Log.v(TAG,"Permission:"+ permissions[0]+"was"+grantResults[0]);
                }
                break;

            case CAMERA_METHOD_PERMISSIONS_CODE:
                Log.d(TAG,"Camera");
                if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    Log.v(TAG,"Permission:"+permissions[0]+"was"+grantResults[0]);
                }
                break;
        }
    }

    private boolean checkCredentials(){

        Log.d(TAG, "Checking credentials");

        if(blobStorageConnection.inputContainerName.isEmpty() || blobStorageConnection.outputContainerName.isEmpty() || blobStorageConnection.storageConnectionString.isEmpty()){

            Log.d(TAG, "They are empty");
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage("Please fill in your blob storage information in the settings page.");
            builder.setPositiveButton("Go to Settings", (dialog, id) -> {
                Intent intent = new Intent(MainActivity.this, Settings.class);
                startActivity(intent);
            });
            builder.setNegativeButton("Cancel", (dialog, id) -> {

            });

            AlertDialog dialog = builder.create();

            dialog.show();

            return false;

        }
        else return true;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        //This is the directory in which the file will be created. This is the default location of Camera photos
        File storageDir = new File(Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_DCIM), "Camera");
        File image = File.createTempFile(imageFileName,".jpg", storageDir);
        // Save a file: path for using again
        cameraFilePath = "file://" + image.getAbsolutePath();
        return image;
    }

    public void pickFromGallery(View view){

        // Check if the credentials needed to access the blob storage are set
        boolean credentialsSet = checkCredentials();
        if (!credentialsSet) return;
        else Log.d(TAG, "Credentials set");

        // Check if the app has read permissions to read from gallery
        if(permissionsRevoked(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, GALLERY_REQUEST_CODE);
            return;
        }

        //Create an Intent with action as ACTION_PICK
        Intent intent=new Intent(Intent.ACTION_PICK);
        // Sets the type as image/*. This ensures only components of type image are selected
        intent.setType("image/*");
        //We pass an extra array with the accepted mime types. This will ensure only components with these MIME types as targeted.
        String[] mimeTypes = {"image/jpeg"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES,mimeTypes);
        // Launching the Intent
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
        Log.d("TAG", "Got the photo");
    }

    public void captureFromCamera(View view) {
/*
         // For Testing purposes
        CheckForOutputImage check = new CheckForOutputImage(blobStorageConnection);
        check.delegate = this;
        ArrayList<String> toCheck = new ArrayList<>();
        toCheck.add("out-metro.jpg");
        toCheck.add("out-hotel.jpg");
        check.execute(toCheck);

*/

        // Check if the credentials needed to access the blob storage are set
        boolean credentialsSet = checkCredentials();
        if (!credentialsSet) return;
        else Log.d(TAG, "Credentials set");


        if(permissionsRevoked(this, CAMERA_METHOD_PERMISSIONS)){
            Log.d(TAG, "Camera permission has not been granted");
            ActivityCompat.requestPermissions(this, CAMERA_METHOD_PERMISSIONS, CAMERA_METHOD_PERMISSIONS_CODE);
        }

        Log.d(TAG, "Getting camera");
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri path =  FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", createImageFile());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, path);
            startActivityForResult(intent, CAMERA_REQUEST_CODE);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode, Intent data){
        Log.d("TAG", "Response");
        super.onActivityResult(requestCode, resultCode, data);

        // Request input filename
        requestFilename( requestCode, resultCode, data);

    }

    InputFilter filter = (source, start, end, dest, dstart, dend) -> {
        for (int i = start; i < end; i++) {
            if (!(Character.isLetterOrDigit(source.charAt(i)) || source.charAt(i) == '_')) {
                return "";
            }
        }
        return null;
    };

    public void requestFilename(final int requestCode, final int resultCode, final Intent data){

        SimpleDateFormat dateFormat;
        dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getDefault());
        Date date = new Date();
        final String now = dateFormat.format(date) + ".jpg";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set filename");

        // Set up the input
        final EditText input = new EditText(this);
        input.setText(now);
        input.setFilters(new InputFilter[]{filter});
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", (dialog, which) -> {
            filename = input.getText().toString();
            continueOnActivityResult( requestCode, resultCode, data);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    public void continueOnActivityResult(int requestCode,int resultCode, Intent data){

        UploadToBlobTask blobTask;
        Uri selectedImage;

        Toast.makeText(this, "Trying to upload image.", Toast.LENGTH_LONG).show();
        // Result code is RESULT_OK only if the user selects an Image
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode){
                case GALLERY_REQUEST_CODE:
                    //data.getData return the content URI for the selected Image
                    selectedImage = data.getData();

                    String[] filePathColumn = { MediaStore.Images.Media.DATA };
                    // Get the cursor
                    Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    // Move to first row
                    cursor.moveToFirst();
                    //Get the column index of MediaStore.Images.Media.DATA
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    //Gets the String value in the column
                    String imgDecodableString = cursor.getString(columnIndex);
                    cursor.close();

                    Log.d(TAG, imgDecodableString);
                    blobTask = new UploadToBlobTask(blobStorageConnection);
                    blobTask.delegate = this;
                    blobTask.execute(imgDecodableString, filename);
                    break;

                case CAMERA_REQUEST_CODE:
                    selectedImage = Uri.parse(cameraFilePath);
                    String imgPath = selectedImage.toString();
                    imgPath = imgPath.replaceAll("file:", "");
                    blobTask = new UploadToBlobTask(blobStorageConnection);
                    blobTask.delegate = this;
                    blobTask.execute(imgPath, filename);
                    break;
            }
        }

    }

    @Override
    public void uploadedToStorage(final String filename, boolean success){
        if(success){
            // File was successfully uploaded
            Toast.makeText(this, "Successfully uploaded " + filename + " to Blob Storage!", Toast.LENGTH_LONG).show();
            imagesToProcess.add(filename);
            // Wait for output file
            final CheckForOutputImage check = new CheckForOutputImage(blobStorageConnection);
            check.delegate = this;

            Timer timer = new Timer();
            TimerTask tt = new TimerTask() {
                @Override
                public void run() {

                    check.execute(imagesToProcess);
                }
            };

            // Check in one minute
            Log.d("TAG", "Checking for output file");
            timer.schedule(tt, 60*1000);

        } else {
            Toast.makeText(this, "Couldn't upload " + filename + " to Blob Storage.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void checkOutbox(ArrayList filenames, boolean fileIsThere){

        if(filenames.size() == 0) return;

        if(fileIsThere){
            Log.d("TAG", "Trying to notify about file " + filenames.toString());

            // Save info
            SharedPreferences prefs = this.getSharedPreferences(getString(R.string.app_name), 0);
            SharedPreferences.Editor editor = prefs.edit();
            Gson gson = new Gson();
            editor.putString("processedImages", gson.toJson(filenames));
            editor.apply();

            String title = "Image Processing Done";
            String body = "Azure function is done processing " + filenames.toString() + ". Do you want to send the output images by email?";

            String NOTIFICATION_CHANNEL_ID = getString(R.string.channel_name);

            Intent intentAction = new Intent(this, DownloadNotificationSelectionReceiver.class);
            PendingIntent SendPendingIntent = PendingIntent.getBroadcast(this,1,intentAction,PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
            builder.setContentTitle(title);
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(body));
            builder.setSmallIcon(R.drawable.cloud_done);
            builder.setContentIntent(SendPendingIntent).setAutoCancel(true);

            Notification notification = builder.build();

           int NOTIFICATION_ID = 101;
            //This is what will will issue the notification i.e.notification will be visible
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
            notificationManagerCompat.notify(NOTIFICATION_ID, notification);

            imagesToProcess.clear();
        } else {
            // File is still not there. Repeat check again in one minute
            Log.d("TAG", "File is still not there. Checking again in one minute");
            final CheckForOutputImage check = new CheckForOutputImage(blobStorageConnection);
            check.delegate = this;

            Timer timer = new Timer();
            TimerTask tt = new TimerTask() {
                @Override
                public void run() {
                    check.execute(imagesToProcess);
                }
            };

            // Check in one minute
            Log.d("TAG", "Checking for output file");
            timer.schedule(tt, 60*1000);

        }

    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(getString(R.string.channel_name), name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.setLightColor(Color.GREEN);
            channel.setVibrationPattern(new long[] {
                    500,
                    500,
                    500,
                    500,
                    500
            });
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void storageConnectionSet(boolean success){
        if(!success)  Toast.makeText(this, "Please fill in credentials", Toast.LENGTH_SHORT).show();

    }

}


