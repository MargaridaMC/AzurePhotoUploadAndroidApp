package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
class BlobStorageConnection {

    String inputContainerName = "";
    String outputContainerName = "";
    String storageConnectionString = "";
    CloudBlobContainer inputContainer;
    CloudBlobContainer outputContainer;
    private Context context;

    BlobStorageConnection(Context context){

        this.context = context.getApplicationContext();
        Log.d("TAG", "Got the context");
        try{
            Log.d("TAG", "Getting credentials");
            this.inputContainerName = getCredential(this.context.getString(R.string.input_container));
            this.outputContainerName = getCredential(this.context.getString(R.string.output_container));
            this.storageConnectionString =  getCredential(this.context.getString(R.string.connection_string));
            Log.d("TAG", "Got them");
            Log.d("TAG", "input: " + inputContainerName);
            Log.d("TAG", "output: " + outputContainerName);
            Log.d("TAG", "Connection String: " + storageConnectionString);

            CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            inputContainer = blobClient.getContainerReference(inputContainerName);
            outputContainer = blobClient.getContainerReference(outputContainerName);

        }catch (Exception e){
            Toast.makeText(this.context, "Please fill in credentials", Toast.LENGTH_SHORT).show();
            Log.e("TAG",e.toString());
        }

    }

    private String getCredential(String credential) {
        SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.app_name), 0);
        return prefs.getString(credential, "");

    }

}

class ListContainerBlobs extends AsyncTask<Void, Integer, Integer>{


    private ArrayList<String> inputBlobList = new ArrayList<>();
    private ArrayList<String> outputBlobList = new ArrayList<>();

    private BlobStorageConnection blobStorageConnection;
    private CloudBlobContainer inputContainer;
    private CloudBlobContainer outputContainer;
    private Context context;

    AsyncResponse delegate = null;

    // Create constructor in order to allow to pass the context as a parameter
    ListContainerBlobs(Context context, BlobStorageConnection blobStorageConnection){
        this.blobStorageConnection = blobStorageConnection;
        this.context = context;

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
                Log.d("TAG", blobName);
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
                Log.d("TAG", blobName);
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
        // set up the RecyclerView
        Log.d("TAG", "in post");

        if(result == 3){
            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            builder.setMessage("Are you sure the your inputted the names of your containers correctly? They weren't found. Please check your blob storage information in the settings page.");
            builder.setPositiveButton("Go to Settings", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Intent intent = new Intent(context, Settings.class);
                    context.startActivity(intent);
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

        delegate.getBlobList(inputBlobList, outputBlobList);
    }




}

class UploadToBlobTask extends AsyncTask<String, Integer, Integer> {

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

class DownloadBlob extends AsyncTask<String, Integer, Integer>{

    private CloudBlobContainer container;

    DownloadBlob(BlobStorageConnection blobStorageConnection, String blobType){

        switch (blobType){
            case "input":
                container = blobStorageConnection.inputContainer;
                break;
            case "output":
                container = blobStorageConnection.outputContainer;
        }

    }

    @Override
    protected Integer doInBackground(String... params) {

        String selectedBlobName = params[0];

        Log.d("TAG","Trying to download " + selectedBlobName);
        try {
            // Get blob
            CloudBlockBlob blob = container.getBlockBlobReference(selectedBlobName);

            // Create file to save the blob to
            File imageLocation = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), selectedBlobName);

            // Download
            Log.d("TAG", "to path: " + imageLocation);
            OutputStream blobStream = new FileOutputStream(imageLocation);
            blob.download(blobStream);
            Log.d("TAG", "Downloaded");
        }catch(Exception e){
            Log.e("TAG", "Error while downloading");
            Log.e("TAG", e.toString());
            e.printStackTrace();
        }

        return 1;

    }

    protected void onPostExecute(Integer Result){
    }

}

class CheckForOutputImage extends AsyncTask<ArrayList<String>, Integer, Integer> {

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

class DeleteBlob extends  AsyncTask<String, Integer, Integer>{

    private BlobStorageConnection blobStorageConnection;
    private Context context;
    private String blobToDeleteName;
    private String blobType;

    DeleteBlob(BlobStorageConnection blobStorageConnection, Context context){
        this.blobStorageConnection = blobStorageConnection;
        this.context = context;
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

        if (result == 1) Toast.makeText(context, "Successfully deleted " + blobToDeleteName + " from " + blobType + " folder.", Toast.LENGTH_LONG).show();
        else Toast.makeText(context, "Couldn't delete " + blobToDeleteName + " from " + blobType + " folder.", Toast.LENGTH_LONG).show();

    }
}