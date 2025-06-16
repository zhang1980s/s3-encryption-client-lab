# Amazon S3 Encryption Client Lab Application

This lab application demonstrates how to use the Amazon S3 Encryption Client for Java to perform client-side encryption of data stored in Amazon S3.

## Prerequisites

- Java 11 (Amazon Corretto 11 recommended)
- Maven
- AWS account with appropriate permissions
- AWS credentials configured

## Setup

1. Deploy the S3 bucket using Pulumi:
   ```
   cd infrastructure
   pulumi up
   ```
   
   Note: Pulumi will create an S3 bucket with a name like "zzhe-sin-encrption-client-lab-bucket-a9733f6".
   The exact name will be shown in the Pulumi output as "bucketName".

2. Build the project:
   ```
   mvn clean package
   ```

3. Update the bucket name in application.properties:
   
   Open `src/main/resources/application.properties` and set the `aws.s3.bucket` property to the bucket name
   created by Pulumi (e.g., "zzhe-sin-encrption-client-lab-bucket-a9733f6").
   
   ```properties
   aws.s3.bucket=zzhe-sin-encrption-client-lab-bucket-a9733f6
   ```

4. Run the S3EncryptionClientLabApplication:
   ```
   java -jar target/s3-encryption-client-lab-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```

   Or directly with Maven:
   ```
   mvn exec:java -Dexec.mainClass="xyz.zzhe.s3encryptionclientlab.S3EncryptionClientLabApplication"
   ```
   
   Or simply:
   ```
   mvn exec:java
   ```

## Lab Exercises

### Exercise 1: Setting up the encryption client

The lab demonstrates how to set up the S3 encryption client using RSA key pairs. The S3EncryptionClientLabApplication will:
- First try to create the client using RSA keys from application.properties
- If that fails, generate a new RSA key pair if one doesn't exist
- Save the key pair to the `keys` directory
- Create an S3 encryption client using the key pair
- Update the application properties with the generated keys for future use

### Exercise 2: Uploading encrypted objects

The lab demonstrates how to upload encrypted objects to S3. The S3EncryptionClientLabApplication will:
- Create a test file with sample content
- Add metadata to the file
- Upload the file to S3 using the encryption client
- Display the upload response, including the file key and pre-signed URL

### Exercise 3: Downloading and decrypting objects

The lab demonstrates how to download and decrypt objects from S3. The S3EncryptionClientLabApplication will:
- Download the previously uploaded file
- Decrypt the file using the encryption client
- Display the decrypted content

### Exercise 4: Working with pre-signed URLs

The lab demonstrates how to generate pre-signed URLs for encrypted objects. The S3EncryptionClientLabApplication will:
- Generate a pre-signed URL for the uploaded file
- Display the URL, which can be used to access the file for a limited time

## Advanced Topics

- Using different encryption algorithms
- Encryption context
- Key rotation
- Cross-account access to encrypted objects

## Key Features

### Client Creation Methods

The application supports two ways of creating the S3 encryption client:

1. **From Configuration**: Using the `createEncryptionS3Client` method that reads RSA key PEM strings from the configuration:
   ```java
   private static S3EncryptionClient createEncryptionS3Client(final S3Properties.S3FileUploadClientConfig clientConfig) {
       if (clientConfig == null ||
           !StringUtils.hasText(clientConfig.getRsaPrivatePem()) ||
           !StringUtils.hasText(clientConfig.getRsaPublicPem())) {
           return null;
       }
       
       KeyPair keyPair = S3FileUploadEncryptionService.reconstructKeyPair(
           clientConfig.getRsaPublicPem(),
           clientConfig.getRsaPrivatePem()
       );
       
       return S3EncryptionClient.builder()
               .rsaKeyPair(keyPair)
               .build();
   }
   ```

2. **From Files**: Loading or generating key pairs from files in the `keys` directory.

## Resources

- [Amazon S3 Encryption Client for Java](https://github.com/aws/amazon-s3-encryption-client-java)
- [AWS SDK for Java Developer Guide](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html)
- [Amazon S3 Developer Guide](https://docs.aws.amazon.com/AmazonS3/latest/dev/Welcome.html)
- [Amazon S3 Encryption Client Documentation](https://docs.aws.amazon.com/AmazonS3/latest/userguide/UsingClientSideEncryption.html)