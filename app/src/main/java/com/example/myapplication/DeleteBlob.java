package com.example.myapplication;

import android.os.AsyncTask;
import android.util.Log;

import com.microsoft.azure.storage.blob.CloudBlob;

public class DeleteBlob extends AsyncTask<String, Integer, Integer> {

    private BlobStorageConnection blobStorageConnection;
    private String blobToDeleteName;
    private String blobType;
    BlobDeleted delegate = null;

    DeleteBlob(BlobStorageConnection blobStorageConnection){
        this.blobStorageConnection = blobStorageConnection;
    }

    @Override
    protected Integer doInBackground(String... params) {

        blobToDeleteName = params[0];
        blobType = params[1];

        try{

            if(blobType.equals("input")){
                CloudBlob blobToDelete = blobStorageConnection.inputContainer.getBlockBlobReference(blobToDeleteName);
                if(blobToDelete.deleteIfExists()){
                    Log.d("TAG","Successfully deleted blob.");
                    return 1;
                }
            } else if(blobType.equals("output")){
                CloudBlob blobToDelete = blobStorageConnection.outputContainer.getBlockBlobReference(blobToDeleteName);
                if(blobToDelete.deleteIfExists()){
                    Log.d("TAG","Successfully deleted blob.");
                    return 1;
                }
            } else {
                Log.d("TAG", "Couldn't figure out if container clicked was input or output");
                return 0;
            }

        } catch (Exception e){
            Log.e("TAG","Couldn't delete blob.");
            Log.e("TAG", e.toString());
            return  0;
        }
        return  1;
    }
    protected void onPostExecute(Integer result){

        if (result == 1) delegate.blobDeletedSuccess(true, blobToDeleteName, blobType);
        else delegate.blobDeletedSuccess(false, blobToDeleteName, blobType);

    }
}