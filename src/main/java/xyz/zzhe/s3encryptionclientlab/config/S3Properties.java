package xyz.zzhe.s3encryptionclientlab.config;

import lombok.Data;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Data
public class S3Properties {
    private String region;
    private String bucketName;
    private String kmsKeyId;

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
            s3Properties.setKmsKeyId(properties.getProperty("aws.kms.keyId"));
            
            return s3Properties;
        } catch (IOException ex) {
            throw new RuntimeException("Error loading application properties", ex);
        }
    }
}