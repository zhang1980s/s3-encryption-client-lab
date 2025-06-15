package xyz.zzhe.s3encryptionclientlab.service;

import xyz.zzhe.s3encryptionclientlab.model.FileUploadMetadata;
import xyz.zzhe.s3encryptionclientlab.model.FileUploadResponse;
import xyz.zzhe.s3encryptionclientlab.util.KeyPairUtil;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.encryption.s3.S3EncryptionClient;

import java.io.File;
import java.security.KeyPair;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class S3FileUploadEncryptionService implements FileUploadService {
    private final S3EncryptionClient s3Client;
    private final AbstractFileUploadConfig config;
    private final Duration defaultExpiration = Duration.ofMinutes(10);
    private final S3Presigner presigner;

    public S3FileUploadEncryptionService(final AbstractFileUploadConfig config, final S3EncryptionClient s3Client) {
        this.config = config;
        this.s3Client = s3Client;
        this.presigner = S3Presigner.builder().build();
    }

    @Override
    public FileUploadResponse uploadFile(
            final String fileName, final File file, final FileUploadMetadata uploadMetadata) {
        final String key = this.config.getKey(fileName);
        PutObjectResponse response = this.s3Client.putObject(createObjectMetadata(PutObjectRequest.builder()
                        .bucket(this.config.getBucketName())
                        .key(key), uploadMetadata)
                        .build(),
                RequestBody.fromFile(file));
        
        log.info("uploadFile response: {}", response);
        
        return FileUploadResponse.builder()
                .fileName(fileName)
                .key(key)
                .url(this.getSignedUrl(key))
                .build();
    }

    @Override
    public byte[] downloadFile(String key) {
        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(config.getBucketName())
                .key(key)
                .build());
        
        log.info("Downloaded file: {}, size: {} bytes", key, objectBytes.asByteArray().length);
        return objectBytes.asByteArray();
    }

    @Override
    public String getSignedUrl(String key) {
        return presigner.presignGetObject(GetObjectPresignRequest.builder()
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(config.getBucketName())
                        .key(key)
                        .build())
                .signatureDuration(defaultExpiration)
                .build()).url().toString();
    }

    private PutObjectRequest.Builder createObjectMetadata(
            PutObjectRequest.Builder builder, FileUploadMetadata metadata) {
        if (metadata == null) {
            return builder;
        }

        Map<String, String> userMetadata = new HashMap<>();
        if (metadata.getUserMetadata() != null) {
            userMetadata.putAll(metadata.getUserMetadata());
        }

        return builder
                .contentType(metadata.getContentType())
                .metadata(userMetadata);
    }

    public static KeyPair reconstructKeyPair(String publicKeyPem, String privateKeyPem) {
        return KeyPairUtil.reconstructKeyPair(publicKeyPem, privateKeyPem);
    }
}