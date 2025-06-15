package xyz.zzhe.s3encryptionclientlab.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileUploadResponse {
    private String fileName;
    private String key;
    private String url;
}