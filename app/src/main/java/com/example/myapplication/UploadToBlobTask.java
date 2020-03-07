package com.example.myapplication;

import android.os.AsyncTask;
import android.util.Log;

import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.io.File;
import java.io.FileInputStream;

public class UploadToBlobTask extends AsyncTask<String, Integer, Integer> {

    private BlobStorageConnection blobStorageConnection;
    private String inputFilename;
    UploadedToStorage delegate = null;

    UploadToBlobTask(BlobStorageConnection blobStorageConnection){
        this.blobStorageConnection = blobStorageConnection;
    }

    protected Integer doInBackground(String... imgPaths) {

        Log.d("TAG", "Trying to send photo to Azure");

        try
        {
            String imgPath = imgPaths[0];

            CloudBlobContainer container = blobStorageConnection.inputContainer;

            // Create or overwrite the blob (with the name "example.jpeg") with contents from a local file.
            inputFilename =  imgPaths[1];
            CloudBlockBlob blob = container.getBlockBlobReference(inputFilename);

            File source = new File(imgPath);
            blob.upload(new FileInputStream(source), source.length());
            Log.d("TAG", "Sent it");

            // Set content type
            BlobProperties properties = blob.getProperties();
            properties.setContentType("image/jpeg");
            blob.uploadProperties();
            return 1;
        }
        catch (Exception e)
        {
            Log.d("TAG", "Couldn't send the photo");
            // Output the stack trace.
            e.printStackTrace();
            return 0;
        }
    }

    protected void onPostExecute(Integer Result){
        //Toast.makeText(UploadToBlobTask.this, "Photo successfully uploaded!", Toast.LENGTH_LONG).show();
        Log.d("TAG", Result.toString());

        if(Result == 1) delegate.uploadedToStorage(inputFilename, true);
        else delegate.uploadedToStorage(inputFilename, false);

    }

}