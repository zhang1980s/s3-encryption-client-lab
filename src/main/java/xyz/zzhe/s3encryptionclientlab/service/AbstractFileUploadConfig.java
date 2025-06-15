package xyz.zzhe.s3encryptionclientlab.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractFileUploadConfig {
    @Getter
    protected final String bucketName;
    
    protected String getKey(String fileName) {
        return "uploads/" + fileName;
    }
}