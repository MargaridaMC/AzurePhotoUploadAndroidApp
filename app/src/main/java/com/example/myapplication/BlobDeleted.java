package com.example.myapplication;

public interface BlobDeleted {
    void blobDeletedSuccess(boolean success, String blobToDeleteName, String blobType);
}
