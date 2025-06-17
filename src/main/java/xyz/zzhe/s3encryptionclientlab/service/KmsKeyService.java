package xyz.zzhe.s3encryptionclientlab.service;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.*;
import software.amazon.encryption.s3.S3EncryptionClient;
import software.amazon.encryption.s3.materials.KmsKeyring;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for interacting with AWS KMS for encryption operations.
 * This implementation uses the standard KMS import process and KMS-based encryption.
 */
@Slf4j
public class KmsKeyService {
    private final KmsClient kmsClient;
    private final String keyId;
    
    public KmsKeyService(String keyId) {
        this.kmsClient = KmsClient.builder().build();
        this.keyId = keyId;
    }
    
    /**
     * Encrypts data using KMS.
     *
     * @param data The data to encrypt
     * @param context The encryption context (optional)
     * @return The encrypted data
     */
    public byte[] encrypt(byte[] data, Map<String, String> context) {
        try {
            log.info("Encrypting data using KMS with key ID: {}", keyId);
            
            EncryptRequest request = EncryptRequest.builder()
                    .keyId(keyId)
                    .plaintext(SdkBytes.fromByteArray(data))
                    .encryptionContext(context != null ? context : new HashMap<>())
                    .build();
            
            EncryptResponse response = kmsClient.encrypt(request);
            return response.ciphertextBlob().asByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt data using KMS", e);
        }
    }
    
    /**
     * Decrypts data using KMS.
     *
     * @param encryptedData The encrypted data
     * @param context The encryption context (must match the one used for encryption)
     * @return The decrypted data
     */
    public byte[] decrypt(byte[] encryptedData, Map<String, String> context) {
        try {
            log.info("Decrypting data using KMS");
            
            DecryptRequest request = DecryptRequest.builder()
                    .ciphertextBlob(SdkBytes.fromByteArray(encryptedData))
                    .keyId(keyId) // Optional but recommended for security
                    .encryptionContext(context != null ? context : new HashMap<>())
                    .build();
            
            DecryptResponse response = kmsClient.decrypt(request);
            return response.plaintext().asByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt data using KMS", e);
        }
    }
    
    /**
     * Creates an S3 encryption client that uses KMS for encryption operations.
     *
     * @return The configured S3 encryption client
     */
    public S3EncryptionClient createS3EncryptionClient() {
        log.info("Creating S3 encryption client with KMS for key ID: {}", keyId);
        
        // Create the S3 encryption client with KMS integration
        return S3EncryptionClient.builder()
                .kmsKeyId(keyId)
                .build();
    }
    
    /**
     * Verifies that the KMS key is accessible and properly configured.
     *
     * @return true if the key is accessible and properly configured
     */
    public boolean verifyKmsKeyAccess() {
        try {
            log.info("Verifying access to KMS key: {}", keyId);
            
            // Try to describe the key to verify access
            DescribeKeyResponse response = kmsClient.describeKey(
                    DescribeKeyRequest.builder()
                            .keyId(keyId)
                            .build());
            
            boolean isEnabled = response.keyMetadata().enabled();
            String keyState = response.keyMetadata().keyStateAsString();
            String keyOrigin = response.keyMetadata().originAsString();
            
            log.info("KMS key status - ID: {}, Enabled: {}, State: {}, Origin: {}",
                    keyId, isEnabled, keyState, keyOrigin);
            
            // Check if the key is enabled and in the correct state
            if (!isEnabled || !"Enabled".equals(keyState)) {
                log.warn("KMS key is not in the expected state. Enabled: {}, State: {}",
                        isEnabled, keyState);
                return false;
            }
            
            // For imported keys, verify the origin is EXTERNAL
            if (!"EXTERNAL".equals(keyOrigin)) {
                log.warn("KMS key does not have EXTERNAL origin. Origin: {}", keyOrigin);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("Failed to verify KMS key access", e);
            return false;
        }
    }
}