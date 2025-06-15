package xyz.zzhe.s3encryptionclientlab.config;

import lombok.Data;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Data
public class S3Properties {
    private String region;
    private String bucketName;
    private S3FileUploadClientConfig clientConfig;

    @Data
    public static class S3FileUploadClientConfig {
        public String rsaPublicPem;
        public String rsaPrivatePem;
    }

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
            
            S3FileUploadClientConfig clientConfig = new S3FileUploadClientConfig();
            clientConfig.setRsaPublicPem(properties.getProperty("aws.s3.encryption.rsa.public"));
            clientConfig.setRsaPrivatePem(properties.getProperty("aws.s3.encryption.rsa.private"));
            s3Properties.setClientConfig(clientConfig);
            
            return s3Properties;
        } catch (IOException ex) {
            throw new RuntimeException("Error loading application properties", ex);
        }
    }
}