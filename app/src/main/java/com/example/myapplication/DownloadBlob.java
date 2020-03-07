package com.example.myapplication;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class DownloadBlob extends AsyncTask<String, Integer, Integer> {

    private CloudBlobContainer container;
    ImageDownloaded delegate;

    String blobType;
    String selectedBlobName;

    DownloadBlob(BlobStorageConnection blobStorageConnection, String blobType){

        this.blobType = blobType;

        switch (this.blobType){
            case "input":
                container = blobStorageConnection.inputContainer;
                break;
            case "output":
                container = blobStorageConnection.outputContainer;
        }

    }

    @Override
    protected Integer doInBackground(String... params) {

        selectedBlobName = params[0];

        Log.d("TAG","Trying to download " + selectedBlobName);
        try {
            // Get blob
            CloudBlockBlob blob = container.getBlockBlobReference(selectedBlobName);

            // Create file to save the blob to
            File imageLocation = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), selectedBlobName);

            // Download
            Log.d("TAG", "to path: " + imageLocation);
            //OutputStream blobStream = new FileOutputStream(imageLocation);
            blob.downloadToFile(imageLocation.toString());


            return 1;

        }catch(Exception e){
            Log.e("TAG", "Error while downloading");
            Log.e("TAG", e.toString());
            e.printStackTrace();

            return 0;
        }

    }

    protected void onPostExecute(Integer result){

        if(result == 1){
            Log.d("TAG", "Downloaded");
            delegate.imageDownloaded(true, selectedBlobName, blobType);
        }

    }

}