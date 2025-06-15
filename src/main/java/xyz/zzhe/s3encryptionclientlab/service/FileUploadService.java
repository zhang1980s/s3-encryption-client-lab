package xyz.zzhe.s3encryptionclientlab.service;

import xyz.zzhe.s3encryptionclientlab.model.FileUploadMetadata;
import xyz.zzhe.s3encryptionclientlab.model.FileUploadResponse;

import java.io.File;

public interface FileUploadService {
    FileUploadResponse uploadFile(String fileName, File file, FileUploadMetadata metadata);
    byte[] downloadFile(String key);
    String getSignedUrl(String key);
}