package xyz.zzhe.s3encryptionclientlab;

import xyz.zzhe.s3encryptionclientlab.config.S3Properties;
import xyz.zzhe.s3encryptionclientlab.model.FileUploadMetadata;
import xyz.zzhe.s3encryptionclientlab.model.FileUploadResponse;
import xyz.zzhe.s3encryptionclientlab.service.AbstractFileUploadConfig;
import xyz.zzhe.s3encryptionclientlab.service.FileUploadService;
import xyz.zzhe.s3encryptionclientlab.service.S3FileUploadEncryptionService;
import xyz.zzhe.s3encryptionclientlab.util.KeyPairUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import software.amazon.encryption.s3.S3EncryptionClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

/**
 * Main application class for the S3 Encryption Client Lab.
 * Demonstrates client-side encryption for S3 objects using RSA key pairs.
 */
@Slf4j
public class S3EncryptionClientLabApplication {

    public static void main(String[] args) {
        try {
            // Load configuration
            S3Properties s3Properties = S3Properties.loadFromProperties();
            log.info("Loaded S3 properties: region={}, bucket={}", s3Properties.getRegion(), s3Properties.getBucketName());

            // Generate or load RSA key pair
            KeyPair keyPair;
            Path keysDir = Paths.get("keys");
            Path publicKeyPath = keysDir.resolve("public_key.pem");
            Path privateKeyPath = keysDir.resolve("private_key.pem");

            if (!Files.exists(keysDir)) {
                Files.createDirectories(keysDir);
            }

            if (!Files.exists(publicKeyPath) || !Files.exists(privateKeyPath)) {
                log.info("Generating new RSA key pair...");
                keyPair = KeyPairUtil.generateRSAKeyPair();
                KeyPairUtil.saveKeyPair(keyPair, publicKeyPath.toString(), privateKeyPath.toString());
                log.info("RSA key pair generated and saved to {}", keysDir.toAbsolutePath());
            } else {
                log.info("Loading existing RSA key pair...");
                keyPair = KeyPairUtil.loadKeyPair(publicKeyPath.toString(), privateKeyPath.toString());
                log.info("RSA key pair loaded from {}", keysDir.toAbsolutePath());
            }

            // Try to create S3 encryption client from properties first
            S3EncryptionClient s3EncryptionClient = createEncryptionS3Client(s3Properties.getClientConfig());
            
            // If client creation from properties failed, use the file-based key pair
            if (s3EncryptionClient == null) {
                log.info("Creating S3 encryption client from file-based key pair");
                s3EncryptionClient = S3EncryptionClient.builder()
                        .rsaKeyPair(keyPair)
                        .build();
                
                // Update the properties with the generated keys for future use
                try {
                    String publicKeyContent = new String(Files.readAllBytes(publicKeyPath));
                    String privateKeyContent = new String(Files.readAllBytes(privateKeyPath));
                    s3Properties.getClientConfig().setRsaPublicPem(publicKeyContent);
                    s3Properties.getClientConfig().setRsaPrivatePem(privateKeyContent);
                } catch (IOException e) {
                    log.warn("Could not read key files to update properties", e);
                }
            }

            // Create file upload service
            AbstractFileUploadConfig uploadConfig = new AbstractFileUploadConfig(s3Properties.getBucketName()) {};
            FileUploadService fileUploadService = new S3FileUploadEncryptionService(uploadConfig, s3EncryptionClient);

            // Create a test file
            File testFile = createTestFile("Hello, this is a test file for S3 encryption client!");

            // Upload the file with metadata
            Map<String, String> userMetadata = new HashMap<>();
            userMetadata.put("purpose", "testing");
            userMetadata.put("created-by", "s3-encryption-client-lab");

            FileUploadMetadata metadata = FileUploadMetadata.builder()
                    .contentType("text/plain")
                    .userMetadata(userMetadata)
                    .build();

            log.info("Uploading encrypted file to S3...");
            FileUploadResponse uploadResponse = fileUploadService.uploadFile("test-file.txt", testFile, metadata);
            log.info("File uploaded successfully: {}", uploadResponse);

            // Download the file
            log.info("Downloading and decrypting file from S3...");
            byte[] downloadedContent = fileUploadService.downloadFile(uploadResponse.getKey());
            log.info("File downloaded and decrypted successfully. Content: {}", 
                    new String(downloadedContent, StandardCharsets.UTF_8));

            // Get a pre-signed URL
            String signedUrl = fileUploadService.getSignedUrl(uploadResponse.getKey());
            log.info("Pre-signed URL for the file: {}", signedUrl);

            log.info("S3 Encryption Client Lab completed successfully!");

        } catch (Exception e) {
            log.error("Error in S3 Encryption Client Lab", e);
        }
    }

    /**
     * Creates a temporary test file with the specified content.
     *
     * @param content The content to write to the test file
     * @return The created test file
     * @throws Exception If an error occurs while creating the file
     */
    private static File createTestFile(String content) throws Exception {
        File tempFile = File.createTempFile("s3-encryption-test-", ".txt");
        tempFile.deleteOnExit();
        
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }
        
        log.info("Created test file: {}", tempFile.getAbsolutePath());
        return tempFile;
    }
    
    /**
     * Creates an S3EncryptionClient from the provided client configuration.
     *
     * @param clientConfig The client configuration containing RSA key PEM strings
     * @return An S3EncryptionClient instance, or null if the configuration is invalid
     */
    private static S3EncryptionClient createEncryptionS3Client(final S3Properties.S3FileUploadClientConfig clientConfig) {
        if (clientConfig == null ||
            !StringUtils.hasText(clientConfig.getRsaPrivatePem()) ||
            !StringUtils.hasText(clientConfig.getRsaPublicPem())) {
            log.info("No valid RSA keys found in configuration");
            return null;
        }
        
        log.info("Creating S3 encryption client from configuration PEM strings");
        KeyPair keyPair = S3FileUploadEncryptionService.reconstructKeyPair(
            clientConfig.getRsaPublicPem(),
            clientConfig.getRsaPrivatePem()
        );
        
        return S3EncryptionClient.builder()
                .rsaKeyPair(keyPair)
                .build();
    }
}