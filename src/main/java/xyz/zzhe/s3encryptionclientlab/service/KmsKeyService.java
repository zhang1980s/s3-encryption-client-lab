package xyz.zzhe.s3encryptionclientlab.service;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.*;
import software.amazon.encryption.s3.S3EncryptionClient;

import java.security.KeyPair;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class KmsKeyService {
    private final KmsClient kmsClient;
    private final String keyId;
    
    public KmsKeyService(String keyId) {
        this.kmsClient = KmsClient.builder().build();
        this.keyId = keyId;
    }
    
    public KeyPair getKeyPairFromKms() {
        try {
            log.info("Retrieving key pair from KMS with key ID: {}", keyId);
            
            // Get key tags which contain our key material
            ListResourceTagsResponse tagsResponse = kmsClient.listResourceTags(
                    ListResourceTagsRequest.builder()
                            .keyId(keyId)
                            .build());
            
            Map<String, String> tags = tagsResponse.tags().stream()
                    .collect(Collectors.toMap(Tag::tagKey, Tag::tagValue));
            
            String publicKeyContent = tags.get("PublicKey");
            String privateKeyContent = tags.get("PrivateKey");
            
            if (publicKeyContent == null || privateKeyContent == null) {
                throw new RuntimeException("Key material not found in KMS tags");
            }
            
            // Format the key content back to PEM format
            String publicKeyPem = "-----BEGIN PUBLIC KEY-----\n" + 
                    publicKeyContent + "\n" +
                    "-----END PUBLIC KEY-----";
            
            String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n" + 
                    privateKeyContent + "\n" +
                    "-----END PRIVATE KEY-----";
            
            // Reconstruct the key pair
            return xyz.zzhe.s3encryptionclientlab.util.KeyPairUtil.reconstructKeyPair(
                    publicKeyPem, privateKeyPem);
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve key pair from KMS", e);
        }
    }
    
    public S3EncryptionClient createS3EncryptionClient() {
        KeyPair keyPair = getKeyPairFromKms();
        return S3EncryptionClient.builder()
                .rsaKeyPair(keyPair)
                .build();
    }
}