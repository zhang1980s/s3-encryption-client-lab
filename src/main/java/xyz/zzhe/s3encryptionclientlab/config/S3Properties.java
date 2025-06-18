package xyz.zzhe.s3encryptionclientlab.config;

import lombok.Data;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Data
public class S3Properties {
    private String region;
    private String bucketName;
    
    // Key file paths
    private String publicKeyPath;
    private String privateKeyPath;
    
    // Key content (used as fallback if key files are not found)
    private String publicKeyContent;
    private String privateKeyContent;

    public static S3Properties loadFromProperties() {
        Properties properties = new Properties();
        try (InputStream input = S3Properties.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find application.properties");
            }
            properties.load(input);
            
            S3Properties s3Properties = new S3Properties();
            s3Properties.setRegion(properties.getProperty("aws.region"));
            s3Properties.setBucketName(properties.getProperty("aws.s3.bucket"));
            
            // Load key paths
            s3Properties.setPublicKeyPath(properties.getProperty("key.public.path"));
            s3Properties.setPrivateKeyPath(properties.getProperty("key.private.path"));
            
            // Load key content
            s3Properties.setPublicKeyContent(properties.getProperty("key.public.content"));
            s3Properties.setPrivateKeyContent(properties.getProperty("key.private.content"));
            
            return s3Properties;
        } catch (IOException ex) {
            throw new RuntimeException("Error loading application properties", ex);
        }
    }
    
    /**
     * Checks if key paths are defined
     *
     * @return true if both public and private key paths are defined
     */
    public boolean hasKeyPaths() {
        return publicKeyPath != null && !publicKeyPath.isEmpty()
                && privateKeyPath != null && !privateKeyPath.isEmpty();
    }
    
    /**
     * Checks if key content is defined
     *
     * @return true if both public and private key content are defined
     */
    public boolean hasKeyContent() {
        return publicKeyContent != null && !publicKeyContent.isEmpty()
                && privateKeyContent != null && !privateKeyContent.isEmpty();
    }
}