package com.example.form.service;


import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;

@Service
public class FileService {
    private final BlobServiceClient blobServiceClient;

    @Autowired
    public FileService(BlobServiceClient blobServiceClient) {
        this.blobServiceClient = blobServiceClient;
    }

    public String uploadFile(@NonNull MultipartFile file, String containerName, String fileName) {
        String url = "";
        fileName = buildFileName(file, fileName);
        BlobContainerClient blobContainerClient = getBlobContainerClient(containerName);
        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(fileName).getBlockBlobClient();
        try {
            // delete file if already exists in that container
            if (blockBlobClient.exists()) {
                blockBlobClient.delete();
            }

            // upload file to azure blob storage
            blockBlobClient.upload(new BufferedInputStream(file.getInputStream()), file.getSize(), true);
            url = blockBlobClient.getBlobUrl();
        } catch (IOException e) {
            url = "Failed to upload file " + fileName;
        }
        return url;
    }

    public String getSas(String containerName) {
        /*
        BlobContainerClient blobContainerClient = getBlobContainerClient(containerName);

        // Get a user delegation key for the Blob service that's valid for seven days.
        // You can use the key to generate any number of shared access signatures over the lifetime of the key.
        keyStart = OffsetDateTime.now();
        keyExpiry = OffsetDateTime.now().plusDays(7);
        userDelegationKey = blobServiceClient.getUserDelegationKey(keyStart, keyExpiry);

        BlobContainerSasPermission blobContainerSas = new BlobContainerSasPermission();
        blobContainerSas.setReadPermission(true);
        BlobServiceSasSignatureValues blobServiceSasSignatureValues = new BlobServiceSasSignatureValues(keyExpiry, blobContainerSas);
        if (!blobContainerClient.exists()) {
            blobContainerClient.create();
        }
            

        String sas = blobContainerClient.generateUserDelegationSas(blobServiceSasSignatureValues, userDelegationKey);
        */
        String sas = "";
        BlobContainerClient blobContainerClient = getBlobContainerClient(containerName);
        
        try {
            OffsetDateTime expiryTime = OffsetDateTime.now().plusDays(1);
            BlobContainerSasPermission permission = new BlobContainerSasPermission().setReadPermission(true);

            BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(expiryTime, permission).setStartTime(OffsetDateTime.now());

            // Client must be authenticated via StorageSharedKeyCredential
            sas = blobContainerClient.generateSas(values);

        } catch (Exception e) {
            sas = "Failed to generate SAS token";
        }
        return '?' + sas;
    }



    private @NonNull BlobContainerClient getBlobContainerClient(@NonNull String containerName) {
        // create container if not exists
        BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!blobContainerClient.exists()) {
            blobContainerClient.create();
        }
        return blobContainerClient;
    }

    private String buildFileName(MultipartFile file, String fileName){
        String fileRealName = file.getOriginalFilename();
        String extention = fileRealName.substring(fileRealName.lastIndexOf(".") + 1);

        return fileName + "." + extention;
    }
}
