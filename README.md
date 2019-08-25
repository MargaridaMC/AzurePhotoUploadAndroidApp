# Android App for photo uploading to Azure Blob Storage

Companion project to https://github.com/lokijota/AzureFunctionInvoicePhotoProcessing.

The goal of the app is to create an easy interface to this azure function, but can be used with other setups. 
It allows you to upload photos to your Azure Blob Storage. When these are done being processed by the function you receive a notification. It also lists all the blobs currently in your blob storage containers and lets you view, delete or attach the images to an email. 

### TODO:
- Improve UI
- Allow multiple file selection in list view
- Multiple options for output images (at the moment, when notification is clicked on, the images are automatically attached to an email of your choice)
- Order list view by input date
