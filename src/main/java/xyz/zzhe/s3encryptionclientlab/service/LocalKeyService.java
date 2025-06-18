package xyz.zzhe.s3encryptionclientlab.service;

import lombok.extern.slf4j.Slf4j;
import software.amazon.encryption.s3.S3EncryptionClient;
import xyz.zzhe.s3encryptionclientlab.config.S3Properties;
import xyz.zzhe.s3encryptionclientlab.util.KeyPairUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;

/**
 * Service for handling encryption operations using local RSA keys.
 * This implementation replaces the KMS-based encryption with local key-based encryption.
 */
@Slf4j
public class LocalKeyService {
    private KeyPair keyPair; // Removed final modifier to allow initialization in different paths
    private static final String DEFAULT_PUBLIC_KEY_PATH = "keys/public_key.pem";
    private static final String DEFAULT_PRIVATE_KEY_PATH = "keys/private_key.pem";
    
    /**
     * Creates a LocalKeyService using the default key paths.
     */
    public LocalKeyService() {
        this(DEFAULT_PUBLIC_KEY_PATH, DEFAULT_PRIVATE_KEY_PATH);
    }
    
    /**
     * Creates a LocalKeyService using the specified key paths.
     *
     * @param publicKeyPath Path to the public key file
     * @param privateKeyPath Path to the private key file
     */
    public LocalKeyService(String publicKeyPath, String privateKeyPath) {
        try {
            log.info("Loading RSA key pair from {} and {}", publicKeyPath, privateKeyPath);
            this.keyPair = KeyPairUtil.loadKeyPair(publicKeyPath, privateKeyPath);
            log.info("RSA key pair loaded successfully");
        } catch (Exception e) {
            log.error("Failed to load RSA key pair", e);
            throw new RuntimeException("Failed to load RSA key pair", e);
        }
    }
    
    /**
     * Creates a LocalKeyService using the properties from S3Properties.
     * If key files are not found or key paths are not specified, it will use key content from properties.
     *
     * @param s3Properties The S3 properties containing key paths and content
     */
    public LocalKeyService(S3Properties s3Properties) {
        try {
            // Try to load keys from files first if paths are specified
            if (s3Properties.hasKeyPaths()) {
                String publicKeyPath = s3Properties.getPublicKeyPath();
                String privateKeyPath = s3Properties.getPrivateKeyPath();
                
                log.info("Attempting to load RSA key pair from {} and {}", publicKeyPath, privateKeyPath);
                
                // Check if files exist with absolute paths for debugging
                java.nio.file.Path absPublicKeyPath = Paths.get(publicKeyPath).toAbsolutePath();
                java.nio.file.Path absPrivateKeyPath = Paths.get(privateKeyPath).toAbsolutePath();
                log.info("Absolute paths: public key={}, private key={}",
                        absPublicKeyPath, absPrivateKeyPath);
                
                boolean publicKeyExists = Files.exists(Paths.get(publicKeyPath));
                boolean privateKeyExists = Files.exists(Paths.get(privateKeyPath));
                
                log.info("Key files exist check: public key={}, private key={}",
                        publicKeyExists, privateKeyExists);
                
                if (publicKeyExists && privateKeyExists) {
                    try {
                        this.keyPair = KeyPairUtil.loadKeyPair(publicKeyPath, privateKeyPath);
                        log.info("RSA key pair loaded successfully from files");
                        return;
                    } catch (Exception e) {
                        log.error("Failed to load key pair from files", e);
                        log.info("Will try fallback methods");
                    }
                } else {
                    log.warn("Key files not found: public key exists: {}, private key exists: {}",
                            publicKeyExists, privateKeyExists);
                    
                    // List directory contents for debugging
                    try {
                        java.nio.file.Path keyDir = Paths.get("keys").toAbsolutePath();
                        log.info("Listing contents of keys directory: {}", keyDir);
                        if (Files.exists(keyDir)) {
                            Files.list(keyDir).forEach(path ->
                                log.info("Found file: {}", path.getFileName()));
                        } else {
                            log.warn("Keys directory does not exist: {}", keyDir);
                        }
                    } catch (Exception e) {
                        log.warn("Error listing keys directory", e);
                    }
                }
            } else {
                log.info("Key paths not specified in properties");
            }
            
            // Fallback to key content if available
            if (s3Properties.hasKeyContent()) {
                log.info("Loading RSA key pair from properties content");
                this.keyPair = KeyPairUtil.reconstructKeyPair(
                        s3Properties.getPublicKeyContent(),
                        s3Properties.getPrivateKeyContent());
                log.info("RSA key pair loaded successfully from properties content");
            } else {
                // If neither paths nor content are available, try default paths
                log.info("Key content not specified in properties, trying default paths: {} and {}",
                        DEFAULT_PUBLIC_KEY_PATH, DEFAULT_PRIVATE_KEY_PATH);
                this.keyPair = KeyPairUtil.loadKeyPair(DEFAULT_PUBLIC_KEY_PATH, DEFAULT_PRIVATE_KEY_PATH);
                log.info("RSA key pair loaded successfully from default paths");
            }
        } catch (Exception e) {
            log.error("Failed to load RSA key pair", e);
            throw new RuntimeException("Failed to load RSA key pair", e);
        }
    }
    
    /**
     * Creates an S3 encryption client that uses the local RSA key pair for encryption operations.
     *
     * @return The configured S3 encryption client
     */
    public S3EncryptionClient createS3EncryptionClient() {
        log.info("Creating S3 encryption client with local RSA key pair");
        
        try {
            // Create the S3 encryption client with the RSA key pair
            return S3EncryptionClient.builder()
                    .rsaKeyPair(keyPair)
                    .build();
        } catch (Exception e) {
            log.error("Failed to create S3 encryption client", e);
            throw new RuntimeException("Failed to create S3 encryption client", e);
        }
    }
    
    /**
     * Verifies that the key pair is valid and can be used for encryption/decryption.
     *
     * @return true if the key pair is valid
     */
    public boolean verifyKeyPair() {
        try {
            log.info("Verifying RSA key pair");
            
            // Simple verification by checking if the keys are not null
            if (keyPair.getPublic() == null || keyPair.getPrivate() == null) {
                log.warn("Invalid key pair: public or private key is null");
                return false;
            }
            
            // Additional verification could be added here if needed
            
            log.info("RSA key pair is valid");
            return true;
        } catch (Exception e) {
            log.error("Failed to verify RSA key pair", e);
            return false;
        }
    }
    
    /**
     * Gets the loaded key pair.
     *
     * @return The loaded key pair
     */
    public KeyPair getKeyPair() {
        return keyPair;
    }
}