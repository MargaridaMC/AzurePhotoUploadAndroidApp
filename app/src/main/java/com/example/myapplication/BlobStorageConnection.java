package com.example.myapplication;

import android.content.SharedPreferences;
import android.util.Log;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;

class BlobStorageConnection {

    String inputContainerName;
    String outputContainerName;
    String storageConnectionString;
    CloudBlobContainer inputContainer;
    CloudBlobContainer outputContainer;
    StorageConnectionSet delegate;

    BlobStorageConnection(SharedPreferences preferences){


        Log.d("TAG", "Getting credentials");
        inputContainerName = getCredential(preferences, "inputContainerName");
        outputContainerName = getCredential(preferences, "outputContainerName");
        storageConnectionString =  getCredential(preferences, "connectionString");

        blobStorageConnection(inputContainerName,outputContainerName, storageConnectionString);

    }

    private void blobStorageConnection(String inputContainerName, String outputContainerName, String storageConnectionString){

        try{
            Log.d("TAG", "Getting credentials");
            this.inputContainerName = inputContainerName;
            this.outputContainerName = outputContainerName;
            this.storageConnectionString = storageConnectionString;
            Log.d("TAG", "Got them");
            Log.d("TAG", "input: " + inputContainerName);
            Log.d("TAG", "output: " + outputContainerName);
            Log.d("TAG", "Connection String: " + storageConnectionString);

            CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            inputContainer = blobClient.getContainerReference(inputContainerName);
            outputContainer = blobClient.getContainerReference(outputContainerName);

        }catch (Exception e){

            delegate.storageConnectionSet(false);
            Log.e("TAG",e.toString());
        }

    }

    private String getCredential(SharedPreferences prefs, String credential) {
        return prefs.getString(credential, "");

    }

}





