package com.example.myapplication;

import android.os.AsyncTask;
import android.util.Log;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.ListBlobItem;

import java.util.ArrayList;

public class ListContainerBlobs extends AsyncTask<Void, Integer, Integer> {

    private ArrayList<String> inputBlobList = new ArrayList<>();
    private ArrayList<String> outputBlobList = new ArrayList<>();

    private BlobStorageConnection blobStorageConnection;
    private CloudBlobContainer inputContainer;
    private CloudBlobContainer outputContainer;

    AsyncResponse delegate = null;

    // Create constructor in order to allow to pass the context as a parameter
    ListContainerBlobs(BlobStorageConnection blobStorageConnection){
        this.blobStorageConnection = blobStorageConnection;

    }

    @Override
    protected Integer doInBackground(Void... params) {

        Log.d("TAG", "Starting with do in background");
        CloudBlobClient blobClient = null;
        try{
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(blobStorageConnection.storageConnectionString);
            blobClient = storageAccount.createCloudBlobClient();
        }catch (Exception e) {
            e.printStackTrace();
            Log.e("TAG", e.toString());
        }

        // Check if the defined input and output containers exist
        boolean inputContainerExists = false;
        boolean outputContainerExists = false;

        for(CloudBlobContainer c : blobClient.listContainers()){
            //Log.d("TAG", c.getName());
            String name = c.getName();
            if(name.equals(blobStorageConnection.inputContainerName)) inputContainerExists = true;
            if(name.equals(blobStorageConnection.outputContainerName)) outputContainerExists = true;
            if(inputContainerExists && outputContainerExists) break;
        }

        if(!inputContainerExists || !outputContainerExists){
            return 3;
        }
        try{
            this.inputContainer = blobClient.getContainerReference(blobStorageConnection.inputContainerName);
            this.outputContainer = blobClient.getContainerReference(blobStorageConnection.outputContainerName);
            Log.d("TAG", "Got the containers");
        }catch (Exception e) {
            e.printStackTrace();
            Log.e("TAG", e.toString());
        }
        // Loop over input container

        try {
            Log.d("TAG", "Before for loop");
            for (ListBlobItem blobItem : inputContainer.listBlobs()) {
                String blobUri = blobItem.getUri().toString();
                String blobName = blobUri.substring(blobUri.lastIndexOf("/") + 1);
                this.inputBlobList.add(blobName);
                //Log.d("TAG", blobName);
            }



        } catch (Exception e) {
            Log.d("TAG", "Couldn't list input blobs");
            Log.e("TAG", e.toString());
            // Output the stack trace.
            e.printStackTrace();
        }

        // loop over output blobs

        try {
            for (ListBlobItem blobItem : outputContainer.listBlobs()) {
                String blobUri = blobItem.getUri().toString();
                String blobName = blobUri.substring(blobUri.lastIndexOf("/") + 1);
                this.outputBlobList.add(blobName);
                //Log.d("TAG", blobName);
            }



        } catch (Exception e) {
            Log.d("TAG", "Couldn't list input blobs");
            // Output the stack trace.
            e.printStackTrace();
            Log.e("TAG", e.toString());
        }

        return 0;

    }

    @Override
    protected void onPostExecute(Integer result){

        if(result == 3){
            delegate.getBlobList(null, null);
            return;
        }

        delegate.getBlobList(inputBlobList, outputBlobList);
    }

}