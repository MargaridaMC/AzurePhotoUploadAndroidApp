package com.example.myapplication;

import android.os.AsyncTask;
import android.util.Log;

import com.microsoft.azure.storage.blob.ListBlobItem;

import java.util.ArrayList;

public class CheckForOutputImage extends AsyncTask<ArrayList<String>, Integer, Integer> {

    private BlobStorageConnection blobStorageConnection;
    OutboxCheck delegate = null;
    private ArrayList<String> requestedBlobNames;
    private ArrayList<Boolean> blobsExist;
    private ArrayList<String> existentBlobNames = new ArrayList<>();

    CheckForOutputImage(BlobStorageConnection blobStorageConnection){
        this.blobStorageConnection = blobStorageConnection;
    }

    @Override
    protected Integer doInBackground(ArrayList<String>... params) {

        requestedBlobNames = params[0];
        blobsExist = new ArrayList<>();

        Log.d("TAG", "Looking for output image");

        // Check if output is already in output container
        try{
            //CloudBlob blob = blobStorageConnection.outputContainer.getBlockBlobReference(blobName);
            int i = 0;
            for(String filename:requestedBlobNames) {
                for (ListBlobItem blobItem : blobStorageConnection.outputContainer.listBlobs()) {
                    String blobUri = blobItem.getUri().toString();
                    if (blobUri.contains(filename)) {
                        blobsExist.add(i, true);
                        existentBlobNames.add(i, blobUri.substring(blobUri.lastIndexOf("/") + 1));
                        Log.d("TAG", "Blob exists");
                        Log.d("TAG", "Output blob name is: " + existentBlobNames.get(i));
                        break;
                    }
                    //  blobsExist.add(i, false); // If you don't break
                }
                if(blobsExist.size() != i + 1) blobsExist.add(i, false);
                i ++;
            }
            //   return 0;

        } catch (Exception e){
            Log.e("TAG", e.toString());
        }

        return 0;
    }

    protected void onPostExecute(Integer result){

        boolean allFound = true;
        for(boolean found:blobsExist) allFound = allFound && found;

        if(!allFound){
            Log.d("TAG","Blob is still not there");
            delegate.checkOutbox(requestedBlobNames, allFound);
        } else {
            Log.d("TAG", "All blobs are there");
            delegate.checkOutbox(existentBlobNames, allFound);
        }

    }
}