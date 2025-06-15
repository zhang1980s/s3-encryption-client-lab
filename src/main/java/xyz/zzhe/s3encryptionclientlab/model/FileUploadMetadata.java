package xyz.zzhe.s3encryptionclientlab.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class FileUploadMetadata {
    private String contentType;
    private Map<String, String> userMetadata;
}